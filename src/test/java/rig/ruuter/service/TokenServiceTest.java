package rig.ruuter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import rig.ruuter.controller.TokenController;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class TokenServiceTest {

    private TokenController tokenController;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.tokenController = new TokenController(new TokenService());
    }

    @Test
    void generateToken() {
        ResponseEntity response = tokenController.generateToken(50).block();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(50, Objects.requireNonNull(response.getBody()).toString().length());
    }

    @Test
    void generateToken_lengthIsNull() {
        ResponseEntity response = tokenController.generateToken(null).block();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void generateToken_lengthIsZero() {
        ResponseEntity response = tokenController.generateToken(0).block();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void generateHash() {
        ResponseEntity response = tokenController.generateHash("input").block();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("yWxtW+jQihLntc3Bsgf6ayQwl0yGgD2IkWdedv2ZLCA=", response.getBody().toString());
    }

    @Test
    void generateHash_inputIsNull() {
        ResponseEntity response = tokenController.generateHash(null).block();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void generateHash_inputIsEmpty() {
        ResponseEntity response = tokenController.generateHash("").block();
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void generateUuid() {
        ResponseEntity response = tokenController.generateUuid().block();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(36, Objects.requireNonNull(response.getBody()).toString().length());
    }


}
