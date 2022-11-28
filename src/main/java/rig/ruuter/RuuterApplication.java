package rig.ruuter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import rig.ruuter.configuration.AOPConfiguration;
import rig.ruuter.configuration.IpWhitelistConfiguration;
import rig.ruuter.configuration.RestConfiguration;
import rig.ruuter.configuration.WebClientConfiguration;
import rig.ruuter.configuration.routing.Configuration;
import rig.ruuter.logging.PayloadLoggingConfiguration;

@Import({
        AOPConfiguration.class,
        RestConfiguration.class,
        WebClientConfiguration.class
})
@EnableConfigurationProperties({PayloadLoggingConfiguration.class, IpWhitelistConfiguration.class})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
public class RuuterApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(RuuterApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void executeAfterSpringAppStarted() {
        Configuration.load();
    }
}
