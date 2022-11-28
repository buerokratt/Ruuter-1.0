package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import rig.ruuter.TestBase;
import rig.ruuter.TestBase.BaseContext;
import rig.ruuter.configuration.WebClientConfiguration;
import rig.ruuter.util.CustomClientResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static rig.ruuter.ServiceStubs.stubRequestWithResponse;
import static rig.ruuter.constant.Constant.DESTINATION;
import static rig.ruuter.constant.Constant.RUUTER_BASE_RESPONSE_STRUCTURE;
import static rig.ruuter.service.BaseService.filterCookies;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WebClientConfiguration.class, BaseContext.class})
@WireMockTest(httpPort = 23456)
class BaseServiceTest extends TestBase {

    private static final String CONFIGURATION_WITH_ENDPOINT_PARAMETER = "{\"action_type\":\"BLOGIC\",\"destination\":{\"frr\":{\"method\":\"get\",\"endpoint\":\"http://localhost:8080/someUrl?personIdCode={#.userinfo#.personalCode}\",\"endpoint_parameters\":{\"userinfo\":{\"method\":\"get\",\"endpoint\":\"http://localhost:23456/jwt/userinfo\",\"response\":{\"ok\":\"proceed\",\"nok\":\"stop\"}}},\"response\":{\"ok\":\"proceed\",\"nok\":\"stop\"}}}}";

    private static MeterRegistry registry = new SimpleMeterRegistry();
    private static BaseService baseService;
    private static final String incomingRequestBody = "{\"body\": {\"requestBodyParam\": \"bodyParamValue\"}}";
    private static final String bodyStruct = "{\"post_body_struct\":{\"nested\":{\"something\":{\"currentDate\":\"{$currentDate$}\",\"something2\":{\"holeincomingbody\":\"{$incoming_request_body}\"}}}}}";
    private static JsonNode handleResponseResult;
    private HttpServletResponse httpServletResponse;

    @BeforeAll
    static void setUp() {
        baseService = new BaseService(registry);
    }

    @BeforeEach
    void beforeEach() {
        handleResponseResult = toJson(RUUTER_BASE_RESPONSE_STRUCTURE);
        this.httpServletResponse = new HttpServletResponseWrapper(new MockHttpServletResponse());
    }

    @Test
    void getUriWithParametersTest() {

        // http://localhost:8080/someurl?query={$payload}&ilm={$requestBodyParam}
        JsonNode conf = toJson(CONFIGURATION).get("search").get(DESTINATION).get("XM");
        Map<String, String> map = new HashMap<>();
        map.put("payload", "payload_value");
        String uriWitParams = baseService.getUriWithParameters(httpServletResponse,
            toJson("{}"), "", conf, map, incomingRequestBody, new ArrayList<>());
        assertEquals("http://localhost:8080/someurl?query={$payload}&ilm={$requestBodyParam}", uriWitParams);
    }

    @Test
    void getUriWithParametersTest_withEndpointParameter() {
        stubRequestWithResponse("/jwt/userinfo", "{\"personalCode\":\"EE10101010005\"}");

        // http://localhost:8080/someUrl?personIdCode={#.userinfo#.personalCode}
        JsonNode conf = toJson(CONFIGURATION_WITH_ENDPOINT_PARAMETER).get(DESTINATION).get("frr");
        Map<String, String> map = new HashMap<String, String>();
        String uriWitParams = baseService.getUriWithParameters(httpServletResponse,
            toJson("{}"), "frr", conf, map, incomingRequestBody, new ArrayList<>());
        assertEquals("http://localhost:8080/someUrl?personIdCode=EE10101010005", uriWitParams);
    }

    @Test
    void getPostBodyTest() {
        JsonNode conf = toJson(bodyStruct);

        String postBody = baseService.getPostBody(httpServletResponse, toJson("{}"),
            "", conf, new HashMap<>(), incomingRequestBody, new ArrayList<>());

        log.info(postBody);
        assertEquals("{\"nested\":{\"something\":{\"currentDate\":\"" + (new SimpleDateFormat("dd.MM.yyyy")).format(new Date()) + "\",\"something2\":{\"holeincomingbody\":{\"body\":{\"requestBodyParam\":\"bodyParamValue\"}}}}}}",
            postBody);
    }

    @Test
    void handleResponseNokStopTest() {
        JsonNode responseConf = toJson("{\"response\":{\"ok\":\"proceed\",\"nok\":\"stop\"}}");
        CustomClientResponse customClientResponse = new CustomClientResponse(HttpStatus.BAD_REQUEST, "{\"body\":\"value\"}");
        assertTrue(baseService.handleResponse(httpServletResponse, handleResponseResult, "", responseConf, null, new ArrayList<>(), customClientResponse, true));

    }

    @Test
    void handleResponseNokProceedTest() {
        JsonNode responseConf = toJson("{\"response\":{\"ok\":\"proceed\",\"nok\":\"proceed\"}}");
        CustomClientResponse customClientResponse = new CustomClientResponse(HttpStatus.BAD_REQUEST, "{\"body\":\"value\"}");
        assertFalse(baseService.handleResponse(httpServletResponse, handleResponseResult, "", responseConf, null, new ArrayList<>(), customClientResponse, true));
    }

    @Test
    void handleResponseOkTest() {
        JsonNode responseConf = toJson("{\"response\":{\"ok\":\"proceed\",\"nok\":\"proceed\"}}");
        CustomClientResponse customClientResponse = new CustomClientResponse(HttpStatus.OK, "{\"body\":\"value\"}");
        baseService.handleResponse(httpServletResponse, handleResponseResult, "key", responseConf, new HashMap<>(), new ArrayList<>(), customClientResponse, true);
        assertEquals(toJson("{\"data\":{\"key\":{\"body\":\"value\"}},\"error\":null}"), handleResponseResult);
    }

    @Test
    void filterCookiesTest() {
        List<Cookie> allCookies = Arrays.asList(new Cookie("first", "first_val"), new Cookie("second", "second_val"), new Cookie("third", "third_val"));
        JsonNode allCookieConf = toJson("{\"cookies\":[\"ALL\"]}");
        JsonNode secondAndThirdCookies = toJson("{\"cookies\":[\"second\",\"third\"]}");

        assertEquals(filterCookies(allCookieConf, allCookies).size(), allCookies.size());
        assertEquals(2, filterCookies(secondAndThirdCookies, allCookies).size());
    }

}
