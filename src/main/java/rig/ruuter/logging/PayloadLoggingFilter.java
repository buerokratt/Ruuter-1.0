package rig.ruuter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static rig.ruuter.logging.PayloadLogger.*;
import static rig.ruuter.logging.PayloadType.*;

@Component
@Slf4j
public class PayloadLoggingFilter extends TurboFilter implements InitializingBean {
    private final PayloadLoggingValidator payloadLoggingValidator;

    public PayloadLoggingFilter(PayloadLoggingValidator payloadLoggingValidator) {
        this.payloadLoggingValidator = payloadLoggingValidator;
    }

    @Override
    public void afterPropertiesSet() {
        log.debug("Attaching PayloadLoggingFilter");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.addTurboFilter(this);
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        return getPayloadType(marker)
                .map(this::isLoggingEnabled)
                .map(enabled -> enabled ? FilterReply.NEUTRAL : FilterReply.DENY)
                .orElse(FilterReply.NEUTRAL);
    }

    private Optional<PayloadType> getPayloadType(Marker marker) {
        if (marker != null) {
            if (marker.contains(REQUEST_MARKER)) {
                return Optional.of(REQUEST);
            }
            if (marker.contains(RESPONSE_MARKER)) {
                return Optional.of(RESPONSE);
            }
        }
        return Optional.empty();
    }

    private Boolean isLoggingEnabled(PayloadType type) {
        String currentConfiguration = LoggingContext.getConfigContext();
        String currentDestination = LoggingContext.getDestinationContext();
        return payloadLoggingValidator.isLoggingEnabled(currentConfiguration, currentDestination, type);
    }
}
