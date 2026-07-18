package fun.ksnb.multilogin.velocity.main;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityPriorityTest {
    @Test
    void usesVelocity41NumericPriorityForFirstListeners() throws NoSuchMethodException {
        Method onPlayerJoin = GlobalListener.class.getDeclaredMethod(
                "onPlayerJoin",
                LoginEvent.class);
        Method onDisconnect = GlobalListener.class.getDeclaredMethod(
                "onDisconnect",
                DisconnectEvent.class);

        assertEquals(
                Short.MAX_VALUE - 1,
                onPlayerJoin.getAnnotation(Subscribe.class).priority());
        assertEquals(
                Short.MAX_VALUE - 1,
                onDisconnect.getAnnotation(Subscribe.class).priority());
    }
}
