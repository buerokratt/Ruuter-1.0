package rig.ruuter.logging;

import org.slf4j.MDC;

public class LoggingContext {
    static final String CONF_MDC_KEY = "ruuterConf";
    static final String DESTINATION_MDC_KEY = "ruuterDestination";

    public static void setConfigContext(String configCode) {
        MDC.put(CONF_MDC_KEY, configCode);
    }

    public static void setDestinationContext(String destinationKey) {
        MDC.put(DESTINATION_MDC_KEY, destinationKey);
    }

    public static String getConfigContext() {
        return MDC.get(CONF_MDC_KEY);
    }

    public static String getDestinationContext() {
        return MDC.get(DESTINATION_MDC_KEY);
    }

    public static void clearContext() {
        MDC.remove(DESTINATION_MDC_KEY);
        MDC.remove(CONF_MDC_KEY);
    }
}
