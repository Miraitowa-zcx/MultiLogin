package moe.caa.multilogin.velocity.injector.handler;

import com.google.common.primitives.Longs;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import lombok.Getter;
import moe.caa.multilogin.api.internal.auth.AuthResult;
import moe.caa.multilogin.api.internal.logger.LoggerProvider;
import moe.caa.multilogin.api.internal.main.MultiCoreAPI;
import moe.caa.multilogin.api.internal.skinrestorer.SkinRestorerResult;
import moe.caa.multilogin.api.profile.GameProfile;
import moe.caa.multilogin.core.auth.LoginAuthResult;
import moe.caa.multilogin.velocity.injector.compat.VelocityInternals;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Takes over Velocity's encryption response handling so MultiLogin can perform
 * authentication before handing control back to AuthSessionHandler.
 */
@Getter
public class MultiInitialLoginSessionHandler {
    private final InitialLoginSessionHandler initialLoginSessionHandler;
    private final MultiCoreAPI multiCoreAPI;
    private final VelocityInternals internals;
    private final AuthSessionFactory authSessionFactory;
    private final VelocityServer server;
    private final MinecraftConnection mcConnection;
    private final LoginInboundConnection inbound;

    private ServerLoginPacket login;
    private byte[] verify;
    private boolean encrypted;

    public MultiInitialLoginSessionHandler(
            InitialLoginSessionHandler initialLoginSessionHandler,
            MultiCoreAPI multiCoreAPI,
            VelocityInternals internals) {
        this.initialLoginSessionHandler = Objects.requireNonNull(
                initialLoginSessionHandler,
                "initialLoginSessionHandler");
        this.multiCoreAPI = Objects.requireNonNull(multiCoreAPI, "multiCoreAPI");
        this.internals = Objects.requireNonNull(internals, "internals");
        this.authSessionFactory = internals::newAuthSessionHandler;
        this.server = internals.server(initialLoginSessionHandler);
        this.mcConnection = internals.connection(initialLoginSessionHandler);
        this.inbound = internals.inbound(initialLoginSessionHandler);
    }

    MultiInitialLoginSessionHandler(
            VelocityServer server,
            MinecraftConnection mcConnection,
            LoginInboundConnection inbound,
            AuthSessionFactory authSessionFactory) {
        this.initialLoginSessionHandler = null;
        this.multiCoreAPI = null;
        this.internals = null;
        this.server = server;
        this.mcConnection = mcConnection;
        this.inbound = inbound;
        this.authSessionFactory = Objects.requireNonNull(authSessionFactory, "authSessionFactory");
    }

    private void initValues() {
        this.login = internals.login(initialLoginSessionHandler);
        this.verify = internals.verifyToken(initialLoginSessionHandler);
    }

    public void handle(EncryptionResponsePacket packet) {
        initValues();

        internals.assertState(initialLoginSessionHandler, internals.encryptionRequestSent());
        internals.setState(initialLoginSessionHandler, internals.encryptionResponseReceived());

        ServerLoginPacket currentLogin = this.login;
        if (currentLogin == null) {
            throw new IllegalStateException("No ServerLogin packet received yet.");
        }
        if (this.verify.length == 0) {
            throw new IllegalStateException("No EncryptionRequest packet sent yet.");
        }

        try {
            KeyPair serverKeyPair = this.server.getServerKeyPair();
            if (this.inbound.getIdentifiedKey() != null) {
                IdentifiedKey playerKey = this.inbound.getIdentifiedKey();
                if (!playerKey.verifyDataSignature(
                        packet.getVerifyToken(),
                        this.verify,
                        Longs.toByteArray(packet.getSalt()))) {
                    throw new IllegalStateException("Invalid client public signature.");
                }
            } else {
                byte[] decryptedVerifyToken =
                        EncryptionUtils.decryptRsa(serverKeyPair, packet.getVerifyToken());
                if (!MessageDigest.isEqual(this.verify, decryptedVerifyToken)) {
                    throw new IllegalStateException(
                            "Unable to successfully decrypt the verification token.");
                }
            }

            byte[] decryptedSharedSecret =
                    EncryptionUtils.decryptRsa(serverKeyPair, packet.getSharedSecret());
            encrypted = true;

            String username = currentLogin.getUsername();
            String serverId = EncryptionUtils.generateServerId(
                    decryptedSharedSecret,
                    serverKeyPair.getPublic());
            String playerIp = ((InetSocketAddress) this.mcConnection.getRemoteAddress())
                    .getHostString();

            multiCoreAPI.getPlugin().getRunServer().getScheduler().runTaskAsync(() ->
                    processAuthentication(
                            username,
                            serverId,
                            playerIp,
                            decryptedSharedSecret));
        } catch (GeneralSecurityException failure) {
            LoggerProvider.getLogger().error("Unable to enable encryption.", failure);
            this.mcConnection.close(true);
        }
    }

