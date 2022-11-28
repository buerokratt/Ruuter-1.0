package rig.ruuter.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.List;

@ConfigurationProperties("ip-whitelist")
@ConstructorBinding
@Getter
@AllArgsConstructor
@ToString
@Slf4j
public class IpWhitelistConfiguration implements InitializingBean {
    private List<Route> routes;

    @Override
    public void afterPropertiesSet() {
        log.debug("IP whitelist configuration loaded: {}", this);
    }

    @Getter
    @AllArgsConstructor
    @ToString
    public static class Route {
        private List<String> patterns;
        private List<String> ips;
    }
}
