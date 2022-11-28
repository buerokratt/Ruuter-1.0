package rig.ruuter.logging;


import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rig.ruuter.logging.PayloadLoggingRule.disable;
import static rig.ruuter.logging.PayloadLoggingRule.enable;
import static rig.ruuter.logging.PayloadType.REQUEST;

class PayloadLoggingValidatorTest {

    @Test
    void payloadLoggingDisabled() {
        PayloadLoggingValidator validator = createValidator(false, List.of(enable("*")));
        assertFalse(validator.isLoggingEnabled("config", "destination", REQUEST));
    }

    @Test
    void payloadLoggingEnabled() {
        PayloadLoggingValidator validator = createValidator(true, List.of(enable("*")));
        assertTrue(validator.isLoggingEnabled("config", "destination", REQUEST));
    }

    @Test
    void blockAll() {
        PayloadLoggingValidator validator = createValidator(true, List.of(disable("*")));
        assertFalse(validator.isLoggingEnabled("config", "destination", REQUEST));
    }

    @Test
    void multipleRules() {
        PayloadLoggingValidator validator = createValidator(true,
            List.of(enable("config"), disable("*")));
        assertTrue(validator.isLoggingEnabled("config", "destination", REQUEST));
        assertFalse(validator.isLoggingEnabled("other", "destination", REQUEST));
    }

    private PayloadLoggingValidator createValidator(boolean enabled, List<PayloadLoggingRule> rules) {
        PayloadLoggingConfiguration configuration = new PayloadLoggingConfiguration(enabled, rules);
        PayloadLoggingValidator validator = new PayloadLoggingValidator(configuration);
        return validator;
    }
}
