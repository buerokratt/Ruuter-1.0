package rig.ruuter.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import rig.ruuter.TestBase;
import rig.ruuter.TestBase.BaseContext;
import rig.ruuter.configuration.WebClientConfiguration;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static rig.ruuter.util.RestUtils.getClient;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WebClientConfiguration.class, BaseContext.class})
@TestPropertySource(properties = {
    "ruuter.reactive.client.default.connection.timeout=10000"
})
public class RestUtilsTest extends TestBase {
    @Test
    public void responseTest() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        Mono<ResponseEntity> monoResponse = Mono.just(new ResponseEntity<>(null, httpHeaders, HttpStatus.OK));
        assertEquals(Objects.requireNonNull(monoResponse.block()).getStatusCode(), Objects.requireNonNull(response(HttpStatus.OK).block()).getStatusCode());
    }

    @Test
    public void getWebClientTest() {
        assertNotNull(getClient(toJson("{}")));
    }

    @Test
    public void getWebTestClientWithTimeout() {
        assertNotNull(getClient(destinationNode));
    }

}
