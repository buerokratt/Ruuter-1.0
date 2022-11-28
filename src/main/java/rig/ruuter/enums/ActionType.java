package rig.ruuter.enums;

import java.util.Arrays;

public enum ActionType {

    FWD, BLOGIC, BOOLEAN,

    /**
     * returns a download
     */
    DOWNLOAD,

    /**
     * returns a json with file name and it's location on disc
     */
    DOWNLOAD_JSON;

    // suppress falsepositive sonarqube warning
    @SuppressWarnings("squid:S2095")
    public static ActionType fromCode(String code) {
        return Arrays.stream(ActionType.values()).filter(a -> a.name().equals(code)).findFirst().orElse(null);
    }

}
