package rig.ruuter.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import javax.annotation.Priority;

@RequiredArgsConstructor
@Priority(Ordered.HIGHEST_PRECEDENCE)
class FixDiscoveryInstanceSchemeBeanPostProcessor implements BeanPostProcessor {
    private final SimpleReactiveDiscoveryProperties simpleDiscoveryProperties;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (!(bean instanceof SimpleReactiveDiscoveryClient)) return bean;

        // Temporary fix for LB issue: https://github.com/spring-cloud/spring-cloud-commons/issues/823
        // This postprocessor can be removed once the LB issue has been release and spring-cloud version upgraded to use the version containing the fix.
        return new SimpleReactiveDiscoveryClient(simpleDiscoveryProperties) {
            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                return simpleDiscoveryProperties.getInstances(serviceId)
                    .map(instance -> {
                        DefaultServiceInstance fixedInstance = new DefaultServiceInstance(instance.getInstanceId(), instance.getServiceId(), instance.getHost(), instance.getPort(), instance.isSecure()) {
                            // by default, the getScheme() method is not implemented and LB ends up using the gateway request scheme.
                            // So if gateway request was received as https, it always ended up sending https to the backend,
                            // but we don't always have https in the backend.
                            @Override
                            public String getScheme() {
                                return isSecure() ? "https" : "http";
                            }
                        };
                        fixedInstance.setServiceId(instance.getServiceId());
                        fixedInstance.setInstanceId(instance.getInstanceId());
                        return fixedInstance;
                    });
            }
        };
    }
}
