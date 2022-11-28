package rig.ruuter.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class PayloadLogger {
    private static final Logger logger = LoggerFactory.getLogger(PayloadLogger.class.getName());
    static final Marker REQUEST_MARKER = MarkerFactory.getMarker("REQUEST");
    static final Marker RESPONSE_MARKER = MarkerFactory.getMarker("RESPONSE");

    public void logRequest(String format, Object... arguments) {
        logger.info(REQUEST_MARKER, format, arguments);
    }

    public void logResponse(String format, Object... arguments) {
        logger.info(RESPONSE_MARKER, format, arguments);
    }
}
