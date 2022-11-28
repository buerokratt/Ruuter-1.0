package rig.ruuter.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import rig.ruuter.TestBase;
import rig.ruuter.TestBase.BaseContext;
import rig.ruuter.configuration.WebClientConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rig.ruuter.util.RestUtils.getClient;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
    "ruuter.reactive.client.load-balancer.enabled=true"
})
@ContextConfiguration(classes = {WebClientConfiguration.class, BaseContext.class})
class RestUtilsLoadBalancerTest extends TestBase {
    @MockBean
    private ExchangeFilterFunction exchangeFilterFunction;

    @Test
    void getWebClientLbTest() {
        WebClient client = getClient(toJson("{}"));
        assertNotNull(client);
        client.mutate().filters(filters -> {
            assertTrue(filters.contains(exchangeFilterFunction));
        });
    }
}
