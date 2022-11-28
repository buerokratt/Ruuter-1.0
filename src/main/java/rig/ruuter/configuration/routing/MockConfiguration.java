package rig.ruuter.configuration.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@Component
public class MockConfiguration implements ConfigurationWrapper {

    private JsonNode confNode;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public MockConfiguration(@Value("${ruuter.mock.path}") String configPath) {
        load(configPath);
    }

    private MockConfiguration() {

    }

    private void load(String configPath) {
        if (confNode == null) {
            confNode = toJson("{}");
            try {
                File configFolder = new File(configPath);
                if (configFolder.listFiles() != null) {
                    List<File> files = Files.walk(Paths.get(configFolder.getAbsolutePath()))
                            .filter(Files::isRegularFile).map(Path::toFile)
                            .filter(file -> (!file.isDirectory() || file.getName().endsWith(".json")))
                            .collect(Collectors.toList());

                    for (File file : files) {
                        ((ObjectNode) confNode)
                                .set(file.getName().replaceAll("\\.json", ""), mapper.readTree(file));
                    }
                }
            }
            catch (Exception e) {
                log.error("Error loading mock configuration.", e);
            }
        }
    }

    @Override
    public JsonNode find(String key) {
        return confNode.get(key);
    }
}
