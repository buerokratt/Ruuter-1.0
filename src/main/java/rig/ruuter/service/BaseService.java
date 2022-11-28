package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import rig.commons.aop.Timed;
import rig.ruuter.enums.ResponseNokType;
import rig.ruuter.enums.Splitter;
import rig.ruuter.util.CustomClientResponse;
import rig.ruuter.util.JsonUtils;
import rig.ruuter.util.StrUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static rig.ruuter.constant.Constant.*;
import static rig.ruuter.enums.Splitter.FROM_CONFIG_PARAM;
import static rig.ruuter.enums.Splitter.FROM_INCOMING_BODY;
import static rig.ruuter.service.RequestBodyMappingUtils.extract;
import static rig.ruuter.util.CustomClientResponse.statusOk;
import static rig.ruuter.util.JsonUtils.findAndReplace;
import static rig.ruuter.util.JsonUtils.jsonSet;
import static rig.ruuter.util.RestUtils.httpGet;
import static rig.ruuter.util.RestUtils.httpRequestWithBody;
import static rig.ruuter.util.StrUtils.toJson;
import static rig.ruuter.util.StrUtils.unWrapQuotes;

@Slf4j
@Service
@Timed
public class BaseService {

    @Value("${legacy-portal-integration.sessionCookieDomain}")
    private String sessionCookieDomain;

    @Autowired
    private MeterRegistry registry;

    public BaseService() {
    }

    public BaseService(MeterRegistry registry) {
        this.registry = registry;
    }

    public CustomClientResponse retrieveTimedResponse(HttpServletResponse httpServletResponse, JsonNode resultBody,
                                                      String key, JsonNode endpointConf, Map<String, String> requestParams,
                                                      String incomingRequestBody, List<Cookie> allCookies) {
        Timer.Sample sample = Timer.start(registry);
        String responseCode = "";
        try {
            CustomClientResponse customClientResponse = retrieveResponse(httpServletResponse, resultBody, key,
                endpointConf, requestParams, incomingRequestBody, allCookies);
            responseCode = getRetrieverResponseCode(customClientResponse);
            return customClientResponse;
        } catch (Exception ex) {
            responseCode = "500";
            throw ex;
        } finally {
            sample.stop(Timer.builder("router.endpoint")
                .tag("endpoint", endpointConf.get("endpoint").textValue())
                .tag("response_code", responseCode)
                .register(registry));
        }
    }

    private String getRetrieverResponseCode(CustomClientResponse customClientResponse) {
        if (customClientResponse != null) {
            return String.valueOf(customClientResponse.getCode().value());
        }
        return "400";
    }

    private String getRequestCookies(List<Cookie> requestCookies) {
        if (requestCookies == null || requestCookies.isEmpty()) {
            log.warn("No cookies in request");
            return "";
        }

        HashMap<String, String> stringStringHashMap = new HashMap<>();
        requestCookies.forEach(cookie -> stringStringHashMap.put(cookie.getName(), cookie.getValue()));

        JsonNode cookiesNode = JsonUtils.pojoToJson(stringStringHashMap);
        return cookiesNode == null ? "" : cookiesNode.toString();
    }

    /**
     * Performs either get or post with corresponding recursive requests
     * returns a tuple of ClientResponse, Boolean boolean indicates if a request should be stopped.
     *
     * @return Pair of response and boolean
     */
    public CustomClientResponse retrieveResponse(HttpServletResponse httpServletResponse,
                                                 JsonNode resultBody,
                                                 String key,
                                                 JsonNode endpointConf,
                                                 Map<String, String> requestParams,
                                                 String incomingRequestBody,
                                                 List<Cookie> allCookies) {
        if (endpointConf.has(METHOD)) {
            List<Cookie> cookies = filterCookies(endpointConf, allCookies);
            HttpHeaders headers = getRequestHeaders(endpointConf, incomingRequestBody);
            switch (endpointConf.get(METHOD).textValue()) {
                case POST:
                case PUT:
                    String postUri = getUriWithParameters(httpServletResponse, resultBody, key, endpointConf, requestParams, incomingRequestBody, allCookies);
                    String postBody = getPostBody(httpServletResponse, resultBody, key, endpointConf, requestParams, incomingRequestBody, allCookies);
                    return postUri == null || postBody == null ? null : httpRequestWithBody(endpointConf.get(METHOD).textValue(), endpointConf, postUri, postBody, cookies, headers);
                case GET:
                    String uri = getUriWithParameters(httpServletResponse, resultBody, key, endpointConf, requestParams, incomingRequestBody, allCookies);
                    return uri == null ? null : httpGet(endpointConf, uri, cookies, headers);
                default:
                    log.warn("Unrecognized method property value {}", endpointConf.get(METHOD).textValue());
                    return null;
            }
        } else {
            log.warn("No property method found in configuration {}", endpointConf);
        }
        log.warn("Endpoint configuration has no method property. {}", endpointConf);
        return null;
    }