    private void processAuthentication(
            String username,
            String serverId,
            String playerIp,
            byte[] decryptedSharedSecret) {
        LoginAuthResult result = (LoginAuthResult) multiCoreAPI
                .getAuthHandler()
                .auth(username, serverId, playerIp);
        try {
            if (!mcConnection.getChannel().eventLoop().submit(() ->
                    enableEncryption(decryptedSharedSecret)).get()) {
                return;
            }

            if (result.getResult() != AuthResult.Result.ALLOW) {
                this.inbound.disconnect(Component.text(result.getKickMessage()));
                return;
            }

            GameProfile gameProfile = restoreSkin(result);
            com.velocitypowered.api.util.GameProfile velocityProfile =
                    generateGameProfile(gameProfile);
            mcConnection.getChannel().eventLoop().submit(() ->
                    this.mcConnection.setActiveSessionHandler(
                            StateRegistry.LOGIN,
                            activateAuthenticatedSession(velocityProfile, serverId))).get();
        } catch (Throwable failure) {
            LoggerProvider.getLogger().error(
                    "An exception occurred while processing validation results.", failure);
            if (isEncrypted()) {
                getInbound().disconnect(Component.text(
                        multiCoreAPI.getLanguageHandler().getMessage("auth_error")));
            }
            mcConnection.close(true);
        }
    }

    private boolean enableEncryption(byte[] decryptedSharedSecret) {
        if (this.mcConnection.isClosed()) {
            return false;
        }
        try {
            this.mcConnection.enableEncryption(decryptedSharedSecret);
            return true;
        } catch (GeneralSecurityException failure) {
            LoggerProvider.getLogger().error(
                    "Unable to enable encryption for connection", failure);
            this.mcConnection.close(true);
            return false;
        }
    }

    private GameProfile restoreSkin(LoginAuthResult result) {
        GameProfile gameProfile = result.getResponse();
        try {
            SkinRestorerResult restorerResult =
                    multiCoreAPI.getSkinRestorerHandler().doRestorer(result);
            if (restorerResult.getThrowable() != null) {
                LoggerProvider.getLogger().error(
                        "An exception occurred while processing the skin repair.",
                        restorerResult.getThrowable());
            }
            LoggerProvider.getLogger().debug(String.format(
                    "Skin restore result of %s is %s.",
                    result.getBaseServiceAuthenticationResult().getResponse().getName(),
                    restorerResult.getReason()));
            if (restorerResult.getResponse() != null) {
                gameProfile = restorerResult.getResponse();
            }
        } catch (Exception failure) {
            LoggerProvider.getLogger().debug(String.format(
                    "Skin restore result of %s is error",
                    result.getBaseServiceAuthenticationResult().getResponse().getName()));
            LoggerProvider.getLogger().debug(
                    "An exception occurred while processing the skin repair.", failure);
        }
        return gameProfile;
    }

    AuthSessionHandler activateAuthenticatedSession(
            com.velocitypowered.api.util.GameProfile profile,
            String serverIdHash) {
        return authSessionFactory.create(
                server,
                inbound,
                profile,
                true,
                serverIdHash);
    }

    private com.velocitypowered.api.util.GameProfile generateGameProfile(GameProfile response) {
        return new com.velocitypowered.api.util.GameProfile(
                response.getId(),
                response.getName(),
                response.getPropertyMap().values().stream()
                        .map(property -> new com.velocitypowered.api.util.GameProfile.Property(
                                property.getName(),
                                property.getValue(),
                                property.getSignature()))
                        .collect(Collectors.toList()));
    }
}
