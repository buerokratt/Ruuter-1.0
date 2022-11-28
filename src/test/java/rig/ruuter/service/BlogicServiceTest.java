package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static rig.ruuter.TestUtils.getJsonNode;

@Slf4j
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@WireMockTest(httpPort = 23456)
class BlogicServiceTest {

    @Autowired
    private BlogicService blogicService;

    private HttpServletResponse httpServletResponse;

    @BeforeEach
    void beforeEach() {
        this.httpServletResponse = new HttpServletResponseWrapper(new MockHttpServletResponse());
    }

    @Test
    void testForwardResponse() throws IOException {
        JsonNode fwdConfig = getJsonNode("test_cfg/forwarded_response_chain.json");
        String expectedBody = getJsonNode("response/forwarded_response_chain_response.json")
            .toString();

        stubFor(post(urlPathEqualTo("/andmemuundur/json/v1/ekl"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"foo\":\"bar\"}")));

        stubFor(get(urlPathEqualTo("/jwt/userinfo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"personalCode\":\"EE10101010005\"}")));

        stubFor(post(urlPathEqualTo("/some-service/api/v1/xtg1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"personalCode\":\"EE10101010005\"}")));

        stubFor(get(urlPathEqualTo("/someUrl"))
            .withQueryParam("personIdCode", equalTo("EE10101010005"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"bar\":\"baz\"}")));

        stubFor(post(urlPathEqualTo("/fwd-endpoint/"))
            .withRequestBody(equalToJson(expectedBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("null")));

        Mono<ResponseEntity> result = blogicService.blogic(
            httpServletResponse, fwdConfig,
            null,
            null,
            null);

        ResponseEntity responseEntity = result.block();

        assertNotNull(responseEntity);
        assertTrue(responseEntity
            .getStatusCode()
            .is2xxSuccessful());
        assertNotNull(responseEntity.getBody());
        assertEquals("{\"data\":{\"forwardService\":null},\"error\":null}", responseEntity
            .getBody()
            .toString());
    }

    @Test
    void testReplace() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/replace_into_array/simple_replace.json");

        String incoming = "{\"data\":{\"fizz\":\"buzz\"}}";

        String mapped = "{\"data\":{\"foo\":\"buzz\",\"faa\":\"dim\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(mapped))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incoming,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testReplaceIntoArray() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/replace_into_array/array_replace.json");

        String incoming = "{\"data\":{\"fizz\":\"buzz\"}}";

