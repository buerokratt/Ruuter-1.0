package rig.ruuter.enums;

import com.fasterxml.jackson.databind.JsonNode;
import rig.ruuter.service.RequestBodyMappingUtils;

/**
 * contains string values that are used as annotation for mapping one json to another
 * @see RequestBodyMappingUtils#extract(JsonNode, String, Splitter)
 */
public enum Splitter {

    FROM_INCOMING_BODY("#"), FROM_CONFIG_PARAM("$"), FUNCTION_DESIGNATOR("$_");

    String val;

    Splitter(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

}