    public CustomClientResponse retrieveMultiRequestResponse(HttpServletResponse httpServletResponse,
                                                             JsonNode resultBody,
                                                             String key,
                                                             JsonNode endpointConf,
                                                             Map<String, String> requestParams,
                                                             String incomingRequestBody,
                                                             List<Cookie> allCookies) {

        if (!endpointConf.has(MULTI_REQUEST) ||
            !endpointConf.get(MULTI_REQUEST).has(MULTI_REQUEST_FIELD) ||
            !endpointConf.get(MULTI_REQUEST).has(MULTI_REQUEST_COLLECTION)) {

            log.error("Invalid {} configuration. Using default instead", MULTI_REQUEST);
            return retrieveTimedResponse(httpServletResponse, resultBody, key, endpointConf, requestParams, incomingRequestBody, allCookies);
        }

        JsonNode multiRequestCollection = extract(toJson(incomingRequestBody),
            endpointConf.get(MULTI_REQUEST).get("collection").asText(), Splitter.FROM_INCOMING_BODY);
        if (multiRequestCollection == null || !multiRequestCollection.isArray()) {
            log.error("Invalid multirequest collection input. {}", multiRequestCollection);
            return null;
        }

        ArrayNode resultBodyArray = (ArrayNode) toJson("[]");
        ArrayNode resultHeaderArray = (ArrayNode) toJson("[]");
        for (JsonNode jsonNode : multiRequestCollection) {
            String multiRequestKey = endpointConf.get(MULTI_REQUEST).get(MULTI_REQUEST_FIELD).asText();
            String multiRequestExtractPath = endpointConf.get(POST_BODY_STRUCT).get(PARAMETERS).get(multiRequestKey).asText();

            JsonNode extraction = extract(jsonNode, multiRequestExtractPath, Splitter.FROM_INCOMING_BODY);

            JsonNode endpointConfCopy = endpointConf.deepCopy();
            ((ObjectNode) endpointConfCopy.get(POST_BODY_STRUCT).get(PARAMETERS)).set(multiRequestKey, extraction);

            CustomClientResponse respose = retrieveTimedResponse(httpServletResponse,
                resultBody, key, endpointConfCopy, requestParams, incomingRequestBody, allCookies);

            resultBodyArray.add(toJson(respose.getBody()).get(RESPONSE));
            resultHeaderArray.add(toJson(respose.getBody()).get(REQUEST));
        }

        JsonNode response = toJson("{}");
        ((ObjectNode) response).set("request", resultHeaderArray);
        ((ObjectNode) response).set("response", resultBodyArray);

        return new CustomClientResponse(HttpStatus.OK, response.toString());
    }

