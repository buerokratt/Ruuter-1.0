package rig.ruuter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import rig.ruuter.enums.ActionType;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static rig.ruuter.constant.Constant.ACTION_TYPE;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
public class JsonUtils {

    private JsonUtils() {
    }

    public static JsonNode pojoToJson(Object object) {
        try {
            return toJson((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert java object to json", e);
            return null;
        }
    }

    /**
     * finds specified value {$@findBy} in the @node and replaces it with @replaceWith
     *
     * @param node        a json node
     * @param findBy      string to find, in json it should be {$findBy}
     * @param replaceWith replaces with this if found
     */
    public static void findAndReplace(JsonNode node, String findBy, String replaceWith) {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            JsonNode value = field.getValue();
            if (value.isContainerNode()) {
                findAndReplace(value, findBy, replaceWith);
            } else if (findBy.equalsIgnoreCase(value.textValue())) {
                ((ObjectNode) node).set(field.getKey(), replaceWith == null ? null : toJson(replaceWith));
            }
        }
    }

    public static void jsonSet(JsonNode node, String fieldName, String toSet) {
        ((ObjectNode) node).set(fieldName, toJson(toSet));
    }

    public static void jsonSet(JsonNode node, String fieldName, JsonNode toSet) {
        ((ObjectNode) node).set(fieldName, toSet);
    }

    public static ActionType getActionType(JsonNode conf) {
        if (conf == null) {
            log.warn("Configuration is null.");
            return null;
        }
        ActionType a = ActionType
                .fromCode(Optional.of(conf.get(ACTION_TYPE).asText()).orElse(null));
        if (a == null) {
            log.warn("No action type specified in configuration {}", conf);
        }
        return a;
    }

}
