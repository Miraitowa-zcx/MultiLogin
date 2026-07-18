package moe.caa.multilogin.velocity.injector.compat;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Resolved, immutable view of the Velocity login internals required by the
 * injector. Resolution is deliberately strict so unsupported snapshots fail
 * before packet registries are modified.
 */
public final class VelocityInternals {
    private static final String LOGIN_STATE_CLASS =
            "com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler$LoginState";

    private final MethodHandle assertState;
    private final MethodHandle setCurrentState;
    private final MethodHandle getLogin;
    private final MethodHandle getVerify;
    private final MethodHandle getServer;
    private final MethodHandle getInbound;
    private final MethodHandle getMinecraftConnection;
    private final MethodHandle getClientPlayPlayer;
    private final MethodHandle authSessionConstructor;
    private final Enum<?> loginPacketExpected;
    private final Enum<?> loginPacketReceived;
    private final Enum<?> encryptionRequestSent;
    private final Enum<?> encryptionResponseReceived;

    private VelocityInternals(
            MethodHandle assertState,
            MethodHandle setCurrentState,
            MethodHandle getLogin,
            MethodHandle getVerify,
            MethodHandle getServer,
            MethodHandle getInbound,
            MethodHandle getMinecraftConnection,
            MethodHandle getClientPlayPlayer,
            MethodHandle authSessionConstructor,
            Enum<?> loginPacketExpected,
            Enum<?> loginPacketReceived,
            Enum<?> encryptionRequestSent,
            Enum<?> encryptionResponseReceived) {
        this.assertState = assertState;
        this.setCurrentState = setCurrentState;
        this.getLogin = getLogin;
        this.getVerify = getVerify;
        this.getServer = getServer;
        this.getInbound = getInbound;
        this.getMinecraftConnection = getMinecraftConnection;
        this.getClientPlayPlayer = getClientPlayPlayer;
        this.authSessionConstructor = authSessionConstructor;
        this.loginPacketExpected = loginPacketExpected;
        this.loginPacketReceived = loginPacketReceived;
        this.encryptionRequestSent = encryptionRequestSent;
        this.encryptionResponseReceived = encryptionResponseReceived;
    }

    public static VelocityInternals resolve() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<InitialLoginSessionHandler> handlerClass = InitialLoginSessionHandler.class;
        Class<? extends Enum<?>> loginStateClass = resolveLoginStateClass();

