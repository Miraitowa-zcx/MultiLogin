package moe.caa.multilogin.core.configuration;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;

import static moe.caa.multilogin.core.configuration.SqlConfig.SqlBackend.POSTGRESQL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlConfigTest {
    @Test
    void readsPostgresqlBackend() throws Exception {
        CommentedConfigurationNode node = CommentedConfigurationNode.root();
        node.node("backend").set("POSTGRESQL");

        assertEquals(POSTGRESQL, SqlConfig.read(node).getBackend());
    }
}
