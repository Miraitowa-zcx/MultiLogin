package moe.caa.multilogin.velocity.injector.compat;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodType;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityInternalsTest {
    @Test
    void resolvesVelocity41LoginContract() {
        VelocityInternals internals = VelocityInternals.resolve();

        assertEquals(MethodType.methodType(
                        AuthSessionHandler.class,
                        VelocityServer.class,
                        LoginInboundConnection.class,
                        GameProfile.class,
                        boolean.class,
                        String.class),
                internals.authSessionConstructor().type());
        assertNotNull(internals.loginPacketExpected());
        assertNotNull(internals.loginPacketReceived());
        assertNotNull(internals.encryptionRequestSent());
        assertNotNull(internals.encryptionResponseReceived());
        assertTrue(Arrays.stream(VelocityInternals.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals("clientPlayPlayerGetter")),
                "VelocityInternals must expose the client-play player getter");
        assertEquals(MethodType.methodType(
                        ConnectedPlayer.class,
                        ClientPlaySessionHandler.class),
                internals.clientPlayPlayerGetter().type());
    }
}
