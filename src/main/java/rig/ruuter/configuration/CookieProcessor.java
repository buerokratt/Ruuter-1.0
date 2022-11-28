package rig.ruuter.configuration;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieProcessor implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    @Value("${ruuter.cookie.sameSitePolicy:Lax}")
    private String sameSitePolicy;
    @Override
    public void customize(TomcatServletWebServerFactory server) {
        server.getTomcatContextCustomizers().add(context -> {
            Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
            cookieProcessor.setSameSiteCookies(sameSitePolicy);
            context.setCookieProcessor(cookieProcessor);
        });
    }
}
