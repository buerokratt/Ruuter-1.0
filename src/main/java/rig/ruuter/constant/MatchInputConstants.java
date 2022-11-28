package rig.ruuter.constant;

public class MatchInputConstants {

    /**
     * Expected attributes for matching input in validation service
     * @see rig.ruuter.service.ValidationService
     */
    public static final String INPUT = "input";
    public static final String VALIDATE_AGAINST = "validate_against";
    public static final String VALIDATION_TYPE = "validation_type";
    /**
     * Allowed attribute names as strings
     */
    public static final String[] ALLOWED_ATTRIBUTES = new String[] {
        "registrikood",
        "registry_code",
        "id_code",
        "chat_id",
        "authorities",
        "is_active"
    };

}