        try {
            Method assertStateMethod = handlerClass.getDeclaredMethod("assertState", loginStateClass);
            makeAccessible(assertStateMethod, "InitialLoginSessionHandler.assertState(LoginState)");

            Field currentState = resolveField(handlerClass, "currentState", loginStateClass);
            Field login = resolveField(handlerClass, "login", ServerLoginPacket.class);
            Field verify = resolveField(handlerClass, "verify", byte[].class);
            Field server = resolveField(handlerClass, "server", VelocityServer.class);
            Field inbound = resolveField(handlerClass, "inbound", LoginInboundConnection.class);
            Field connection = resolveField(
                    handlerClass,
                    "mcConnection",
                    MinecraftConnection.class);
            Field clientPlayPlayer = resolveField(
                    ClientPlaySessionHandler.class,
                    "player",
                    ConnectedPlayer.class);

            Constructor<AuthSessionHandler> authConstructor =
                    AuthSessionHandler.class.getDeclaredConstructor(
                            VelocityServer.class,
                            LoginInboundConnection.class,
                            GameProfile.class,
                            boolean.class,
                            String.class);
            makeAccessible(
                    authConstructor,
                    "AuthSessionHandler(VelocityServer, LoginInboundConnection, "
                            + "GameProfile, boolean, String)");

            return new VelocityInternals(
                    lookup.unreflect(assertStateMethod),
                    lookup.unreflectSetter(currentState),
                    lookup.unreflectGetter(login),
                    lookup.unreflectGetter(verify),
                    lookup.unreflectGetter(server),
                    lookup.unreflectGetter(inbound),
                    lookup.unreflectGetter(connection),
                    lookup.unreflectGetter(clientPlayPlayer),
                    lookup.unreflectConstructor(authConstructor),
                    resolveState(loginStateClass, "LOGIN_PACKET_EXPECTED"),
                    resolveState(loginStateClass, "LOGIN_PACKET_RECEIVED"),
                    resolveState(loginStateClass, "ENCRYPTION_REQUEST_SENT"),
                    resolveState(loginStateClass, "ENCRYPTION_RESPONSE_RECEIVED"));
        } catch (NoSuchMethodException failure) {
            throw new VelocityCompatibilityException(
                    "Missing Velocity internal contract: AuthSessionHandler"
                            + "(VelocityServer, LoginInboundConnection, GameProfile, boolean, String)"
                            + " or InitialLoginSessionHandler.assertState(LoginState)",
                    failure);
        } catch (IllegalAccessException failure) {
            throw new VelocityCompatibilityException(
                    "Velocity internal login members are not accessible", failure);
        }
    }

    public VelocityServer server(InitialLoginSessionHandler handler) {
        return invokeGetter(getServer, handler, VelocityServer.class, "server");
    }

    public MinecraftConnection connection(InitialLoginSessionHandler handler) {
        return invokeGetter(
                getMinecraftConnection,
                handler,
                MinecraftConnection.class,
                "mcConnection");
    }

    public LoginInboundConnection inbound(InitialLoginSessionHandler handler) {
        return invokeGetter(getInbound, handler, LoginInboundConnection.class, "inbound");
    }

    public ServerLoginPacket login(InitialLoginSessionHandler handler) {
        return invokeGetter(getLogin, handler, ServerLoginPacket.class, "login");
    }

    public byte[] verifyToken(InitialLoginSessionHandler handler) {
        return invokeGetter(getVerify, handler, byte[].class, "verify");
    }

    public ConnectedPlayer player(ClientPlaySessionHandler handler) {
        return invokeGetter(getClientPlayPlayer, handler, ConnectedPlayer.class, "player");
    }

    public void assertState(InitialLoginSessionHandler handler, Enum<?> state) {
        try {
            assertState.invoke(handler, state);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw invocationFailure("assertState", failure);
        }
    }

    public void setState(InitialLoginSessionHandler handler, Enum<?> state) {
        try {
            setCurrentState.invoke(handler, state);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw invocationFailure("currentState", failure);
        }
    }

    public AuthSessionHandler newAuthSessionHandler(
            VelocityServer server,
            LoginInboundConnection inbound,
            GameProfile profile,
            boolean onlineMode,
            String serverIdHash) {
        try {
            return (AuthSessionHandler) authSessionConstructor.invoke(
                    server,
                    inbound,
                    profile,
                    onlineMode,
                    serverIdHash);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw invocationFailure("AuthSessionHandler constructor", failure);
        }
    }

    public String compatibilitySummary() {
        return "Velocity 4.1 login states, login and client-play fields, assertState, "
                + "and five-argument AuthSessionHandler constructor resolved";
    }

    public MethodHandle authSessionConstructor() {
        return authSessionConstructor;
    }

    public MethodHandle clientPlayPlayerGetter() {
        return getClientPlayPlayer;
    }

    public Enum<?> loginPacketExpected() {
        return loginPacketExpected;
    }

    public Enum<?> loginPacketReceived() {
        return loginPacketReceived;
    }

    public Enum<?> encryptionRequestSent() {
        return encryptionRequestSent;
    }

    public Enum<?> encryptionResponseReceived() {
        return encryptionResponseReceived;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> resolveLoginStateClass() {
        try {
            Class<?> rawClass = Class.forName(LOGIN_STATE_CLASS);
            if (!rawClass.isEnum()) {
                throw new VelocityCompatibilityException(
                        "Velocity internal contract is not an enum: " + LOGIN_STATE_CLASS);
            }
            return (Class<? extends Enum<?>>) rawClass;
        } catch (ClassNotFoundException failure) {
            throw new VelocityCompatibilityException(
                    "Missing Velocity internal contract: " + LOGIN_STATE_CLASS, failure);
        }
    }

    private static Field resolveField(Class<?> owner, String name, Class<?> expectedType) {
        try {
            Field field = owner.getDeclaredField(name);
            if (field.getType() != expectedType) {
                throw new VelocityCompatibilityException(
                        "Velocity internal field " + owner.getSimpleName() + "." + name
                                + " changed type: expected " + expectedType.getName()
                                + ", got " + field.getType().getName());
            }
            makeAccessible(field, owner.getSimpleName() + "." + name);
            return field;
        } catch (NoSuchFieldException failure) {
            throw new VelocityCompatibilityException(
                    "Missing Velocity internal field: " + owner.getSimpleName() + "." + name
                            + " (" + expectedType.getName() + ")",
                    failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Enum<?> resolveState(Class<? extends Enum<?>> type, String name) {
        try {
            return Enum.valueOf((Class) type, name);
        } catch (IllegalArgumentException failure) {
            throw new VelocityCompatibilityException(
                    "Missing Velocity internal login state: " + name, failure);
        }
    }

    private static void makeAccessible(
            java.lang.reflect.AccessibleObject member,
            String description) {
        if (!member.trySetAccessible()) {
            throw new VelocityCompatibilityException(
                    "Velocity internal contract is inaccessible: " + description);
        }
    }

    private static <T> T invokeGetter(
            MethodHandle getter,
            Object owner,
            Class<T> expectedType,
            String description) {
        try {
            return expectedType.cast(getter.invoke(owner));
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw invocationFailure(description, failure);
        }
    }

    private static VelocityCompatibilityException invocationFailure(
            String member,
            Throwable failure) {
        return new VelocityCompatibilityException(
                "Unable to invoke resolved Velocity internal contract: " + member,
                failure);
    }
}
