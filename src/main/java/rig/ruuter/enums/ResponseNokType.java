package rig.ruuter.enums;

import java.util.Arrays;

public enum ResponseNokType {

    // See rig.ruuter.constant.Constant.RUUTER_BASE_RESPONSE_STRUCTURE for ruuter response json structure.

    /**
     * immediatly return error code to client
     * <p>
     * no further requests will be made
     */
    STOP("stop"),

    /**
     * does nothing with the error
     * <p>
     * does not report it
     * <p>
     * proceeds with configuration's other requests
     */
    PROCEED("proceed"),

    /**
     * do error handling request
     * <p>
     * if no error handling is specified, tries to add the response's body to error property of the result
     * <p>
     * continue with configuration's other requests
     */
    ON_ERROR("on_error"),

    /**
     * same as ON_ERROR but do not continue with configuration's other requests
     * <p>
     * instead no further requests will be made and tries to return the same error code to client
     */
     ON_ERROR_AND_STOP("on_error_and_stop"),

    /**
     * attempt to add response's body to result body
     * <p>
     * appends null if no body present
     * <p>
     * continue with configuration's other requests
     * <p>
     * does not indicate that error occured
     */
    IGNORE_ERROR("ignore_error");

    private String value;

    ResponseNokType(String code) {
        this.value = code;
    }

    // suppress falsepositive sonarqube warning
    @SuppressWarnings("squid:S2095")
    public static ResponseNokType fromValue(String value) {
        return Arrays.stream(ResponseNokType.values()).filter(t -> t.getValue().equals(value)).findFirst().orElse(null);
    }

    public String getValue() {
        return value;
    }

}
