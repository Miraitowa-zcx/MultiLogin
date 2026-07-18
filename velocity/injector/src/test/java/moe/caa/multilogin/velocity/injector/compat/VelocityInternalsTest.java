package moe.caa.multilogin.velocity.injector.compat;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    }
}