        String mapped = "{\"data\":[{\"foo\":\"buzz\"}]}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(mapped))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incoming,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testHttpPut() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/http-put.json");
        String mapped = "{\"foo\":\"bar\"}";

        stubFor(put(urlPathEqualTo("/testUrl"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            List.of(),
            null,
            null);
        ResponseEntity response = result.block();

        assertNotNull(response);
        verify(1, putRequestedFor(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalToJson(mapped)));
    }

    @Test
    void testGetRequestHeaders() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/request_header/get-request-with-header.json");
        String incoming = "{\"apiKey\": \"testApiKey\"}";

        stubFor(get(urlPathEqualTo("/testUrl"))
            .willReturn(aResponse()));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            List.of(),
            incoming,
            null);
        ResponseEntity response = result.block();

        assertNotNull(response);
        verify(1, getRequestedFor(urlPathEqualTo("/testUrl"))
            .withHeader("x-api-key", equalTo("testApiKey")));
    }

    @Test
    void testPostRequestHeaders() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/request_header/post-request-with-header.json");
        String incoming = "{\"apiKey\": \"testApiKey\"}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .willReturn(aResponse()));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            List.of(),
            incoming,
            null);
        ResponseEntity response = result.block();

        assertNotNull(response);
        verify(1, postRequestedFor(urlPathEqualTo("/testUrl"))
            .withHeader("x-api-key", equalTo("testApiKey")));
    }

    @Test
    void testFunctionRunOnPostBody() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/run_method/run_method_from_post_body.json");

        String expectedQueryBody = "{\"data\":{\"bat\":\"bruce wayne\",\"man\":\"BRUCE WAYNE\",\"batman\":\"Bat-(a.k.a.)-BRUCE-WAYNE-man\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            null,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testFunctionRunOnPostBodyWithMappedValues() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/run_method/run_method_from_mapped_post_body.json");

        String expectedQueryBody = "{\"data\":{\"bat\":\"bruce WAYNE\",\"man\":\"BRUCE WAYNE\",\"batman\":\"Bruce Wayne\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"bat\":\"Bruce\",\"man\":\"Wayne\",\"batman\":\"Bruce Wayne\"}")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            null,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testFunctionRunOnPostBodyWithArray() throws IOException {
        JsonNode cfg = getJsonNode(
            "test_cfg/run_method/run_method_from_post_body_with_array.json");

        String incomingQueryBody = "{\"data\":{\"fizz\":\"BUZZ\"}}";
        String expectedQueryBody = "{\"data\":[{\"foo\":\"buzz\",\"bar\":\"foo bar\"}]}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testFunctionWithMultipleInputsRunOnPostBody() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/run_method/run_method_with_inputs_from_post_body.json");

        String incomingQueryBody = "{\"data\":{\"fizz\":\"BUZZ\"}}";
        String expectedQueryBody = "{\"data\":{\"foo\":\"true\",\"bar\":\"true\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testFunctionsInPostBodyAreIgnored() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/security/try_to_exploit_function_run.json");

        String incomingQueryBody = "{\"foo\":\"$_toUppercase(bar)\",\"bar\":\"$_encodeBase64(bar)\"}";
        String expectedQueryBody = "{\"data\":{\"foo\":\"$_toUppercase(bar)\",\"bar\":\"$_encodebase64(bar)\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testPostBodySpecialDesignatorsAreIgnored() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/security/try_to_exploit_mapping_from_body.json");

        String incomingQueryBody = "{\"bat\":\"{#.man}\",\"man\":\"Bruce Wayne\",\"batman\":\"{$.data$.foo}\"}";
        String expectedQueryBody = "{\"data\":{\"foo\":\"{#.man} {#.man}\",\"bar\":\"Bruce Wayne\",\"foobar\":\"{$.data$.foo}\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testSubQueryBodySpecialDesignatorsAreIgnored() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/security/try_to_exploit_mapping_from_subquery.json");

        String incomingQueryBody = "{\"foo\":\"bar\",\"bar\":\"{$.data$.exploit}\" }";
        String expectedQueryBody = "{\"data\":{\"foo\":\"{#.foo}\",\"bar\":\"{$.data$.exploit}\"}}";

        stubFor(get(urlPathEqualTo("/exploit"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"exploit\":\"{#.foo}\"}")));

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testArrayMappingWithPropertyDesignator() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/from_property_array_mapping.json");

        String incomingQueryBody = "{\"emails\":[{\"email\":\"some@mail1.com\",\"note\":\"somestring1\"},{\"email\": \"some@mail2.com\", \"note\":\"somestring2\"}]}";
        String expectedQueryBody = "{\"outArray\":[{\"address\":\"prefix-some@mail1.com-suffix\",\"activated\":true},{\"address\":\"prefix-some@mail2.com-suffix\",\"activated\":true}]}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testSpecialCharacters() throws IOException {
        JsonNode cfg = getJsonNode("test_cfg/special_characters.json");

        String incomingQueryBody = "{\"bat\":\"\\\"po\\\"\",\"man\":\"\\\"yo\\\"\"}";
        String expectedQueryBody = "{\"data\":{\"bat\":\"\\\"yo\\\"\",\"man\":\"\\\"po\\\"\",\"batman\":\"\\\"yo\\\"\"}}";

        stubFor(post(urlPathEqualTo("/testUrl"))
            .withRequestBody(equalTo(expectedQueryBody))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        Mono<ResponseEntity> result = blogicService.blogic(httpServletResponse,
            cfg,
            singletonList(new Cookie("JWTTOKEN", "cookie")),
            incomingQueryBody,
            null);
        ResponseEntity response = result.block();
        assertNotNull(response);
    }

    @Test
    void testRenameHeader() throws IOException {
        String oldCookieValue = "old-sad-bear";
        JsonNode renameRequestHeaderConf =
            getJsonNode("test_cfg/request_header/rename-request-header.json");

        stubFor(post(urlPathEqualTo("/sad-bear"))
            .withRequestBody(equalToJson("{}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(oldCookieValue)));

        ResponseEntity response = blogicService.blogic(httpServletResponse,
            renameRequestHeaderConf,
            singletonList(new Cookie("oldJwtName", oldCookieValue)),
            null,
            null)
            .block();

        assertNotNull(response);
        verify(1, postRequestedFor(urlPathEqualTo("/sad-bear"))
            .withCookie("newJwtName", equalTo(oldCookieValue)));
    }
}
