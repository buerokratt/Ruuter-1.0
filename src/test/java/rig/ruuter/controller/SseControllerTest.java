package rig.ruuter.controller;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import rig.ruuter.BaseIntegrationTest;
import rig.ruuter.RuuterApplication;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SpringBootTest(webEnvironment = DEFINED_PORT, classes = RuuterApplication.class)
class SseControllerTest extends BaseIntegrationTest {

    @Test
    void callLibrary_shouldCallSseMethod() throws JSONException {
        Flux<String> responseStream = client.get()
            .uri("http://localhost:65432/sse/get-uuid")
            .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(String.class)
            .getResponseBody();
        List<String> responses = new ArrayList<>();
        StepVerifier.create(responseStream)
            .consumeNextWith(responses::add)
            .consumeNextWith(responses::add)
            .thenCancel()
            .verify();
        String uuid1 = new JSONObject(responses.get(0)).getJSONObject("body").getJSONObject("data").getJSONObject("get_uuid").getString("output");
        String uuid2 = new JSONObject(responses.get(1)).getJSONObject("body").getJSONObject("data").getJSONObject("get_uuid").getString("output");
        assertNotEquals(uuid1, uuid2);
    }
}
