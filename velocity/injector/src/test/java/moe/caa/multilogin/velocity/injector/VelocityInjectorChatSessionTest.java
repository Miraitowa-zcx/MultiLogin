package moe.caa.multilogin.velocity.injector;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import moe.caa.multilogin.velocity.injector.compat.VelocityInternals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityInjectorChatSessionTest {
    @Test
    void retainsInternalsAndResolvesFinalProfileForPacketSupplier() {
        assertDoesNotThrow(() -> {
            assertEquals(
                    VelocityInternals.class,
                    VelocityInjector.class.getDeclaredField("internals").getType());
            VelocityInjector.class.getDeclaredMethod(
                    "resolveFinalProfileId",
                    MinecraftSessionHandler.class);
        });
    }
}
