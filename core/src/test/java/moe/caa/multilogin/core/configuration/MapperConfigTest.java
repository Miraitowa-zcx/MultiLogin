package moe.caa.multilogin.core.configuration;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperConfigTest {
    @TempDir
    Path dataDirectory;

    @ParameterizedTest(name = "protocol {0} uses ChatSessionUpdate packet 0x{1}")
    @CsvSource({
            "767, 07", "768, 08", "769, 08", "770, 08",
            "771, 09", "772, 09", "773, 09", "774, 09",
            "775, 0A", "776, 0A"
    })
    void coversMinecraft121Through26(int protocol, String expectedHex) {
        MapperConfig config = new MapperConfig(dataDirectory.toFile());
        int actual = config.getPacketMapping().floorEntry(protocol).getValue();

        assertEquals(Integer.parseInt(expectedHex, 16), actual);
    }
}
