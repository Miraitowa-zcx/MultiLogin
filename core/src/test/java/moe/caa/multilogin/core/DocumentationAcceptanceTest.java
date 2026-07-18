package moe.caa.multilogin.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationAcceptanceTest {
    @Test
    void documentsSupportedRuntimeBuildSelectionAndPostgresql() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String documentation = Files.readString(root.resolve("README.md"))
                + Files.readString(root.resolve("README.en.md"))
                + Files.readString(root.resolve("core/src/main/resources/config.yml"));

        for (String required : new String[]{
                "JDK 25",
                "4.1.0-SNAPSHOT",
                "velocityBuild",
                "POSTGRESQL",
                "5432",
                "jdbc:postgresql"
        }) {
            assertTrue(documentation.contains(required),
                    () -> "Documentation is missing required token: " + required);
        }
    }

    @Test
    void documentsSecureChatMixedModeAndSupportedClientRange() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String chinese = Files.readString(root.resolve("README.md"));
        String english = Files.readString(root.resolve("README.en.md"));

        for (String documentation : new String[]{chinese, english}) {
            for (String required : new String[]{
                    "enforce-secure-profile=false",
                    "1.21.x",
                    "26.x",
                    "modern forwarding"
            }) {
                assertTrue(documentation.contains(required),
                        () -> "Secure chat documentation is missing required token: " + required);
            }
        }
    }
}
