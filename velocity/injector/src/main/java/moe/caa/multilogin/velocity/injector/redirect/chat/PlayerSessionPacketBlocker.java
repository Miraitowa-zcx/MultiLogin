package moe.caa.multilogin.velocity.injector.redirect.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import moe.caa.multilogin.api.internal.logger.LoggerProvider;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class PlayerSessionPacketBlocker implements MinecraftPacket {
    private final Function<MinecraftSessionHandler, UUID> finalProfileIdResolver;
    private final ChatSessionForwardingPolicy forwardingPolicy;
    private UUID sessionId;
    private IdentifiedKey identifiedKey;
    private boolean hasKey = true;

    public PlayerSessionPacketBlocker(
            Function<MinecraftSessionHandler, UUID> finalProfileIdResolver) {
        this(finalProfileIdResolver, new ChatSessionForwardingPolicy());
    }

    PlayerSessionPacketBlocker(
            Function<MinecraftSessionHandler, UUID> finalProfileIdResolver,
            ChatSessionForwardingPolicy forwardingPolicy) {
        this.finalProfileIdResolver = Objects.requireNonNull(
                finalProfileIdResolver,
                "finalProfileIdResolver");
        this.forwardingPolicy = Objects.requireNonNull(
                forwardingPolicy,
                "forwardingPolicy");
    }

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        hasKey = true;
        byteBuf.markReaderIndex();
        try {
            sessionId = ProtocolUtils.readUuid(byteBuf);
            identifiedKey = ProtocolUtils.readPlayerKey(protocolVersion, byteBuf);
        } catch (Throwable t) {
            byteBuf.resetReaderIndex();
            LoggerProvider.getLogger().debug("Failed to decode player session packet.", t);
            hasKey = false;
        }
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        //不发送ChatSession
        if (hasKey) {
            ProtocolUtils.writeUuid(byteBuf, sessionId);
            ProtocolUtils.writePlayerKey(byteBuf, identifiedKey);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        if (!hasKey || identifiedKey == null) {
            return true;
        }
        try {
            UUID finalProfileId = finalProfileIdResolver.apply(minecraftSessionHandler);
            return forwardingPolicy.decide(finalProfileId, identifiedKey)
                    != ChatSessionForwardingPolicy.Decision.FORWARD_SIGNED;
        } catch (RuntimeException failure) {
            LoggerProvider.getLogger().debug(
                    "Unable to validate player chat session; using unsigned fallback.",
                    failure);
            return true;
        }
    }
}
