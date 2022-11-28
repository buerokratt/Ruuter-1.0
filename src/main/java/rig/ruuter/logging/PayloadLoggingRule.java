package rig.ruuter.logging;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class PayloadLoggingRule {
    private final String enable;
    private final String disable;

    public PayloadLoggingRule(String enable, String disable) {
        if ((enable != null) == (disable != null)) {
            throw new IllegalArgumentException("Exactly one of enable/disable has to be specified");
        }
        this.enable = enable;
        this.disable = disable;
    }

    public static PayloadLoggingRule enable(String enable) {
        return new PayloadLoggingRule(enable, null);
    }

    public static PayloadLoggingRule disable(String disable) {
        return new PayloadLoggingRule(null, disable);
    }

    String getPattern() {
        return enable != null ? enable : disable;
    }
}
