package rig.ruuter.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rig.commons.handlers.DynamicContent;
import rig.commons.handlers.MDCwrapper;
import rig.ruuter.logging.PayloadLogger;

import javax.servlet.http.Cookie;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;

import static rig.ruuter.constant.Constant.*;
import static rig.ruuter.service.BaseService.getResponseBody;

@Slf4j
// TODO: this class needs to be converted to a spring singleton bean instead of this static mess.
public class RestUtils {
    private static final DynamicContent mdc = new MDCwrapper();
    private static final PayloadLogger payloadLogger = new PayloadLogger();

    private static AutowireCapableBeanFactory beanFactory;

    public RestUtils(AutowireCapableBeanFactory beanFactory) {
        RestUtils.beanFactory = beanFactory;
    }

    /*
     * ResponseEntity methods
     */
    public static Mono<ResponseEntity> response(HttpStatus status) {
        return response(null, status);
    }

    public static Mono<ResponseEntity> response(Object obj) {
        return response(obj, HttpStatus.OK);
    }

    public static Mono<ResponseEntity> response(boolean valid) {
        return response(valid, valid ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
    }

    public static Mono<ResponseEntity> response(Object obj,
                                                HttpStatus statusCode) {
        HttpHeaders httpHeaders = noCacheHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return Mono.just(new ResponseEntity<>(obj, httpHeaders, statusCode));
    }

    /*
     * REST request methods
     */
    public static CustomClientResponse httpGet(JsonNode endpointConf, String uri, List<Cookie> cookies, HttpHeaders headers) {
        log.info("GET request to {}", uri);
        WebClient.RequestHeadersUriSpec<?> request = getClient(endpointConf).get();
        setCookies(request, cookies);
        ClientResponse response;
        try {
            URI uri2;
            try {
                uri2 = new URI(uri);
            } catch (java.net.URISyntaxException e) {
                log.info("Unable to create URI", e);
                return null;
            }

            WebClient.RequestHeadersSpec<?> requestHeadersSpec = request.uri(uri2)
                .header(REQUEST_UID, mdc.get(REQ_GUID));
            if (endpointConf.has(ALLOW_INCOMING_HEADERS_FORWARDING) &&
                endpointConf.get(ALLOW_INCOMING_HEADERS_FORWARDING).isBoolean() &&
                endpointConf.get(ALLOW_INCOMING_HEADERS_FORWARDING).booleanValue()) {
                includeIncomingHeaders(requestHeadersSpec);
            }

            headers.forEach((name, values) -> requestHeadersSpec.header(name, values.toArray(new String[values.size()])));

            response = requestHeadersSpec.header("Pragma", "no-cache")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("Expires", "-1")
                .exchange()
                .block();
        } catch (ReadTimeoutException rte) {
            log.warn("GET request for uri {} timed out.", uri);
            return new CustomClientResponse(HttpStatus.REQUEST_TIMEOUT, null);
        }

        if (response == null) {
            log.info("GET request for uri {}. Response is null", uri);
            return CustomClientResponse.getInstance();
        }

        String responseBody = getResponseBody(response);
        log.info("HTTP GET for uri {} response code {}", uri, response.statusCode());
        payloadLogger.logResponse("Response body: {}", responseBody);

        return new CustomClientResponse(response.statusCode(), responseBody, response.headers().asHttpHeaders());
    }

    public static CustomClientResponse httpRequestWithBody(String method, JsonNode endpointConf, String uri,
                                                           String body, List<Cookie> cookies, HttpHeaders headers) {
        payloadLogger.logRequest("Request body: {}", body);
        String methodUc = method.toUpperCase();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = getClient(endpointConf).method(HttpMethod.resolve(methodUc));
        setCookies(requestBodyUriSpec, cookies);
        ClientResponse response;
        try {
            URI uri2;
            try {
                uri2 = new URI(uri);
            } catch (java.net.URISyntaxException e) {
                log.info("Unable to create URI", e);
                return null;
            }
            WebClient.RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uri2)
                .header(REQUEST_UID, mdc.get(REQ_GUID));
            if (endpointConf.has(ALLOW_INCOMING_HEADERS_FORWARDING) &&
                endpointConf.get(ALLOW_INCOMING_HEADERS_FORWARDING).isBoolean() &&
                endpointConf.get(ALLOW_INCOMING_HEADERS_FORWARDING).booleanValue()) {
                includeIncomingHeaders(requestBodySpec);
            }

            headers.forEach((name, values) -> requestBodySpec.header(name, values.toArray(new String[values.size()])));

            response = requestBodySpec
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Pragma", "no-cache")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("Expires", "-1")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .exchange()
                .block();
        } catch (ReadTimeoutException rte) {
            log.warn("{} request for uri {} timed out.", methodUc, uri);
            return new CustomClientResponse(HttpStatus.REQUEST_TIMEOUT, null);
        }
        if (response == null) {
            return new CustomClientResponse(HttpStatus.BAD_REQUEST, null);
        }
        String responseBody = getResponseBody(response);
        log.info("HTTP {} for uri {} response code {}", methodUc, uri, response.statusCode());
        payloadLogger.logResponse("Response body: {}", responseBody);
        return new CustomClientResponse(response.statusCode(), responseBody, response.headers().asHttpHeaders());
    }

    private static void includeIncomingHeaders(WebClient.RequestHeadersSpec requestBodySpec) {
        ServletRequestAttributes servletRequestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        if (servletRequestAttributes != null) {
            Enumeration<String> headerNames = servletRequestAttributes.getRequest().getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = servletRequestAttributes.getRequest().getHeader(key);
                requestBodySpec.header(key, value);
            }
        }
    }

    static WebClient getClient(JsonNode destinationNode) {
        return beanFactory.getBean(WebClient.class, beanFactory.getBean(WebClient.Builder.class), destinationNode);
    }

    public static <T> void setCookies(T request, List<Cookie> cookies) {
        if (request instanceof WebClient.RequestBodyUriSpec) {
            cookies.forEach(c -> {
                ((WebClient.RequestBodyUriSpec) request).cookie(c.getName(), c.getValue());
                log.debug("Added cookie with name '{}' and value '{}'", c.getName(), c.getValue());
            });
        } else if (request instanceof WebClient.RequestHeadersUriSpec<?>) {
            cookies.forEach(c -> {
                ((WebClient.RequestHeadersUriSpec<?>) request).cookie(c.getName(), c.getValue());
                log.debug("Added cookie with name '{}' and value '{}'", c.getName(), c.getValue());
            });
        }
    }

    private static AsynchronousFileChannel createAsyncFile(Path path) {
        try {
            return AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
        } catch (Exception e) {
            log.error("Couldn't create Asynch file channel", e);
            return null;
        }
    }

    public static HttpHeaders noCacheHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setPragma("no-cache");
        httpHeaders.setCacheControl(CacheControl.noCache());
        httpHeaders.setExpires(-1);
        return httpHeaders;
    }

    public static Mono<ResponseEntity> sanitizeResponse(Mono<ResponseEntity> monoEntity) {
        return monoEntity.flatMap(responseEntity -> {
            if (responseEntity.getStatusCodeValue() > 199 && responseEntity.getStatusCodeValue() < 400) {
                return Mono.just(responseEntity);
            }
            return Mono.just(new ResponseEntity(HttpStatus.OK));
        });
    }
}
