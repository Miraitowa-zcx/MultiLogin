package moe.caa.multilogin.velocity.injector.redirect.chat;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;

final class ChatSessionForwardingPolicy {
    enum Decision {
        FORWARD_SIGNED,
        USE_UNSIGNED_FALLBACK
    }

    private final BiPredicate<IdentifiedKey, UUID> validator;

    ChatSessionForwardingPolicy() {
        this(ChatSessionForwardingPolicy::validateVelocityKey);
    }

    ChatSessionForwardingPolicy(BiPredicate<IdentifiedKey, UUID> validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    Decision decide(UUID finalProfileId, IdentifiedKey key) {
        if (finalProfileId == null || key == null || key.hasExpired()) {
            return Decision.USE_UNSIGNED_FALLBACK;
        }
        return validator.test(key, finalProfileId)
                ? Decision.FORWARD_SIGNED
                : Decision.USE_UNSIGNED_FALLBACK;
    }

    private static boolean validateVelocityKey(IdentifiedKey key, UUID finalProfileId) {
        return key instanceof IdentifiedKeyImpl velocityKey
                && velocityKey.internalAddHolder(finalProfileId);
    }
}