    /**
     * extracts endpoint property value from json node, replaces payload into endpoint if endpoint
     * has a regex for it, performs necessary get requests to fill in the gaps of url
     */
    public String getUriWithParameters(HttpServletResponse httpServletResponse,
                                       JsonNode resultBody,
                                       String key,
                                       JsonNode nodeWithEndpoint,
                                       Map<String, String> requestParams,
                                       String incomingRequestBody,
                                       List<Cookie> allCookies) {

        if (nodeWithEndpoint.has(ENDPOINT)) {

            String uri = StrUtils.findAndReplaceParameters(nodeWithEndpoint.get(ENDPOINT).asText(), requestParams);

            if (incomingRequestBody != null)
                uri = StrUtils.findAndReplaceParameters(uri, toJson(incomingRequestBody));

            if (nodeWithEndpoint.has(ENDPOINT_PARAMETERS)) {
                Iterator<Map.Entry<String, JsonNode>> parameters = nodeWithEndpoint.get(ENDPOINT_PARAMETERS).fields();

                while (parameters.hasNext()) {
                    Map.Entry<String, JsonNode> parameter = parameters.next();
                    if (uri.contains("{$" + parameter.getKey() + "}")) {
                        CustomClientResponse response = retrieveTimedResponse(httpServletResponse,
                            resultBody,
                            key,
                            parameter.getValue(),
                            requestParams,
                            incomingRequestBody,
                            allCookies);
                        if (statusOk(response)) {
                            try {
                                uri = uri.replaceAll(
                                    "\\{\\$" + parameter.getKey() + "\\}",
                                    URLEncoder.encode(unWrapQuotes(response.getBody()), StandardCharsets.UTF_8.name()));
                            } catch (UnsupportedEncodingException e) {
                                log.info("Unable to encode URI part", e);
                                return null;
                            }
                        } else {
                            return null;
                        }
                    } else {
                        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
                        Matcher matcher = pattern.matcher(uri);
                        while (matcher.find()) {
                            if (matcher.group(1).startsWith("#." + parameter.getKey())) {
                                String[] extracts = matcher.group(1).substring(2).split("#\\.");
                                if (extracts.length > 1) {
                                    CustomClientResponse response = retrieveTimedResponse(httpServletResponse,
                                        resultBody,
                                        key,
                                        parameter.getValue(),
                                        requestParams,
                                        incomingRequestBody,
                                        allCookies);
                                    if (statusOk(response)) {
                                        JsonNode body = toJson(response.getBody());
                                        JsonNode extracted = extractFromJson(body, extracts);
                                        try {
                                            uri = uri.replaceAll(
                                                Pattern.quote("{" + matcher.group(1) + "}"),
                                                URLEncoder.encode(unWrapQuotes(extracted.toString()), StandardCharsets.UTF_8.name()));
                                        } catch (UnsupportedEncodingException e) {
                                            log.info("Unable to encode URI part", e);
                                            return null;
                                        }
                                    } else {
                                        return null;
                                    }
                                }

                            }
                        }
                    }
                }
            }
            log.debug("Request uri: {}", uri);
            return uri;
        }
        log.warn("No endpoint property in configuration node {}", nodeWithEndpoint);
        return null;
    }

    private JsonNode extractFromJson(JsonNode json, String[] extracts) {
        for (int i = 0; i < extracts.length; i++) {
            if (i == 0) {
                continue;
            }
            if (json != null) {
                json = json.get(extracts[i]);
            } else {
                return null;
            }
        }
        return json;
    }

    /**
     * extract post_body_struct property, replaces values from incoming request's body into it,
     * performs requests specified in post_body_parameters and replaces the result of those requests into post_body_struct
     * prefers incoming request body over requests
     */
    public String getPostBody(HttpServletResponse httpServletResponse,
                              JsonNode resultBody,
                              String key,
                              JsonNode postRequestNode,
                              Map<String, String> requestParams,
                              String incomingRequestBody,
                              List<Cookie> allCookies) {
        JsonNode bodyParametersNode = toJson("{}");
        if (postRequestNode.has(POST_BODY_PARAMETERS)) {
            Iterator<Map.Entry<String, JsonNode>> parameters = postRequestNode.get(POST_BODY_PARAMETERS)
                .fields();
            while (parameters.hasNext()) {
                Map.Entry<String, JsonNode> bodyParameter = parameters.next();
                CustomClientResponse response = retrieveTimedResponse(httpServletResponse,
                    resultBody,
                    key,
                    bodyParameter.getValue(),
                    requestParams,
                    incomingRequestBody,
                    allCookies);
                if (response == null || !statusOk(response)) {
                    handleResponse(httpServletResponse, resultBody, key, bodyParameter.getValue(), requestParams, allCookies, response, false);
                    return null;
                } else {
                    ((ObjectNode) bodyParametersNode).set(bodyParameter.getKey(), toJson(response.getBody()));
                }
            }
        }

        JsonNode postBodyStruct = postRequestNode.get(POST_BODY_STRUCT);

        findAndReplace(postBodyStruct, "{$cookies$}", getRequestCookies(allCookies));
        findAndReplace(postBodyStruct, "{$currentDate$}", (new SimpleDateFormat("dd.MM.yyyy")).format(new Date()));
        findAndReplace(postBodyStruct, "{$currentDateTime$}", (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).format(new Date()));

        if (isHardcodedValue(postRequestNode)) return postBodyStruct.textValue();
        postBodyStruct = RequestBodyMappingUtils.mapJson(postBodyStruct, bodyParametersNode, toJson(incomingRequestBody));
        return postBodyStruct.toString();
    }

