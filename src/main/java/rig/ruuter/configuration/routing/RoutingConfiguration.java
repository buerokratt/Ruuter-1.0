package rig.ruuter.configuration.routing;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class RoutingConfiguration implements ConfigurationWrapper {

    @Override
    public JsonNode find(String key) {
        return Configuration.find(key);
    }
}
