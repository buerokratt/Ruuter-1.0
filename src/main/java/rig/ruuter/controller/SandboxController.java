package rig.ruuter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.configuration.UnsafeUnicodeTranslator;
import rig.ruuter.util.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static rig.ruuter.constant.Constant.REQUEST_UID;
import static rig.ruuter.util.StrUtils.toJson;


/**
 * This controller will only contain endpoints that are used to demonstrate current router application functionality.
 * Some endpoints to route or forward to when doing mock request to router etc.
 */
@Slf4j
@RestController
@Timed
@RequiredArgsConstructor
public class SandboxController {
    private final UnsafeUnicodeTranslator unsafeUnicodeTranslator;

    @GetMapping(value = "/sandbox/ok", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> getOK(HttpServletRequest request,
                                      HttpServletResponse response,
                                      @RequestBody(required = false) String body,
                                      @RequestParam(required = false) Map<String, String> requestParams) {

        return RestUtils.response(responseBuilder("getOK", request, response), HttpStatus.OK);
    }

    @PostMapping(value = "/sandbox/ok")
    public Mono<ResponseEntity> getOKWithPostedData(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    @RequestBody(required = false) String body,
                                                    @RequestParam(required = false) Map<String, String> requestParams) {

        return RestUtils.response(responseBuilder("postOK", request, response, unsafeUnicodeTranslator.translate(body), true), HttpStatus.OK);
    }

    @RequestMapping(value = "/sandbox/nok", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> getNOK(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @RequestBody(required = false) String body,
                                       @RequestParam(required = false) Map<String, String> requestParams) {

        return RestUtils.response(responseBuilder("getNOK", request, response), HttpStatus.BAD_REQUEST);
    }

    private JsonNode responseBuilder(String endPoint,
                                     HttpServletRequest req,
                                     HttpServletResponse resp,
                                     String body,
                                     boolean showBody) {
        StringBuilder sb = new StringBuilder("{\"endpoint\":");
        sb.append("\"").append(endPoint).append("\"");
        sb.append(", \"uiIn\":");
        sb.append("\"").append(req.getHeader(REQUEST_UID)).append("\"");
        sb.append(", \"uiOut\":");
        sb.append("\"").append(resp.getHeader(REQUEST_UID)).append("\"");
        if (showBody) {
            sb.append(", \"postBody\":").append(body);
        }
        sb.append("}");

        return toJson(sb.toString());
    }

    private JsonNode responseBuilder(String endPoint,
                                     HttpServletRequest req,
                                     HttpServletResponse resp) {
        return responseBuilder(endPoint, req, resp, null, false);
    }

}
