package rig.ruuter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rig.ruuter.configuration.UnsafeUnicodeTranslator;
import rig.ruuter.configuration.routing.MockConfiguration;
import rig.ruuter.configuration.routing.RoutingConfiguration;
import rig.ruuter.service.FileHandlingService;
import rig.ruuter.service.RouterService;
import rig.ruuter.validation.CsrfCheck;
import rig.ruuter.validation.ExternalValidation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.RestUtils.sanitizeResponse;
import static rig.ruuter.util.StrUtils.toJsonString;

/**
 * Router API endpoints
 */
@Slf4j
@RestController
@rig.commons.aop.Timed
@RequiredArgsConstructor
public class RuuterController {

    private final RouterService routerService;
    private final FileHandlingService fileService;
    private final RoutingConfiguration routingConfiguration;
    private final MockConfiguration mockConfiguration;
    private final UnsafeUnicodeTranslator unsafeUnicodeTranslator;

    @Value("${ruuter.version}")
    private String ruuterVersion;

    @Value("${polling.delay}")
    private Long pollingDelay = 2L;

    /**
     * @param request             automatically injected by Spring MVC
     * @param httpServletResponse automatically injected by Spring MVC
     * @param code                specific router configuration code
     * @param body                optional request body
     * @param requestParams       optional request parameters
     * @return http response exact nature of which depends on the request configuration code and parameters,
     * the header REQUEST_UID of the response will contain unique timestamp based ID (Java long type number)
     * for details how this timestamp is generated and set
     * @see rig.ruuter.configuration.RestConfiguration
     * and
     * @see rig.ruuter.interceptor.AddGuidHeaderInterceptor
     */
    @CsrfCheck(requestBodyIdx = 3)
    @ExternalValidation(requestBodyIdx = 3)
    @RequestMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> getRoute(HttpServletRequest request,
                                         HttpServletResponse httpServletResponse,
                                         @PathVariable("code") String code,
                                         @RequestBody(required = false) String body,
                                         @RequestParam(required = false) Map<String, String> requestParams) {
        return sanitizeResponse(routerService.getResponseEntityMono(routingConfiguration, request, httpServletResponse, code, unsafeUnicodeTranslator.translate(body), requestParams));
    }

    @CsrfCheck(requestBodyIdx = 3)
    @ExternalValidation(requestBodyIdx = 3)
    @RequestMapping(value = "/sse/{code}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity> getSSERoute(HttpServletRequest request,
                                                             HttpServletResponse httpServletResponse,
                                                             @PathVariable("code") String code,
                                                             @RequestBody(required = false) String body,
                                                             @RequestParam(required = false) Map<String, String> requestParams) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(pollingDelay))
            .publishOn(Schedulers.boundedElastic())
            .map(tick -> requireNonNull(sanitizeResponse(
                routerService.getResponseEntityMono(routingConfiguration, request, httpServletResponse, code, unsafeUnicodeTranslator.translate(toJsonString(requestParams)), requestParams)))
                .block());
    }

    /**
     * @return always http response with status OK
     */
    @PostMapping(value = "/tmp/delete")
    public Mono<ResponseEntity> deleteTmpFiles() {
        return sanitizeResponse(fileService.deleteTmpFiles());
    }

    /**
     * @param hash     hash for inbox file attachment
     * @param filename filename for attachment
     * @return on success returns a response containing selected file (as Application/Octet stream content),
     * filename for returned attachment will be set as provided in parameter filename
     */
    @GetMapping(value = "/inbox/attachments/{hash}/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity> getInboxAttachmentByHashWithFilename(@PathVariable("hash") String hash,
                                                                     @PathVariable("filename") String filename) {
        log.info("Asking for file with hash {} and name {}", hash, filename);
        return sanitizeResponse(fileService.getInboxAttachment(hash, filename));
    }

    /**
     * @param hash hash for inbox file attachment
     * @return on success returns a response containing selected file (as Application/Octet stream content),
     * filename for returned attachment will be set the same as hash parameter string
     */
    @GetMapping(value = "/inbox/attachments/{hash}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity> getInboxAttachmentByHash(@PathVariable("hash") String hash) {
        log.info("Asking for file with hash {}", hash);
        return sanitizeResponse(fileService.getInboxAttachment(hash, null));
    }

    /**
     * @param code specific router configuration code, for this method specific mock configurations are used
     * @return response depends on the specific input
     */
    @CsrfCheck(requestBodyIdx = 3)
    @ExternalValidation(requestBodyIdx = 3)
    @RequestMapping(value = "/mock/{code}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> mock(HttpServletRequest request,
                                     HttpServletResponse httpServletResponse,
                                     @PathVariable("code") String code,
                                     @RequestBody(required = false) String body,
                                     @RequestParam(required = false) Map<String, String> requestParams) {
        return sanitizeResponse(routerService.getResponseEntityMono(mockConfiguration, request, httpServletResponse, code, unsafeUnicodeTranslator.translate(body), requestParams));
    }

    /**
     * @return returns json formatted response containing router version
     */
    @RequestMapping(value = "/rest/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> getRestTest() {
        return sanitizeResponse(getTest());
    }

    /**
     * @return returns json formatted response containing router version
     */
    @RequestMapping(value = "/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<ResponseEntity> getTest() {
        return sanitizeResponse(response("{\"ruuter_version" + "\": \"" + ruuterVersion + "\"}"));
    }
}