    /**
     * if response code is 4xx and error handling specified - do error handling
     * if response.nok is stop - stop the request completely, returns response code if present, bad request otherwise
     *
     * @return if this request should be terminated
     */
    public boolean handleResponse(HttpServletResponse httpServletResponse, JsonNode resultBody, String key, JsonNode destination,
                                  Map<String, String> requestParams, List<Cookie> cookies,
                                  CustomClientResponse response) {
        return handleResponse(httpServletResponse, resultBody, key, destination, requestParams, cookies, response, true);
    }

    /**
     * if response code is 200 and @addToResult is true - append it to resultBody
     * if response code is 4xx and error handling specified - do error handling
     * if response.nok is stop - stop the request completely, returns response code if present, bad request otherwise
     *
     * @return if this request should be terminated
     */
    boolean handleResponse(HttpServletResponse httpServletResponse, JsonNode resultBody, String key, JsonNode destination,
                           Map<String, String> requestParams, List<Cookie> cookies,
                           CustomClientResponse response, boolean addToResult) {

        if (addToResult && response != null && statusOk(response)) {
            jsonSet(resultBody.get(DATA), key, response.getBody());

            if (destination.has(OUTPUT_FROM) && destination.get(OUTPUT_FROM).isTextual()) {
                JsonNode outputFrom = extract(resultBody.deepCopy().get(DATA),
                    destination.get(OUTPUT_FROM).asText(),
                    Splitter.FROM_CONFIG_PARAM);
                ((ObjectNode) resultBody).remove(DATA);
                ((ObjectNode) resultBody).set(DATA, outputFrom);
            }

            return false;
        }
        if (destination != null && destination.has(RESPONSE)) {

            // see explanations of each action in ResponseNokType enum
            switch (ResponseNokType.fromValue(
                destination.get(RESPONSE).get(RESP_NOK).textValue())) {
                case ON_ERROR:
                    log.info("Error handling action was to report it to EH.");
                    onError(httpServletResponse, resultBody, key, destination, requestParams, cookies, response);
                    return false;
                case ON_ERROR_AND_STOP:
                    log.info("Error handling action was to report it to EH and STOP THE REQUEST.");
                    onError(httpServletResponse, resultBody, key, destination, requestParams, cookies, response);
                    return true;
                case STOP:
                    log.info("Error handling action was to stop the request all together.");
                    return true;
                case PROCEED:
                    log.info("Error handling action was to proceed with the request.");
                    return false;
                case IGNORE_ERROR:
                    log.info("Error handling action was to ignore the error.");
                    jsonSet(resultBody.get(DATA), key, response != null ? response.getBody() : null);
                    return false;
                default:
                    log.info("No response error handling action recognized, won't do any error handling," +
                        " adding initial to error section. conf {}", destination);
                    if (resultBody.get(ERROR).isNull()) {
                        jsonSet(resultBody, ERROR, "{}");
                    }
                    jsonSet(resultBody.get(ERROR), key, response != null ? response.getBody() : null);
                    return false;
            }
        }
        return false;
    }

    void passthroughResponseHeaders(HttpServletResponse httpServletResponse, JsonNode destination, CustomClientResponse response) {
        if (response == null) return;

        HttpHeaders responseHeaders = response.getHeaders();
        JsonNode confNode = destination.get(PASSTHROUGH_RESPONSE_HEADERS);

        if (destination.has(PASSTHROUGH_RESPONSE_HEADERS) && confNode.getNodeType().equals(JsonNodeType.ARRAY)) {
            if (responseHeaders == null || responseHeaders.isEmpty()) {
                log.warn("Passthrough response headers specified, but no response headers found in configuration {}", destination);
                return;
            }

            HashMap<String, String> matchingHeaders = new HashMap<>();
            confNode.forEach(headerToPass -> responseHeaders.forEach((key, values) -> {
                if (key.equalsIgnoreCase(headerToPass.textValue())) {
                    matchingHeaders.put(headerToPass.textValue(), String.join(", ", values));
                }
            }));

            if (matchingHeaders.isEmpty()) {
                log.warn("Passthrough response headers specified, but no matching response headers found in configuration {}", destination);
                return;
            }

            matchingHeaders.forEach(httpServletResponse::setHeader);
        }
    }

