package fun.ksnb.multilogin.velocity.main;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityTargetTest {
    @Test
    void parsesCompileTargetProperties() {
        Properties properties = new Properties();
        properties.setProperty("velocity.version", "4.1.0-SNAPSHOT");
        properties.setProperty("velocity.build", "8");
        properties.setProperty("velocity.commit", "9eb338bd1c785fe822cb4aa7d48489627a05973b");
        properties.setProperty(
                "velocity.sha256",
                "95ba0941466ec883e698cb5c69ba032726a5ce44eb81d0ce2fa1dc89b98524e1");

        VelocityTarget target = VelocityTarget.from(properties);

        assertEquals("4.1.0-SNAPSHOT", target.version());
        assertEquals(8, target.build());
        assertEquals("9eb338bd1c785fe822cb4aa7d48489627a05973b", target.commit());
        assertEquals(
                "95ba0941466ec883e698cb5c69ba032726a5ce44eb81d0ce2fa1dc89b98524e1",
                target.sha256());
    }

    @Test
    void loadsGeneratedCompileTarget() {
        assertEquals("4.1.0-SNAPSHOT", VelocityTarget.load().version());
    }
}
