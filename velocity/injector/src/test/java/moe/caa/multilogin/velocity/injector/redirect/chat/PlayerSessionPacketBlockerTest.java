package moe.caa.multilogin.velocity.injector.redirect.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSessionPacketBlockerTest {
    private static final MinecraftSessionHandler TEST_HANDLER =
            new MinecraftSessionHandler() { };

    @Test
    void exposesFinalProfileResolverForConditionalForwarding() {
        assertDoesNotThrow(() -> PlayerSessionPacketBlocker.class.getDeclaredConstructor(
                Function.class,
                ChatSessionForwardingPolicy.class));
    }

    @Test
    void forwardsValidSessionForFinalProfileUuid() throws Exception {
        UUID finalUuid = UUID.randomUUID();
        PlayerSessionPacketBlocker packet = new PlayerSessionPacketBlocker(
                ignored -> finalUuid,
                new ChatSessionForwardingPolicy((key, uuid) -> finalUuid.equals(uuid)));
        setField(packet, "identifiedKey", TestIdentifiedKey.valid());

        assertFalse(packet.handle(TEST_HANDLER));
    }

    @Test
    void blocksMalformedSessionAfterPreviousDecodedKey() throws Exception {
        PlayerSessionPacketBlocker packet = new PlayerSessionPacketBlocker(
                ignored -> UUID.randomUUID(),
                new ChatSessionForwardingPolicy((key, uuid) -> true));
        setField(packet, "identifiedKey", TestIdentifiedKey.valid());
        ByteBuf malformed = Unpooled.buffer().writeByte(1);
        try {
            packet.decode(
                    malformed,
                    ProtocolUtils.Direction.SERVERBOUND,
                    ProtocolVersion.MINECRAFT_1_21);
        } finally {
            malformed.release();
        }

        assertTrue(packet.handle(TEST_HANDLER));
    }

    @Test
    void blocksWhenFinalProfileCannotBeResolved() throws Exception {
        PlayerSessionPacketBlocker packet = new PlayerSessionPacketBlocker(
                ignored -> {
                    throw new IllegalStateException("missing player");
                },
                new ChatSessionForwardingPolicy((key, uuid) -> true));
        setField(packet, "identifiedKey", TestIdentifiedKey.valid());

        assertDoesNotThrow(() -> assertTrue(packet.handle(TEST_HANDLER)));
    }

    @Test
    void preservesValidSessionWireFields() throws Exception {
        ByteBuf input = encodedSession(Instant.now().plusSeconds(3600));
        byte[] expected = ByteBufUtil.getBytes(
                input,
                input.readerIndex(),
                input.readableBytes());
        PlayerSessionPacketBlocker packet = new PlayerSessionPacketBlocker(
                ignored -> UUID.randomUUID(),
                new ChatSessionForwardingPolicy((key, uuid) -> true));
        ByteBuf output = Unpooled.buffer();
        try {
            packet.decode(
                    input,
                    ProtocolUtils.Direction.SERVERBOUND,
                    ProtocolVersion.MINECRAFT_1_21);
            packet.encode(
                    output,
                    ProtocolUtils.Direction.SERVERBOUND,
                    ProtocolVersion.MINECRAFT_1_21);

            assertArrayEquals(expected, ByteBufUtil.getBytes(output));
        } finally {
            input.release();
            output.release();
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static ByteBuf encodedSession(Instant expiry)
            throws GeneralSecurityException {
        KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        IdentifiedKey key = new IdentifiedKeyImpl(
                IdentifiedKey.Revision.LINKED_V2,
                pair.getPublic(),
                expiry,
                new byte[]{1, 2, 3});
        ByteBuf buffer = Unpooled.buffer();
        ProtocolUtils.writeUuid(buffer, UUID.randomUUID());
        ProtocolUtils.writePlayerKey(buffer, key);
        return buffer;
    }

    private record TestIdentifiedKey(Instant expiry) implements IdentifiedKey {
        static TestIdentifiedKey valid() {
            return new TestIdentifiedKey(Instant.now().plusSeconds(3600));
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
