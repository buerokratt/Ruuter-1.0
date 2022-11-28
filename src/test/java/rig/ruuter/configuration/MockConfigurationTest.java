package rig.ruuter.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import rig.ruuter.configuration.routing.MockConfiguration;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockConfigurationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String configPath = "mock_configurations";

    @Test
    void showConfig() throws Exception {
        MockConfiguration conf = new MockConfiguration(configPath);
        JsonNode found = conf.find("TWO_OK");
        JsonNode read = mapper.readTree(new File(configPath + "/TWO_OK.json"));
        assertEquals(found, read);
    }

}
