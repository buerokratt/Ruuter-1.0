package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.configuration.routing.Configuration;
import rig.ruuter.logging.LoggingContext;
import rig.ruuter.util.CustomClientResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static rig.ruuter.configuration.routing.Configuration.skippableConfiguration;
import static rig.ruuter.constant.Constant.*;
import static rig.ruuter.util.CustomClientResponse.statusOk;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@Service
@Timed
public class BlogicService extends BaseService {

    private final FileHandlingService fileHandlingService;

    @Autowired
    public BlogicService(FileHandlingService fileHandlingService) {
        this.fileHandlingService = fileHandlingService;
    }

    /**
     * @param httpServletResponse forwarded from Controller
     * @param conf                configuration found by configuration code specified by request parameter(code) in controller
     * @param cookies             request cookies
     * @param incomingRequestBody request body
     * @param requestParams       request parameters
     * @return response the content of which if filled by requests specified in requestBody
     * @see rig.ruuter.controller.RuuterController#getRoute(HttpServletRequest, HttpServletResponse, String, String, Map)
     * @see Configuration#find(String)
     */
    public Mono<ResponseEntity> blogic(HttpServletResponse httpServletResponse,
                                       JsonNode conf,
                                       List<Cookie> cookies,
                                       String incomingRequestBody,
                                       Map<String, String> requestParams) {

        JsonNode resultBody = toJson(RUUTER_BASE_RESPONSE_STRUCTURE);
        if (conf.get(DESTINATION).getNodeType().equals(JsonNodeType.ARRAY)) {
            Iterator<JsonNode> destinationArray = conf.get(DESTINATION).elements();

            log.debug("Destination node type was array, start constructing result body");

            ObjectNode responseHistory = (ObjectNode) toJson("{}");

            while (destinationArray.hasNext()) {
                log.debug("Getting next destination node from array");
                Iterator<Map.Entry<String, JsonNode>> destination = destinationArray.next().fields();
                resultBody = toJson(RUUTER_BASE_RESPONSE_STRUCTURE);

                while (destination.hasNext()) {

                    log.debug("Getting destinations from destination node");
                    Map.Entry<String, JsonNode> setting = destination.next();
                    LoggingContext.setDestinationContext(setting.getKey());
                    JsonNode destinations = setting.getValue();

                    // verify should result in following action
                    // proceed or proceed_with_mock or stop
                    String proceeding = getProceedingPathAfterVerify(httpServletResponse,
                        destinations.get(VERIFY),
                        requestParams,
                        incomingRequestBody,
                        cookies);

                    if (proceeding != null && !STOP.equals(proceeding) && destinations.has(proceeding)) {
                        log.debug("Proceeding after verification: ");
                        JsonNode proceedingConf = destinations.get(proceeding);

                        if (!skippableConfiguration(destinations.get(proceeding))) {
                            CustomClientResponse response;
                            if (proceedingConf.has(MULTI_REQUEST)) {
                                response = retrieveMultiRequestResponse(httpServletResponse,
                                    resultBody, setting.getKey(), proceedingConf, requestParams, incomingRequestBody, cookies);
                            } else {
                                response = retrieveTimedResponse(httpServletResponse,
                                    resultBody, setting.getKey(), proceedingConf, requestParams, incomingRequestBody, cookies);
                            }

                            if (handleResponse(httpServletResponse, resultBody, setting.getKey(), proceedingConf, requestParams, cookies, response)) {
                                log.debug("Returning response {}", response);
                                return response != null && response.getCode() != null ?
                                    response(response.getBody(), response.getCode()) :
                                    response(HttpStatus.BAD_REQUEST);
                            } else {
                                addCustomHeaders(httpServletResponse, proceedingConf, resultBody, incomingRequestBody);
                                passthroughResponseHeaders(httpServletResponse, proceedingConf, response);
                            }

                            responseHistory.set(setting.getKey(), resultBody.get(DATA).get(setting.getKey()));
                            incomingRequestBody = responseHistory.toString();

                        } else {
                            log.info("Skippable configuration. I will do nothing.");
                        }
                    } else if (STOP.equalsIgnoreCase(proceeding)) {
                        log.info("Proceeding action was to stop for BLOGIC request");
                        return response(HttpStatus.FORBIDDEN);
                    } else {
                        log.warn("Proceeding action was not found for BLOGIC request");
                        return response(HttpStatus.NOT_FOUND);
                    }
                }
            }

        } else if (conf.get(DESTINATION).getNodeType().equals(JsonNodeType.OBJECT)) {
            log.debug("Destination node type was object, start constructing result body");
            Iterator<Map.Entry<String, JsonNode>> destination = conf.get(DESTINATION).fields();
            while (destination.hasNext()) {
                log.debug("Getting destinations from destination node");
                Map.Entry<String, JsonNode> setting = destination.next();
                LoggingContext.setDestinationContext(setting.getKey());
                JsonNode destinations = setting.getValue();

                // verify should result in following action
                // proceed or proceed_with_mock or stop
                String proceeding = getProceedingPathAfterVerify(httpServletResponse,
                    destinations.get(VERIFY),
                    requestParams,
                    incomingRequestBody,
                    cookies);

                if (proceeding != null && !STOP.equals(proceeding) && destinations.has(proceeding)) {
                    JsonNode proceedingConf = destinations.get(proceeding);
                    if (!skippableConfiguration(destinations.get(proceeding))) {
                        CustomClientResponse response =
                            retrieveTimedResponse(httpServletResponse, resultBody, setting.getKey(), proceedingConf, requestParams, incomingRequestBody, cookies);
                        if (handleResponse(httpServletResponse, resultBody, setting.getKey(), proceedingConf, requestParams, cookies, response)) {
                            return response != null && response.getCode() != null ?
                                response(response.getBody(), response.getCode()) :
                                response(HttpStatus.BAD_REQUEST);
                        } else {
                            addCustomHeaders(httpServletResponse, proceedingConf, resultBody, incomingRequestBody);
                            passthroughResponseHeaders(httpServletResponse, proceedingConf, response);
                        }
                    } else {
                        log.info("Skippable configuration. I will do nothing.");
                    }
                } else if (STOP.equalsIgnoreCase(proceeding)) {
                    log.info("Proceeding action was to stop for BLOGIC request");
                    return response(HttpStatus.FORBIDDEN);
                } else {
                    log.warn("Proceeding action was not found for BLOGIC request");
                    return response(HttpStatus.NOT_FOUND);
                }
            }
        } else {
            return response(HttpStatus.BAD_REQUEST);
        }

        log.debug("Final response {}", resultBody);

        return response(resultBody);
    }


