package rig.ruuter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import rig.ruuter.BaseIntegrationTest;

import java.util.HashMap;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

class HelperFunctionControllerTest extends BaseIntegrationTest {

    @Test
    void callLibrary_shouldCallCommonsService_withListInput() throws Exception {
        String input = "midagi";
        String output = "bWlkYWdp";

        client.post()
            .uri("functions/encodeBase64")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("encodeBase64")
            .jsonPath("$.input").isEqualTo(input)
            .jsonPath("$.output").isEqualTo(output);
    }

    @Test
    void callLibrary_shouldCallCommonsService_withMapInput() throws Exception {
        client.post()
            .uri("functions/encodeBase64")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper().writeValueAsBytes(new HashMap<>() {{
                put("test", "midagi");
            }}))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("encodeBase64")
            .jsonPath("$.output").isEqualTo("bWlkYWdp");
    }
}
