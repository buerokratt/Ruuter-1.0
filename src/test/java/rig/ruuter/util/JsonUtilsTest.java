package rig.ruuter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import rig.ruuter.TestBase;
import rig.ruuter.enums.ActionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static rig.ruuter.util.JsonUtils.*;
import static rig.ruuter.util.StrUtils.toJson;

class JsonUtilsTest extends TestBase {

    @Test
    void postParametersReplaceTest() {
        JsonNode nodeFromString = toJson(
            "{\"register\":\"RR\",\"isikukood\":\"{$jwt_isikukood}\",\"service\":\"414\",\"parameters\":{\"isikukood\":\"{$jwt_isikukood}\",\"parameters\":{\"isikukood\":\"{$jwt_isikukood}\",\"isikukood1\":\"{$jwt_isikukood}\",\"parameters\":{\"isikukood\":\"{$jwt_isikukood}\"}}}}");
        findAndReplace(nodeFromString, "{$jwt_isikukood}", "\"11111111111\"");
        assertEquals(
            "{\"register\":\"RR\",\"isikukood\":\"11111111111\",\"service\":\"414\",\"parameters\":{\"isikukood\":\"11111111111\",\"parameters\":{\"isikukood\":\"11111111111\",\"isikukood1\":\"11111111111\",\"parameters\":{\"isikukood\":\"11111111111\"}}}}",
            nodeFromString.toString());
    }

    @Test
    void jsonSetTest() {
        JsonNode json = toJson("{\"test\":\"value\"}");
        jsonSet(json, "test", "changedValue");
        assertEquals(toJson("{\"test\":\"changedValue\"}"), json);
    }

    @Test
    void getActionTypeTest() {
        JsonNode conf = toJson(CONFIGURATION).get("search");
        ActionType at = getActionType(conf);
        assertEquals(ActionType.FWD, at);

        assertNull(getActionType(null));
        JsonNode nullActionTypeConf = ((ObjectNode) conf.deepCopy()).set("action_type", toJson("UNEXISTINGFOOBAR"));
        assertNull(getActionType(nullActionTypeConf));
    }


}
