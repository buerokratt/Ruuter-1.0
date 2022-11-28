package rig.ruuter.filter;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import rig.ruuter.service.ExternalValidationService;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rig.ruuter.service.ExternalValidationService.REQUEST_URL_HEADER_NAMESPACE;
import static rig.ruuter.util.StrUtils.toJson;

@WireMockTest(httpPort = 23456)
class ExternalValidatorInterceptorTest {

    private ExternalValidationService externalValidatorInterceptor;

    private final String uri = "/turvis";

    private final String endpoint = "http://localhost:23456".concat(uri);

    @BeforeEach
    void setUp() {
        this.externalValidatorInterceptor = new ExternalValidationService();
        this.externalValidatorInterceptor.setIncomingRequestExternalValidationEnabled(true);
        this.externalValidatorInterceptor.setIncomingRequestExternalValidationEndpoint(endpoint);
    }

    boolean testPreHandle(Integer statusCode) throws Exception {
        String requestBody = "{\"data\":\"foobar\"}";
        Cookie cookie = new Cookie("token", "value");
        String serverName = "www.lorem.ipsum/test";

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setServerName(serverName);
        httpServletRequest.setRequestURI("/foo");
        httpServletRequest.setContent(toJson(requestBody)
            .toString().getBytes(StandardCharsets.UTF_8));
        httpServletRequest.setCookies(cookie);

        stubFor(post(urlPathEqualTo(uri))
            .withHeader(REQUEST_URL_HEADER_NAMESPACE, equalTo("test"))
            .withRequestBody(equalTo(requestBody))
            .withCookie(cookie.getName(), equalTo(cookie.getValue()))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")));

        return this.externalValidatorInterceptor.isValid(httpServletRequest, "{\"data\":\"foobar\"}");
    }

    @Test
    void testPreHandleSuccess() throws Exception {
        assertTrue(testPreHandle(200));
    }

    @Test
    void testPreHandleFail() throws Exception {
        assertFalse(testPreHandle(400));
    }

}
