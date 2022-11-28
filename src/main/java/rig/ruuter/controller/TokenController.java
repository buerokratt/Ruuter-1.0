package rig.ruuter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.service.TokenService;

import static rig.ruuter.util.RestUtils.response;

/**
 * API endpoint(s) for generating tokens and hashes
 *
 * @see TokenService
 */
@Slf4j
@RestController
@Timed
public class TokenController {

    private final TokenService tokenService;

    @Autowired
    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping(value = "/generate_token/{length}")
    public Mono<ResponseEntity> generateToken(@PathVariable("length") Integer length) {
        if (length == null || length < 1) {
            String errorMessage = "Invalid request. Length value is " + length;
            log.warn(errorMessage);
            return response(errorMessage, HttpStatus.BAD_REQUEST);
        }
        return response(tokenService.generateToken(length));
    }

    @GetMapping(value = "/generate_hash/{input}")
    public Mono<ResponseEntity> generateHash(@PathVariable("input") String input) {
        if (StringUtils.isEmpty(input)) {
            String errorMessage = "Invalid request. Empty input value";
            log.warn(errorMessage);
            return response(errorMessage, HttpStatus.BAD_REQUEST);
        }
        return response(tokenService.generateHash(input));
    }

    @GetMapping(value = "/generate_uuid")
    public Mono<ResponseEntity> generateUuid() {
        return response(tokenService.generateUuid());
    }
}