    /**
     * @param returnAsJson should the response contain file as Application/Octet stream or its content as json
     * @return response containing the file or its content as json
     * @see #blogic(HttpServletResponse, JsonNode, List, String, Map)
     * the same as above but dealing with the file download specifically
     * there is an additional parameter
     */
    public Mono<ResponseEntity> download(HttpServletResponse httpServletResponse,
                                         JsonNode conf,
                                         List<Cookie> cookies,
                                         String incomingRequestBody,
                                         Map<String, String> requestParams,
                                         boolean returnAsJson) {

        Iterator<Map.Entry<String, JsonNode>> destinations = conf
            .get(DESTINATION).fields();

        // since it's download, we can only check for 1 destination
        if (destinations.hasNext()) {
            JsonNode destination = destinations.next().getValue();
            String proceeding = getProceedingPathAfterVerify(httpServletResponse,
                destination.get(VERIFY),
                requestParams,
                incomingRequestBody,
                cookies);

            if (proceeding != null && !STOP.equals(proceeding) && destination.has(proceeding)) {
                JsonNode proceedingConf = destination.get(proceeding);
                if (!skippableConfiguration(proceedingConf)) {
                    return returnAsJson
                        ? fileHandlingService.saveFileAndGetLocation(httpServletResponse,
                        proceedingConf,
                        requestParams,
                        incomingRequestBody,
                        cookies)
                        : fileHandlingService.getFileAndPrepareForUpload(httpServletResponse,
                        proceedingConf,
                        requestParams,
                        incomingRequestBody,
                        cookies);
                } else {
                    log.info("Skippable configuration. I will do nothing.");
                    return response(HttpStatus.OK);
                }
            } else if (STOP.equalsIgnoreCase(proceeding)) {
                log.info("Proceeding action was to stop for DOWNLOAD request");
                return response(HttpStatus.FORBIDDEN);
            } else {
                log.warn("Proceeding action was not found for DOWNLOAD request");
                return response(HttpStatus.NOT_FOUND);
            }
        }
        log.info("No destinations configuration found for DOWNLOAD request");
        return response(HttpStatus.NOT_FOUND);
    }

    private String getProceedingPathAfterVerify(HttpServletResponse httpServletResponse,
                                                JsonNode verifyNode,
                                                Map<String, String> requestParams,
                                                String incomingRequestBody,
                                                List<Cookie> cookies) {
        Iterator<Map.Entry<String, JsonNode>> fields = verifyNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode verify = field.getValue();
            if (!skippableConfiguration(verify) && verify.has(ENDPOINT)) {
                CustomClientResponse status = retrieveTimedResponse(httpServletResponse, toJson(""), field.getKey(), verify, requestParams, incomingRequestBody, cookies);
                if (status == null || !statusOk(status)) {
                    log.info("Verification returned status code {} for configuration node " +
                        "{} and request params {}", status, verify, requestParams);
                    return verifyNode.get(RESPONSE).get(RESP_NOK).asText();
                }
            }
        }
        return verifyNode.get(RESPONSE).get(RESP_OK).asText();
    }

}
