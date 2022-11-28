package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import rig.ruuter.model.ContentCachingRequestWrapper;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@Component
public class CsrfValidationService {

    @Value("${csrf.enabled:false}")
    private boolean csrfEnabled;

    @Value("${csrf.jwt_to_validate_against:JWTTOKEN}")
    private String csrfValidateAgainst;

    @Value("${csrf.jwt_to_validate_against_key_name:hash}")
    private String csrfValidateAgainstKeyName;

    @Value("${csrf.post_key_to_validate_against:hash}")
    private String postKeyToValidateAgainst;

    @Value("${csrf.tim_userinfo_url:}")
    private String timUserInfoUrl;

    @Value("#{'${csrf.request.method.whitelist:GET}'.split(',')}")
    private List<String> csrfRequestMethodWhitelist;

    @Value("#{'${csrf.ruuter_services_whitelist:}'.split(',')}")
    private List<String> csrfRuuterServicesWhitelist;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isValid(ServletRequest req, String requestBody) throws IOException {
        HttpServletRequest request;
        if (!(req instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper((HttpServletRequest) req);
        } else {
            request = (HttpServletRequest) req;
        }

        if (csrfRequestMethodWhitelist.size() > 0 && csrfRequestMethodWhitelist.contains(request.getMethod())) {
            log.debug("Request method {} for request {} whitelisted. Skipping filter ..",
                    request.getMethod(), request.getRequestURI());
            return true;
        }

        if (csrfEnabled && !requestIsCsrfWhitelisted(request)) {
            try {
                if (request.getCookies() == null) {
                    log.error("Authorization cookies not present in the request");
                    return false;
                }

                Cookie csrfValidateAgainstCookie = Arrays.stream(request.getCookies())
                        .filter(getFindByCookieNamePredicate(csrfValidateAgainst))
                        .findFirst()
                        .orElse(null);

                if (csrfValidateAgainstCookie == null) {
                    log.error("Cookie {} required by CSRF no found in request", csrfValidateAgainst);
                    return false;
                }

                String csrfHashFromRequestBody = getHashFromPostBody(requestBody, postKeyToValidateAgainst);

                if (csrfHashFromRequestBody == null) {
                    log.error("Hash not found from request body {}", postKeyToValidateAgainst);
                    return false;
                }

                String csrfHashFromCookie = getHashFromCookie(csrfValidateAgainstCookie);

                if (csrfHashFromCookie == null) {
                    log.error("Hash not found from cookie {}", csrfValidateAgainst);
                    return false;
                }

                if (!csrfHashFromCookie.equals(csrfHashFromRequestBody)) {
                    log.error("CSRF hash mismatch. Denying request.");
                    return false;
                }
            } catch (Throwable t) {
                log.error("Failed validating csrf", t);
                return false;
            }
        }

        log.debug("Validated CSRF request {} {} according to the CSRF rules", request.getMethod(), request.getRequestURI());

        return true;
    }

    private boolean requestIsCsrfWhitelisted(HttpServletRequest request) {
        String[] requestUriArray = request.getRequestURI().split("/");
        boolean isWhitelisted =  requestUriArray.length > 0 &&
                csrfRuuterServicesWhitelist.contains(requestUriArray[requestUriArray.length - 1]);
        log.debug("Request {} csrf whitelisted: {}", request.getRequestURI(), isWhitelisted);
        return isWhitelisted;
    }

    private String getHashFromPostBody(String requestBody, String postKeyToValidateAgainst) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(requestBody);
        return jsonNode.has(postKeyToValidateAgainst) ?
                jsonNode.get(postKeyToValidateAgainst).asText() : null;
    }

    private String getHashFromCookie(Cookie cookie) {
        String result = null;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", cookie.getName().concat("=").concat(cookie.getValue()));
        HttpEntity requestEntity = new HttpEntity(null, requestHeaders);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(timUserInfoUrl, HttpMethod.GET, requestEntity, Map.class);
            Map<String, String> responseBody = response.getBody();
            result = responseBody.get(csrfValidateAgainstKeyName);
        } catch (Throwable t) {
            log.error("Failed retrieving csrf hash", t);
        }
        return result;
    }

    private Predicate<Cookie> getFindByCookieNamePredicate(String cookieName) {
        return cookie -> cookieName.equals(cookie.getName());
    }

}