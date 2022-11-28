package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import rig.ruuter.controller.ValidationController;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static rig.ruuter.TestUtils.getJsonNode;

class MatchInputTest {

    private ValidationController matchInputController;

    @BeforeEach
    void setUp() {
        this.matchInputController = new ValidationController(new ValidationService());
    }

    @Test
    void testMustMatchAll1() throws IOException {
        testMatchCase(200, "match_input/must_match_all_1_200.json");
    }

    @Test
    void testMustMatchAll2() throws IOException {
        testMatchCase(200, "match_input/must_match_all_2_200.json");
    }

    @Test
    void testMustMatchAll3() throws IOException {
        testMatchCase(200, "match_input/must_match_all_3_200.json");
    }

    @Test
    void testMustMatchAll4() throws IOException {
        testMatchCase(200, "match_input/must_match_all_4_200.json");
    }

    @Test
    void testMustMatchAll5() throws IOException {
        testMatchCase(401, "match_input/must_match_all_5_401.json");
    }

    @Test
    void testMustMatchAny1() throws IOException {
        testMatchCase(200, "match_input/must_match_any_1_200.json");
    }

    @Test
    void testMustMatchAny2() throws IOException {
        testMatchCase(200, "match_input/must_match_any_2_200.json");
    }

    @Test
    void testMustMatchAny3() throws IOException {
        testMatchCase(200, "match_input/must_match_any_3_200.json");
    }

    @Test
    void testMustMatchAny4() throws IOException {
        testMatchCase(401, "match_input/must_match_any_4_401.json");
    }

    @Test
    void testMustMatchAny5() throws IOException {
        testMatchCase(401, "match_input/must_match_any_5_401.json");
    }

    @Test
    void testMustMatchExcact1() throws IOException {
        testMatchCase(200, "match_input/must_match_exact_1_200.json");
    }

    @Test
    void testMustMatchExcact2() throws IOException {
        testMatchCase(200, "match_input/must_match_exact_2_200.json");
    }

    @Test
    void testMustMatchExcact3() throws IOException {
        testMatchCase(401, "match_input/must_match_exact_3_401.json");
    }

    @Test
    void testMustMatchExcact4() throws IOException {
        testMatchCase(401, "match_input/must_match_exact_4_401.json");
    }

    private void testMatchCase(int httpStatusCode, String matchCasePath) throws IOException {
        JsonNode input = getJsonNode(matchCasePath);
        ResponseEntity response = matchInputController.matchInput(input).block();
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        assertEquals(httpStatusCode, response.getStatusCode().value());
    }

}
