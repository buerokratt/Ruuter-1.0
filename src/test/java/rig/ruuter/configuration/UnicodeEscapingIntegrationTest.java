package rig.ruuter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class UnicodeEscapingIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void shouldReplaceCharactersNotInWhitelistWithHexCodepoint() throws Exception {
        String escapedInput = "0x202E this text should not be reversed";

        client.post()
            .uri("functions/trim")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of("\u202E this text should not be reversed")))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("trim")
            .jsonPath("$.input").isEqualTo(escapedInput)
            .jsonPath("$.output").isEqualTo(escapedInput);
    }

    @Test
    void shouldNotReplaceBasicCharacters() throws Exception {
        String basicLatin = "! \"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        String basicLatinAllowedControls = "\n \r";
        String estonianCharacters = "ŠšŽžÕõÄäÖöÜü";
        String input = basicLatin + basicLatinAllowedControls + estonianCharacters;

        client.post()
            .uri("functions/trim")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("trim")
            .jsonPath("$.input").isEqualTo(input)
            .jsonPath("$.output").isEqualTo(input);
    }

    @Test
    void shouldHandleHighValueCodePoint() throws Exception {
        String input = "\uD803\uDFFD";
        String hex = "0x10FFD";

        client.post()
            .uri("functions/trim")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("trim")
            .jsonPath("$.input").isEqualTo(hex)
            .jsonPath("$.output").isEqualTo(hex);
    }

    @Test
    void shouldAllowCyrillic() throws Exception {
        String input = "АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя";

        client.post()
            .uri("functions/trim")
            .contentType(APPLICATION_JSON)
            .bodyValue(new ObjectMapper()
                .writeValueAsBytes(List.of(input)))
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.function").isEqualTo("trim")
            .jsonPath("$.input").isEqualTo(input)
            .jsonPath("$.output").isEqualTo(input);
    }
}
