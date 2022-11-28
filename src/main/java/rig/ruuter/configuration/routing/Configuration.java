package rig.ruuter.configuration.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static rig.ruuter.constant.Constant.SKIP;
import static rig.ruuter.util.FileUtils.readFile;
import static rig.ruuter.util.FileUtils.writeFile;
import static rig.ruuter.util.StrUtils.toJson;
import static rig.ruuter.util.StrUtils.unWrapQuotes;

/**
 * this class loads, holds and provides the means to retrieve the configuration for services
 */
@Slf4j
@Component
public final class Configuration {

    private static String configPath;
    private static String configEnvFilePath;
    private static String configDeployFolder;

    // this holds configuration during uptime
    private static JsonNode confNode;

    private static ObjectMapper mapper = new ObjectMapper();

    private Configuration() {
    }

    /**
     * Loads json configurations from specified @folder,
     * replaces endpoints to corresponding values according to set profile.
     * <p>
     * See pom.xml profiles and corresponding env files in environments/
     */
    // suppress falsepositive sonarqube warning
    @SuppressWarnings("squid:S2095")
    public static void load() {
        log.info("Ruuter environment file path {}", configEnvFilePath);
        log.info("Configuration templates folder {}", configPath);
        try {
            confNode = toJson("{}");
            File configFolder = valid(configPath);
            if (configFolder != null && configFolder.listFiles() != null) {

                // Get urls of environment specified by maven profile
                JsonNode environmentUrls = mapper.readTree(readFile(configEnvFilePath));
                // Get json configuration paths
                List<Path> paths = Files.walk(Paths.get(configFolder.getAbsolutePath()))
                        .filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path path : paths) {
                    File file = path.toFile();
                    if (file.isDirectory() || !file.getName().endsWith(".json")) {
                        continue;
                    }
                    // read config file and replace env url into it
                    JsonNode conf = mapper.readTree(getConfigOfEnvironment(environmentUrls, file));
                    if (!conf.getNodeType().equals(JsonNodeType.OBJECT)) {
                        terminate("Configuration node is not a json object. File " + file.getAbsolutePath());
                    }
                    // Add it to ruuter config memory
                    ((ObjectNode) confNode).set(file.getName().replaceAll("\\.json", ""), conf);
                }
            } else {
                terminate("Configuration folder is null");
            }
        } catch (Exception e) {
            log.error("Error loading configuration.", e);
            terminate("Couldn't load configuration. See error above.");
        }
        // Write config to deploy folder
//        writeDeployedconfiguration();
    }

    private static String getConfigOfEnvironment(JsonNode environmentUrls, File configFile) throws IOException {
        String configurationNode = readFile(configFile.getAbsolutePath());
        Iterator<Map.Entry<String, JsonNode>> urls = environmentUrls.fields();
        while (urls.hasNext()) {
            Map.Entry<String, JsonNode> entry = urls.next();
            String key = "{" + entry.getKey() + "}";
            while (configurationNode.contains(key)) {
                configurationNode = configurationNode.replace(key, unWrapQuotes(entry.getValue().toString()));
            }
        }
        return configurationNode;
    }

    private static void writeDeployedconfiguration() {
        FileUtils.deleteQuietly(new File(configDeployFolder));
        confNode.fields().forEachRemaining(node -> {
            try {
                writeFile(configDeployFolder, node.getKey() + ".json", mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(node.getValue()));
            } catch (Exception e) {
                terminate("Couldn't write deploy config.");
            }
        });
        log.info("Configuration loaded and (over)written to {}", configDeployFolder);
    }

    /**
     * @param key identifying a part of configuration (json parameter name )
     * @return a sub-node defined by parameter name {@code key} is returned
     * (the sub node is retrieved from a deep copy of the original node)
     */
    public static JsonNode find(String key) {
        return Objects.requireNonNull(get()).get(key);
    }

    private static JsonNode get() {
        return confNode == null ? null : confNode.deepCopy();
    }

    private static File valid(String folder) {
        File configFolder = null;
        if (folder != null) {
            configFolder = new File(folder);
            if (!configFolder.exists()) {
                terminate(
                        "No configuration file found from specified folder: " + folder);
            }
        }else {
            terminate("Missing configuration folder.");
        }
        return configFolder;
    }

    private static void terminate(String errorMsg) {
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }


    @Value("${ruuter.config.path}")
    public void setConfigPath(String newPath) {
        if (configPath == null) {
            configPath = newPath;
        }
    }

    @Value("${ruuter.config.env.file.path}")
    public void setConfigEnvFilePath(String newPath) {
        if (configEnvFilePath == null) {
            configEnvFilePath = newPath;
        }
    }

    @Value("${ruuter.config.deploy.folder}")
    public void setConfigDeployFolder(String newPath) {
        if (configDeployFolder == null) {
            configDeployFolder = newPath;
        }
    }

    /**
     *
     * @param endpointNode check this node's skippable status
     * @return if the node has boolean skip attribute then return it, else return false
     */
    public static boolean skippableConfiguration(JsonNode endpointNode) {
        return endpointNode.has(SKIP) && endpointNode.get(SKIP).booleanValue();
    }

}
