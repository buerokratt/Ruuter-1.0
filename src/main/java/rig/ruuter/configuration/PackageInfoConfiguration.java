package rig.ruuter.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Component
@PropertySource("classpath:heartbeat.properties")
public class PackageInfoConfiguration {

    @Value("${app.name:}")
    private String appName;

    @Value("${app.version:}")
    private String version;

    @Value("${app.packaging.time:}")
    private long packagingTime;

}
