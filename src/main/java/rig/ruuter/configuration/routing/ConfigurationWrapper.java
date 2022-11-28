package rig.ruuter.configuration.routing;

import com.fasterxml.jackson.databind.JsonNode;

public interface ConfigurationWrapper {

    JsonNode find(String key);
}
