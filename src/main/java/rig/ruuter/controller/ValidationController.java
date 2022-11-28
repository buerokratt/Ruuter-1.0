package rig.ruuter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.enums.MatchInputType;
import rig.ruuter.json.validation.EmptyList;
import rig.ruuter.json.validation.NumberParamLimits;
import rig.ruuter.json.validation.StringParamLimits;
import rig.ruuter.service.ValidationService;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static rig.ruuter.constant.MatchInputConstants.INPUT;
import static rig.ruuter.constant.MatchInputConstants.VALIDATE_AGAINST;
import static rig.ruuter.constant.MatchInputConstants.VALIDATION_TYPE;
import static rig.ruuter.util.RestUtils.response;

/**
 * API endpoint(s) for json validation service
 *
 * @see rig.ruuter.service.ValidationService
 */
@Slf4j
@RestController
@Timed
public class ValidationController {

    private final ValidationService validationService;

    @Autowired
    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * @param body Json node containing a Json node to be validated, validation type and optionally a Json node to validate against
     * @return http response with status 200 for validated input and 401 for failed validation (400 in case of malformed request)
     * @see rig.ruuter.constant.MatchInputConstants for expected attribute names
     */
    @RequestMapping(value = "/match_input", method = POST)
    public Mono<ResponseEntity> matchInput(@RequestBody JsonNode body) {
        log.debug("Incoming request body '{}'", body);

        boolean response = false;

        if (!body.has(INPUT) && !body.get(INPUT).isObject()) {
            log.error("Missing attribute {}", INPUT);
            return response("Missing attribute ".concat(INPUT), HttpStatus.BAD_REQUEST);
        }

        if (!body.has(VALIDATE_AGAINST) && !body.get(VALIDATE_AGAINST).isObject()) {
            log.error("Missing attribute {}", VALIDATE_AGAINST);
            return response("Missing attribute ".concat(VALIDATE_AGAINST), HttpStatus.BAD_REQUEST);
        }

        if (!body.has(VALIDATION_TYPE) && !body.get(VALIDATION_TYPE).isTextual()) {
            log.error("Missing attribute {}", VALIDATION_TYPE);
            return response("Missing attribute ".concat(VALIDATION_TYPE), HttpStatus.BAD_REQUEST);
        }

        if (!Enums.getIfPresent(MatchInputType.class, body.get(VALIDATION_TYPE).asText()).isPresent()) {
            log.error("Validation type {} not present", body.get(VALIDATION_TYPE).asText());
            return response("Validation type ".concat(body.get(VALIDATION_TYPE).asText())
                    .concat(" not present"), HttpStatus.BAD_REQUEST);
        }

        switch (MatchInputType.valueOf(body.get(VALIDATION_TYPE).asText().toUpperCase())) {
            case MUST_MATCH_ALL:
                response = validationService.mustMatchAll(body.get(INPUT), body.get(VALIDATE_AGAINST));
                break;
            case MUST_MATCH_ANY:
                response = validationService.mustMatchAny(body.get(INPUT), body.get(VALIDATE_AGAINST));
                break;
            case MUST_MATCH_EXACT:
                response = validationService.mustMatchExact(body.get(INPUT), body.get(VALIDATE_AGAINST));
                break;
        }

        log.debug("Final response {}", response);

        return response(response);
    }

    @RequestMapping(value = "/param_string_length", method = POST)
    public Mono<ResponseEntity> paramStringLength(@Valid @RequestBody StringParamLimits params) {
        if (params.getMax() == null && params.getMin() == null) {
            log.warn("Neither min nor max was specified {}", params);
            return response("Neither min nor max was specified", HttpStatus.BAD_REQUEST);
        }
        return response(validationService.validateLength(params) ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/param_number_value_limit", method = POST)
    public Mono<ResponseEntity> paramNumberValueLimit(@Valid @RequestBody NumberParamLimits params) {
        if (params.getMax() == null && params.getMin() == null) {
            log.warn("Neither min nor max was specified {}", params);
            return response("Neither min nor max was specified", HttpStatus.BAD_REQUEST);
        }
        return response(validationService.validateNumberValue(params) ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/empty_list", method = POST)
    public Mono<ResponseEntity> emptyListValidation(@Valid @RequestBody EmptyList params) {
        if (params.getList() == null) {
            log.warn("No list was specified {}", params);
            return response("No list was specified", HttpStatus.BAD_REQUEST);
        }
        return response(validationService.validateEmptyList(params) ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
