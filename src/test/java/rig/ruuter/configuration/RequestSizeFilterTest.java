package rig.ruuter.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "maxRequestSizeBytes=12")
class RequestSizeFilterTest {

    @Autowired
    private WebTestClient client;

    @Test
    void shouldAllowRequests_whenSmallerThanLimit() throws JsonProcessingException {
        String input = "12 bytes";
        client.post()
            .uri("functions/encodeBase64")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("encodeBase64")
            .jsonPath("$.input").isEqualTo(input);
    }

    @Test
    void shouldReturnNoContent_whenLargerThanLimit() throws JsonProcessingException {
        String input = "13 bytes!";
        client.post()
            .uri("functions/encodeBase64")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody().isEmpty();
    }
}
