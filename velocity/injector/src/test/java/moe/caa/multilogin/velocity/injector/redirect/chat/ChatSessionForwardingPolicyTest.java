package moe.caa.multilogin.velocity.injector.redirect.chat;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static moe.caa.multilogin.velocity.injector.redirect.chat.ChatSessionForwardingPolicy.Decision.FORWARD_SIGNED;
import static moe.caa.multilogin.velocity.injector.redirect.chat.ChatSessionForwardingPolicy.Decision.USE_UNSIGNED_FALLBACK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSessionForwardingPolicyTest {
    @Test
    void forwardsOnlyWhenValidatorAcceptsTheFinalUuid() {
        UUID finalUuid = UUID.randomUUID();
        AtomicReference<UUID> validatedUuid = new AtomicReference<>();
        ChatSessionForwardingPolicy accepted = new ChatSessionForwardingPolicy(
                (key, uuid) -> {
                    validatedUuid.set(uuid);
                    return true;
                });
        ChatSessionForwardingPolicy rejected =
                new ChatSessionForwardingPolicy((key, uuid) -> false);

        assertEquals(FORWARD_SIGNED,
                accepted.decide(finalUuid, TestIdentifiedKey.valid()));
        assertEquals(finalUuid, validatedUuid.get());
        assertEquals(USE_UNSIGNED_FALLBACK,
                rejected.decide(finalUuid, TestIdentifiedKey.valid()));
        assertEquals(USE_UNSIGNED_FALLBACK, accepted.decide(finalUuid, null));
        assertEquals(USE_UNSIGNED_FALLBACK,
                accepted.decide(finalUuid, TestIdentifiedKey.expired()));
    }

    private record TestIdentifiedKey(Instant expiry) implements IdentifiedKey {
        static TestIdentifiedKey valid() {
            return new TestIdentifiedKey(Instant.now().plusSeconds(3600));
        }

        static TestIdentifiedKey expired() {
            return new TestIdentifiedKey(Instant.now().minusSeconds(1));
        }

        @Override
        public PublicKey getSignedPublicKey() {
            return null;
        }

        @Override
        public boolean verifyDataSignature(byte[] signature, byte[]... data) {
            return true;
        }

        @Override
        public UUID getSignatureHolder() {
            return null;
        }

        @Override
        public Revision getKeyRevision() {
            return Revision.LINKED_V2;
        }

        @Override
        public PublicKey getSigner() {
            return null;
        }

        @Override
        public Instant getExpiryTemporal() {
            return expiry;
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }
    }
}
