package rig.ruuter.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import rig.ruuter.model.ContentCachingRequestWrapper;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This request interceptor class, if enabled, will forward the incoming request body and header to external validation.
 * If external service responds with status code 200, the request will pass.
 */
@Slf4j
@Component
public class ExternalValidationService {

    public static final String REQUEST_URL_HEADER_NAMESPACE = "Turvis-Request-For";

    @Setter
    @Getter
    @Value("${incoming.request.external.validation.enabled:false}")
    private boolean incomingRequestExternalValidationEnabled;

    @Setter
    @Getter
    @Value("${incoming.request.external.validation.endpoint:}")
    private String incomingRequestExternalValidationEndpoint;

    private final RestTemplate restTemplate;


    public ExternalValidationService() {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        this.restTemplate = restTemplate;
    }

    public boolean isValid(ServletRequest request, String requestBody) throws IOException {
        HttpServletRequest req;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            req = new ContentCachingRequestWrapper((HttpServletRequest) request);
        } else {
            req = (HttpServletRequest) request;
        }

        if (incomingRequestExternalValidationEnabled && incomingRequestExternalValidationEndpoint != null
                && !incomingRequestExternalValidationEndpoint.trim().isEmpty()) {

            HttpHeaders headers = Collections
                    .list(req.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            h -> Collections.list(req.getHeaders(h)),
                            (oldValue, newValue) -> newValue,
                            HttpHeaders::new
                    ));
            headers.add(REQUEST_URL_HEADER_NAMESPACE, getCfgPath(getFullUrl(req)));

            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                log.info("Sending req {} to external validation to url {}", req.getRequestURI(),
                        incomingRequestExternalValidationEndpoint);
                ResponseEntity<String> response =
                        restTemplate.exchange(incomingRequestExternalValidationEndpoint, HttpMethod.POST, requestEntity, String.class);
                log.info("External validation result {}", response.getStatusCodeValue());
                if (!response.getStatusCode().equals(HttpStatus.OK)) {
                    return false;
                }
            } catch (Throwable t) {
                log.error("Error while performing external validation", t);
                return false;
            }
        }
        return true;
    }


    private String getFullUrl(HttpServletRequest req) {
        int serverPort = req.getServerPort();
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        String servletPath = req.getServletPath();
        String contextPath = req.getContextPath();
        String queryString = req.getQueryString();
        String pathInfo = req.getPathInfo();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    private String getCfgPath(String url) {
        String[] urlArray = url.split("/");
        return urlArray.length > 0 ? urlArray[urlArray.length-1] : "";
    }

}
