package rig.ruuter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import rig.ruuter.util.HelperFunctionUtils;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.RestUtils.sanitizeResponse;

@Slf4j
@RestController
public class HelperFunctionController {

    /**
     * Call a function from external commons library
     *
     * @param functionName Function to call from commons
     * @param body         Arguments for the function as JSON
     * @return Returns the function name, input arguments and output result of the function call as JSON
     */
    @PostMapping(value = "/functions/{functionName}", produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity> callLibrary(@PathVariable String functionName,
                                            @RequestBody(required = false) Object body) {
        Object response = HelperFunctionUtils.callFunction(functionName, body);

        return sanitizeResponse(response(Map.of(
            "function", functionName,
            "input", body == null ? "" : body,
            "output", response == null ? "" : response)));
    }
}
