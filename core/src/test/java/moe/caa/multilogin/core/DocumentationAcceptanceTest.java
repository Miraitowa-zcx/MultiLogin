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
}