    void addCustomHeaders(HttpServletResponse httpServletResponse, JsonNode destination, JsonNode resultBody, String incomingRequestBody) {
        if (destination.has(RESPONSE_HEADER) && destination.get(RESPONSE_HEADER).isObject()) {
            Iterator<String> headersIterator = destination.get(RESPONSE_HEADER).fieldNames();
            while (headersIterator.hasNext()) {

                String header = headersIterator.next();
                JsonNode headerValue = destination.get(RESPONSE_HEADER).get(header);
                JsonNode contents;

                if (httpServletResponse != null) {
                    if (header.equals(HttpHeaders.SET_COOKIE) || header.equals(HttpHeaders.SET_COOKIE2)) {
                        if (headerValue.isObject()) {
                            contents = getContents(resultBody, incomingRequestBody, headerValue);
                            if (contents != null) {
                                setCookie(httpServletResponse, headerValue, contents);
                            }
                        } else if (headerValue.isArray()) {
                            for (JsonNode val : headerValue) {
                                contents = getContents(resultBody, incomingRequestBody, val);
                                if (contents != null) {
                                    setCookie(httpServletResponse, val, contents);
                                }
                            }
                        }
                    } else {
                        contents = getContents(resultBody, incomingRequestBody, headerValue);
                        httpServletResponse.setHeader(header, contents.asText());
                    }
                }
            }
        }
    }

    private JsonNode getContents(JsonNode resultBody, String incomingRequestBody, JsonNode headerValue) {
        JsonNode results = null;
        String fromProperty = FROM_INCOMING_BODY.getVal().concat(".from_property");
        if (headerValue.isTextual()) {
            results = extract(resultBody.deepCopy(), headerValue.asText(), FROM_INCOMING_BODY);
        } else if (headerValue.has(fromProperty) &&
            headerValue.get(fromProperty).isTextual()) {

            if (headerValue.get(fromProperty).asText().equals(INCOMING_BODY)) {
                results = StrUtils.toJson(incomingRequestBody);
            } else {
                JsonNode resultBodyExt = resultBody.get(DATA).deepCopy();
                results = extract(resultBodyExt.deepCopy(), headerValue.get(fromProperty).asText(), FROM_INCOMING_BODY);

                if (results == null) {
                    results = extract(toJson(incomingRequestBody),
                        headerValue.get(fromProperty).asText(), FROM_INCOMING_BODY);
                }
            }
        } else if (headerValue.has("value")) {
            results = headerValue.get("value");
        }
        return results;
    }


