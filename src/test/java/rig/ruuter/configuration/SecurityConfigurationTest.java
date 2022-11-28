package rig.ruuter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import rig.ruuter.BaseIntegrationTest;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@TestPropertySource(properties = {"ip-whitelist.routes[0].patterns[0]=/functions/encodeBase64", "ip-whitelist.routes[0].ips[0]=127.0.0.2"})
class SecurityConfigurationTest extends BaseIntegrationTest {

    @Test
    void shouldThrowException_whenIpNotInAllowlist() throws Exception {
        String input = "midagi";

        client.post()
            .uri("functions/encodeBase64")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .isEmpty();
    }
}
