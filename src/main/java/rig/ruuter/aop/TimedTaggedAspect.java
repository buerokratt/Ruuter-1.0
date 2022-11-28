package rig.ruuter.aop;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.apache.catalina.connector.RequestFacade;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import rig.ruuter.configuration.routing.RoutingConfiguration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Component
public class TimedTaggedAspect {
    public static final String DEFAULT_METRIC_NAME = "method.timed";

    private final MeterRegistry registry;
    private final Function<ProceedingJoinPoint, Iterable<io.micrometer.core.instrument.Tag>> tagsBasedOnJoinPoint;

    public TimedTaggedAspect() {
        this(Metrics.globalRegistry);
    }

    public TimedTaggedAspect(MeterRegistry registry) {
        this(registry, pjp ->
                Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
                        "method", pjp.getStaticPart().getSignature().getName())
        );
    }

    public TimedTaggedAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<io.micrometer.core.instrument.Tag>> tagsBasedOnJoinPoint) {
        this.registry = registry;
        this.tagsBasedOnJoinPoint = tagsBasedOnJoinPoint;
    }

    @Around("execution (@rig.ruuter.aop.Timed * *.*(..))")
    public Object timedMethod(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Timed timed = method.getAnnotation(Timed.class);
        if (timed == null) {
            method = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
            timed = method.getAnnotation(Timed.class);
        }

        final String metricName = timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();
        return processWithTimer(pjp, timed, metricName);
    }

    private Object processWithTimer(ProceedingJoinPoint pjp, Timed timed, String metricName) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        int responseCode = 0;
        try {
            Object result = pjp.proceed();
            responseCode = getResponseCode(result);
            return result;
        } catch (Exception ex) {
            responseCode = getExceptionalResponseCode(ex);
            throw ex;
        } finally {
            record(pjp, timed, metricName, sample, responseCode);
        }
    }

    private int getResponseCode(Object proceedResult) {
        AtomicInteger responseCode = new AtomicInteger(0);
        Mono<ResponseEntity> monoResult = (Mono<ResponseEntity>) proceedResult;
        monoResult.subscribe(responseEntity -> setResponseCode(responseEntity, responseCode));
        return responseCode.get();
    }

    private int getExceptionalResponseCode(Exception ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("io.netty.channel.ConnectTimeoutException")) {
            return 408;
        }
        return 500;
    }

    private void setResponseCode(ResponseEntity responseEntity, AtomicInteger responseCode) {
        responseCode.set(responseEntity.getStatusCodeValue());
    }


    private void record(ProceedingJoinPoint pjp, Timed timed, String metricName, Timer.Sample sample, int responseCode) {
        try {
            sample.stop(Timer.builder(metricName)
                    .description(timed.description().isEmpty() ? null : timed.description())
                    .tags("route", getTagValue(pjp))
                    .tags("has_config", String.valueOf(hasRouterConfig(pjp)))
                    .tags("response_code", String.valueOf(responseCode))
                    .tags("client_ip", getRemoteAddr(pjp))
                    .publishPercentileHistogram(timed.histogram())
                    .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles())
                    .register(registry));
        } catch (Exception e) {
            // ignoring on purpose
        }
    }

    private boolean hasRouterConfig(ProceedingJoinPoint pjp) {
        Optional<Object> routingConfigOptional = Arrays.stream(pjp.getArgs())
                .filter(a -> a instanceof RoutingConfiguration)
                .findFirst();

        if (routingConfigOptional.isPresent()) {
            RoutingConfiguration routingConfig = (RoutingConfiguration) routingConfigOptional.get();
            String route = getTagValue(pjp);
            JsonNode configNode = routingConfig.find(route);
            if (configNode == null) {
                return false;
            }
        }
        return true;
    }

    private String getRemoteAddr(ProceedingJoinPoint pjp) {
        Optional<Object> requestFacadeOptional = Arrays.stream(pjp.getArgs())
                .filter(a -> a instanceof RequestFacade)
                .findFirst();

        if (requestFacadeOptional.isPresent()) {
            RequestFacade requestFacade = (RequestFacade) requestFacadeOptional.get();
            return requestFacade.getRemoteAddr();
        }
        return "";
    }

    private String getTagValue(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String tagValue = null;

        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter param = method.getParameters()[i];

            for (Annotation a : param.getDeclaredAnnotations()) {
                if (a instanceof Tag) {
                    tagValue = Arrays.stream(pjp.getArgs()).collect(Collectors.toList()).get(i).toString();
                }
            }
        }
        return tagValue;
    }
}

