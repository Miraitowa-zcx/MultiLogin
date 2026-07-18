package fun.ksnb.multilogin.velocity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityStartupSmokeTest {
    private static final String SUCCESS_MARKER =
            "Velocity 4.1 internal compatibility verified";

    @TempDir
    Path serverDirectory;

    @Test
    @Timeout(value = 150, unit = TimeUnit.SECONDS)
    void startsOfficialVelocityWithVerifiedInternalContract() throws Exception {
        Path velocityJar = requiredFileProperty("multilogin.velocityJar");
        Path pluginJar = requiredFileProperty("multilogin.pluginJar");
        Path pluginsDirectory = Files.createDirectories(serverDirectory.resolve("plugins"));
        Files.copy(velocityJar, serverDirectory.resolve("velocity.jar"));
        Files.copy(pluginJar, pluginsDirectory.resolve("MultiLogin.jar"));
        Files.writeString(
                serverDirectory.resolve("forwarding.secret"),
                "multilogin-smoke-secret",
                StandardCharsets.UTF_8);
        Files.writeString(
                serverDirectory.resolve("velocity.toml"),
                velocityConfig(unusedPort()),
                StandardCharsets.UTF_8);

        Process process = new ProcessBuilder(
                javaExecutable().toString(),
                "-jar",
                "velocity.jar")
                .directory(serverDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        StringBuffer log = new StringBuffer();
        Thread outputReader = new Thread(
                () -> readOutput(process, log),
                "velocity-smoke-output");
        outputReader.setDaemon(true);
        outputReader.start();

        try {
            waitForMarkerOrFailure(process, log, Duration.ofSeconds(120));
        } finally {
            stopOwnedProcess(process);
            outputReader.join(TimeUnit.SECONDS.toMillis(5));
        }

        String output = log.toString();
        assertTrue(output.contains(SUCCESS_MARKER), output);
        assertFalse(output.contains("NoSuchMethodException"), output);
        assertFalse(output.contains("VelocityCompatibilityException"), output);
    }

    private static void waitForMarkerOrFailure(
            Process process,
            StringBuffer log,
            Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            String current = log.toString();
            if (current.contains(SUCCESS_MARKER)
                    || current.contains("NoSuchMethodException")
                    || current.contains("VelocityCompatibilityException")) {
                return;
            }
            if (!process.isAlive()) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private static void readOutput(Process process, StringBuffer log) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append(System.lineSeparator());
            }
        } catch (IOException failure) {
            log.append("[smoke-reader-error] ").append(failure).append(System.lineSeparator());
        }
    }

    private static void stopOwnedProcess(Process process) throws Exception {
        if (!process.isAlive()) {
            return;
        }
        try {
            try (Writer writer = new OutputStreamWriter(
                    process.getOutputStream(),
                    StandardCharsets.UTF_8)) {
                writer.write("shutdown\n");
                writer.flush();
            }
            if (process.waitFor(10, TimeUnit.SECONDS)) {
                return;
            }
        } finally {
            if (process.isAlive()) {
                process.destroy();
            }
            if (process.isAlive() && !process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private static Path requiredFileProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing system property: " + name);
        }
        Path path = Path.of(value);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("File does not exist for " + name + ": " + path);
        }
        return path;
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String velocityConfig(int port) {
        return """
                config-version = "2.8"
                bind = "127.0.0.1:%d"
                motd = "MultiLogin smoke test"
                show-max-players = 1
                online-mode = true
                force-key-authentication = true
                prevent-client-proxy-connections = false
                player-info-forwarding-mode = "MODERN"
                forwarding-secret-file = "forwarding.secret"
                announce-forge = false
                kick-existing-players = false
                ping-passthrough = "DISABLED"
                sample-players-in-ping = false
                enable-player-address-logging = false

                [packet-limiter]
                interval = 7
                packets-per-second = -1
                bytes-per-second = -1
                decompressed-bytes-per-second = 5242880

                [servers]
                lobby = "127.0.0.1:30066"
                try = ["lobby"]

                [forced-hosts]

                [advanced]
                compression-threshold = 256
                compression-level = -1
                login-ratelimit = 3000
                connection-timeout = 5000
                read-timeout = 30000
                haproxy-protocol = false
                tcp-fast-open = false
                bungee-plugin-message-channel = true
                show-ping-requests = false
                failover-on-unexpected-server-disconnect = true
                announce-proxy-commands = true
                log-command-executions = false
                log-player-connections = false
                accepts-transfers = false
                enable-reuse-port = false
                command-rate-limit = 50
                forward-commands-if-rate-limited = true
                kick-after-rate-limited-commands = 0
                tab-complete-rate-limit = 10
                kick-after-rate-limited-tab-completes = 0

                [query]
                enabled = false
                port = %d
                map = "Velocity"
                show-plugins = false
                """.formatted(port, port);
    }
}
