package rig.ruuter.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.List;

@ConfigurationProperties(prefix = "payload-logging")
@ConstructorBinding
@Getter
@AllArgsConstructor
@ToString
@Slf4j
public class PayloadLoggingConfiguration implements InitializingBean {
    private boolean enabled;
    private List<PayloadLoggingRule> rules;

    @Override
    public void afterPropertiesSet() {
        log.debug("Payload logging configuration loaded: {}", this);
    }
}
