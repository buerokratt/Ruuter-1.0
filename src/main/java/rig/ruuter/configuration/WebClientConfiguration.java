package rig.ruuter.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import rig.ruuter.util.RestUtils;

import javax.net.ssl.SSLException;
import java.util.Optional;

import static rig.ruuter.constant.Constant.CONNECT_TIMEOUT_MS;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ImportAutoConfiguration({
        SimpleReactiveDiscoveryClientAutoConfiguration.class,
})
public class WebClientConfiguration {
    private static final String DEFAULT_CONNECTION_TIMEOUT_PROPERTY = "ruuter.reactive.client.default.connection.timeout";

    private final Environment environment;
    private final Optional<ExchangeFilterFunction> loadBalancerExchangeFilterFunction;

    @Bean
    RestUtils restUtils(AutowireCapableBeanFactory beanFactory) {
        return new RestUtils(beanFactory);
    }

    @Bean
    BeanPostProcessor fixDiscoveryInstanceSchemes(SimpleReactiveDiscoveryProperties simpleDiscoveryProperties) {
        return new FixDiscoveryInstanceSchemeBeanPostProcessor(simpleDiscoveryProperties);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @ConditionalOnProperty(value = "ruuter.reactive.client.load-balancer.enabled", havingValue = "true")
    WebClient loadBalancedWebClient(WebClient.Builder webClientBuilder, JsonNode destinationNode) {
        log.debug("Creating load balanced webclient");
        return getClient(webClientBuilder, destinationNode)
                .filters(fs -> fs.add(loadBalancerExchangeFilterFunction.orElseThrow()))
                .build();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @ConditionalOnProperty(value = "ruuter.reactive.client.load-balancer.enabled", havingValue = "false", matchIfMissing = true)
    WebClient singleTargetWebClient(WebClient.Builder webClientBuilder, JsonNode destinationNode) {
        log.debug("Creating single target webclient");
        return getClient(webClientBuilder, destinationNode)
                .build();
    }

    private WebClient.Builder getClient(WebClient.Builder webClientBuilder, JsonNode destinationNode) {
        try {
            return webClientBuilder.clientConnector(getReactorClientHttpConnector(destinationNode));
        } catch (Exception e) {
            log.error("Cannot create web client", e);
            throw new IllegalStateException("Cannot create web client");
        }
    }

    private ReactorClientHttpConnector getReactorClientHttpConnector(JsonNode destinationNode) {
        HttpClient client = HttpClient.create()
                .compress(true)
                .secure((sslContextSpec) -> sslContextSpec.sslContext(webClientSslContext()))
                .tcpConfiguration(tcp -> {
                    if (destinationNode != null && destinationNode.has(CONNECT_TIMEOUT_MS)) {
                        int connectTimeout = destinationNode.get(CONNECT_TIMEOUT_MS).asInt();
                        if (connectTimeout > 0) {
                            log.debug("Client connection timeout set to {}ms", connectTimeout);
                            return tcp.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
                        }
                        log.debug("Client without connection timeout");
                        return tcp;
                    } else {
                        Integer defaultConnectionTimeout = environment.getProperty(DEFAULT_CONNECTION_TIMEOUT_PROPERTY, Integer.class, 10000);
                        log.debug("Client connection timeout set to default {}ms", defaultConnectionTimeout);
                        return tcp.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, defaultConnectionTimeout);
                    }
                });

        return new ReactorClientHttpConnector(client);
    }

    @Bean
    SslContext webClientSslContext() {
        try {
            // TODO: make webclient ssl context configurable with both trust and client keystores.
            return SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE) // replace this once certs kick in
                    .build();
        } catch (SSLException e) {
            log.error("Cannot create ssl context", e);
            throw new IllegalStateException("Cannot create ssl context");
        }
    }

    @Bean
    WebClientCustomizer webClientCustomizer() {
       return builder -> builder
               .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    }
}
