package moe.caa.multilogin.velocity.injector.handler;

import com.velocitypowered.api.util.GameProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiInitialLoginSessionHandlerTest {
    @Test
    void passesServerIdHashToAuthenticatedSession() {
        AtomicReference<CapturedSession> captured = new AtomicReference<>();
        AuthSessionFactory factory = (server, inbound, profile, onlineMode, serverIdHash) -> {
            captured.set(new CapturedSession(profile, onlineMode, serverIdHash));
            return null;
        };
        MultiInitialLoginSessionHandler handler =
                new MultiInitialLoginSessionHandler(null, null, null, factory);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "TestPlayer", List.of());

        handler.activateAuthenticatedSession(profile, "expected-server-id");

        assertSame(profile, captured.get().profile());
        assertTrue(captured.get().onlineMode());
        assertEquals("expected-server-id", captured.get().serverIdHash());
    }

    private record CapturedSession(
            GameProfile profile,
            boolean onlineMode,
            String serverIdHash) {
    }
}
