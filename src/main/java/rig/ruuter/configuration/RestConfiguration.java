package rig.ruuter.configuration;

import brave.Tracer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import rig.commons.handlers.GenericHeaderLogHandler;
import rig.commons.handlers.LogHandler;
import rig.commons.handlers.MDCwrapper;
import rig.ruuter.configuration.routing.RoutingConfiguration;
import rig.ruuter.interceptor.AddCSPHeaderInterceptor;
import rig.ruuter.interceptor.AddGuidHeaderInterceptor;
import rig.ruuter.interceptor.RequestMethodValidatorInterceptor;
import rig.ruuter.interceptor.TraceIdInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static rig.ruuter.constant.Constant.REQ_GUID;

@Configuration
@RequiredArgsConstructor
public class RestConfiguration implements WebMvcConfigurer {

    private LogHandler handler = LogHandler.builder().build();

    //"REQ_GUID" as key name in MDC is set by LogHandler
    private AddGuidHeaderInterceptor headerInterceptor = new AddGuidHeaderInterceptor(REQ_GUID, new MDCwrapper());

    private final AddCSPHeaderInterceptor cspHeaderInterceptor;

    private final RoutingConfiguration routingConfiguration;

    private final Tracer tracer;

    @Value("${userIPLoggingPrefix:from ip}")
    private String loggingPrefix = "from ip";

    @Value("${userIPHeaderName:x-forwarded-for}")
    private String headerName = "x-forwarded-for";

    @Value("${userIPLoggingMDCkey:userIP}")
    private String key = "userIP";

    @Value("${verify.requested.method.type:false}")
    private boolean verifyRequestedMethodType;

    @Value("#{'${allowed.requested.method.types:GET,POST}'.split(',')}")
    private List<String> allowedRequestedMethodTypes;

    @Value("${default.requested.method.type:POST}")
    private String defaultRequestedMethodType;

    @Value("${requested.method.type.error.http.response.code:200}")
    private Integer requestedMethodTypeErrorHttpResponseCode;

    @Value("${ruuter.cors.allowedOrigins:}")
    private String allowedOrigins;
    @Value("${ruuter.cors.allowedHeaders:}")
    private String allowedHeaders;
    @Value("${ruuter.cors.exposeHeaders:}")
    private String exposeHeaders;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration corsRegistration = registry.addMapping("/**")
            .allowCredentials(true)
            .allowedMethods("GET", "POST")
            .maxAge(3600)
            .allowedHeaders(getAllowedHeaders())
            .exposedHeaders(getExposedHeaders());
        if (!hasText(allowedOrigins) || CorsConfiguration.ALL.equals(allowedOrigins)) {
            corsRegistration.allowedOriginPatterns(CorsConfiguration.ALL);
        } else {
            corsRegistration.allowedOrigins(allowedOrigins.split("[, ]+"));
        }
    }

    private String[] getAllowedHeaders() {
        return corsValueList(allowedHeaders, "Content-Type", "CID");
    }

    private String[] getExposedHeaders() {
        return corsValueList(exposeHeaders, "X-B3-TraceId");
    }

    private String[] corsValueList(String configValue, String... alwaysIncluded) {
        List<String> result = new ArrayList<>();
        if (hasText(configValue)) {
            result.addAll(Arrays.asList(configValue.split("[, ]+")));
        }
        result.addAll(Arrays.asList(alwaysIncluded));
        return result.toArray(new String[0]);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        GenericHeaderLogHandler ipHeaderHandler = GenericHeaderLogHandler.builder().key(key).messagePrefix(loggingPrefix + " ").headerName(headerName).build();
        if (verifyRequestedMethodType)
            registry.addInterceptor(requestMethodValidatorInterceptor());
        registry.addInterceptor(ipHeaderHandler);
        registry.addInterceptor(handler);
        registry.addInterceptor(traceIdInterceptor());
        registry.addInterceptor(cspHeaderInterceptor);
        registry.addInterceptor(headerInterceptor);
    }

    @Bean
    public UnsafeUnicodeTranslator unsafeUnicodeTranslator(@Value("${ruuter.allowedCharacters}") String allowedCharacters) {
        return new UnsafeUnicodeTranslator(allowedCharacters);
    }

    @Bean
    public SimpleModule unsafeUnicodeModule(UnsafeUnicodeTranslator unsafeUnicodeTranslator) {
        return new SimpleModule().addDeserializer(String.class, new StringDeserializer() {
            @Override
            public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                return unsafeUnicodeTranslator.translate(super.deserialize(jsonParser, deserializationContext));
            }
        });
    }

    @Bean
    RequestMethodValidatorInterceptor requestMethodValidatorInterceptor() {
        return new RequestMethodValidatorInterceptor(routingConfiguration,
            allowedRequestedMethodTypes,
            defaultRequestedMethodType,
            requestedMethodTypeErrorHttpResponseCode);
    }

    @Bean
    TraceIdInterceptor traceIdInterceptor() {
        return new TraceIdInterceptor(tracer);
    }

}