    private void setCookie(HttpServletResponse httpServletResponse, JsonNode headerValue, JsonNode contents) {
        String cookieName = headerValue.has("name") ?
            headerValue.get("name").asText() : DEFAULT_CUSTOM_JWT_NAME;
        String cookieBody = null;
        if (contents.isTextual()) {
            cookieBody = contents.asText();
        } else {
            try {
                cookieBody = URLEncoder.encode(contents.toString(), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                log.info("Unable to parse cookie contents", e);
            }
        }

        Cookie c = new Cookie(cookieName, cookieBody);
        boolean httpOnly = true;
        if (headerValue.has("httpOnly")) {
            httpOnly = headerValue.get("httpOnly").asBoolean();
        }
        c.setHttpOnly(httpOnly);

        boolean setSecure = true;
        if (headerValue.has("setSecure") && headerValue.get("setSecure").isBoolean()) {
            setSecure = headerValue.get("setSecure").asBoolean();
        }
        c.setSecure(setSecure);

        if (headerValue.has("maxAge") && headerValue.get("maxAge").isNumber()) {
            c.setMaxAge(headerValue.get("maxAge").asInt());
        }

        c.setPath(headerValue.has("path") ? headerValue.get("path").asText() : "/");
        c.setDomain(headerValue.has("domain") ? headerValue.get("domain").asText() : sessionCookieDomain);

        httpServletResponse.addCookie(c);
    }


    /**
     * add  errorReported=true to response
     */
    public void onError(HttpServletResponse httpServletResponse, JsonNode resultBody, String key, JsonNode destination,
                        Map<String, String> requestParams, List<Cookie> cookies,
                        CustomClientResponse response) {
        if (response != null && !response.getErrorReported()) {
            if (resultBody.get(ERROR).isNull()) {
                jsonSet(resultBody, ERROR, "{}");
            }
            if (destination.has(ON_ERROR)) {
                HashMap<String, String> errorPayloadMap = new HashMap<>();
                if (response.getCode() != null) {
                    errorPayloadMap.put("payload", response.getCode().value() + "");
                }
                CustomClientResponse errorHandlerResponse = retrieveTimedResponse(httpServletResponse, resultBody, key,
                    destination.get(ON_ERROR), response.getCode() != null ? errorPayloadMap : requestParams, null, cookies);
                log.info("Adding to error {}", errorHandlerResponse.getBody());
                jsonSet(resultBody.get(ERROR), key, errorHandlerResponse.getBody());
            } else {
                log.warn("Response nok action is set to error handling but not on error configuration defined." +
                    " Adding initial to error section. Conf {}", destination);
                jsonSet(resultBody.get(ERROR), key, response.getBody());
            }
            response.errorReported();
        }
    }

    /**
     * @param conf           allowed Cookies configuration
     * @param requestCookies Cookies in request
     * @return list of Cookies filtered according to configuration
     */
    public static List<Cookie> filterCookies(JsonNode conf, List<Cookie> requestCookies) {
        if (conf.has(COOKIES) && conf.get(COOKIES).getNodeType().equals(JsonNodeType.ARRAY)) {
            if (requestCookies == null || requestCookies.isEmpty()) {
                log.warn("Configuration node had cookies specified, but incoming request didn't have any cookies. conf {}", conf);
                return emptyList();
            }
            if (conf.get(COOKIES).size() == 1 && "\"ALL\"".equals(conf.get(COOKIES).get(0).toString())) {
                return requestCookies;
            }
            List<String> allowedCookieNames = new ArrayList<>();
            for (JsonNode node : conf.get(COOKIES)) {
                allowedCookieNames.add(node.asText());
            }
            return requestCookies.stream()
                .filter(c -> allowedCookieNames.contains(c.getName())).collect(toList());
        }
        if (conf.has(COOKIES) && conf.get(COOKIES).isTextual() && conf.get(COOKIES).asText().equals("*")) {
            return requestCookies;
        }
        if (conf.has(COOKIES) && conf.get(COOKIES).getNodeType().equals(JsonNodeType.OBJECT)) {
            if (requestCookies == null || requestCookies.isEmpty()) {
                log.warn("Configuration node had cookies specified, but incoming request didn't have any cookies. conf {}", conf);
                return emptyList();
            }

            HashMap<String, String> cookieMap = new HashMap<>();
            if (conf.get(COOKIES).isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = conf.get(COOKIES).fields();
                fields.forEachRemaining(field -> cookieMap.put(field.getKey(), field.getValue().textValue()));
            }
            ;

            List<Cookie> renamedCookies = new ArrayList<>();
            cookieMap.forEach((currentName, newName) -> {
                String cookieValue = requestCookies.stream()
                    .filter(cookie -> currentName.equalsIgnoreCase(cookie.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(String.format("Could not find cookie with name: %s", currentName)))
                    .getValue();
                renamedCookies.add(new Cookie(newName, cookieValue));
            });
            return renamedCookies;
        }
        return emptyList();
    }

    /**
     * @param response to get the body from
     * @return body of clientResponse
     */
    public static String getResponseBody(ClientResponse response) {
        if (response == null) {
            log.warn("Response is null, cannot extract body.");
            return null;
        }
        return response.bodyToMono(String.class).block();
    }

    private boolean isHardcodedValue(JsonNode postRequestNode) {
        return postRequestNode.has(POST_BODY_STRUCT) &&
            postRequestNode.get(POST_BODY_STRUCT).isTextual() &&
            (!postRequestNode.get(POST_BODY_STRUCT).textValue().contains(FROM_INCOMING_BODY.getVal()) ||
                postRequestNode.get(POST_BODY_STRUCT).textValue().contains(FROM_CONFIG_PARAM.getVal()));
    }

    private HttpHeaders getRequestHeaders(JsonNode destination, String incomingRequestBody) {
        HttpHeaders headers = new HttpHeaders();

        if (destination.has(REQUEST_HEADER) && destination.get(REQUEST_HEADER).isObject()) {
            Iterator<String> headersIterator = destination.get(REQUEST_HEADER).fieldNames();
            while (headersIterator.hasNext()) {
                String header = headersIterator.next();
                JsonNode headerValue = destination.get(REQUEST_HEADER).get(header);

                JsonNode contents = extract(toJson(incomingRequestBody), headerValue.asText(), FROM_INCOMING_BODY);
                if (contents != null) {
                    headers.add(header, contents.asText());
                }
            }
        }
        return headers;
    }
}
