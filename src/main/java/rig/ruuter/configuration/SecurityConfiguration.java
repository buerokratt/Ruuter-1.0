package rig.ruuter.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.List;

import static java.lang.String.format;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final IpWhitelistConfiguration ipWhitelistConfiguration;

    public SecurityConfiguration(IpWhitelistConfiguration ipWhitelistConfiguration) {
        super(true);
        this.ipWhitelistConfiguration = ipWhitelistConfiguration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (ipWhitelistConfiguration.getRoutes() != null) {
            for (IpWhitelistConfiguration.Route route : ipWhitelistConfiguration.getRoutes()) {
                http.authorizeRequests().antMatchers(route.getPatterns().toArray(String[]::new))
                    .access(getAllowedIps(route.getIps()));
            }
        }
        http.authorizeRequests().anyRequest().permitAll().and().anonymous().principal("anonymous");
    }

    private String getAllowedIps(List<String> ips) {
        return ips.stream().reduce("", (result, element) -> {
            if (result.equals("")) {
                return format("hasIpAddress('%s')", element);
            }
            return result + format(" or hasIpAddress('%s')", element);
        });
    }
}
