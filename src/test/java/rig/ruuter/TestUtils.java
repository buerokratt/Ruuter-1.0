package rig.ruuter;

import com.fasterxml.jackson.databind.JsonNode;
import rig.ruuter.util.StrUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    private static final String JSON_TESTS_ROOT_FOLDER = "src/test/resources/";

    public static JsonNode getJsonNode(String cfgPath) throws IOException {
        Path hcSetRoleConfigPath =
                Paths.get(JSON_TESTS_ROOT_FOLDER.concat(cfgPath));
        return StrUtils.toJson(new String(Files.readAllBytes(hcSetRoleConfigPath)));
    }

}
