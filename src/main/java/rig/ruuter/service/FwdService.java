package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.util.CustomClientResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static rig.ruuter.configuration.routing.Configuration.skippableConfiguration;
import static rig.ruuter.constant.Constant.DESTINATION;
import static rig.ruuter.constant.Constant.RUUTER_BASE_RESPONSE_STRUCTURE;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
@Service
@Timed
public class FwdService extends BaseService {

    /**
     * @param httpServletResponse forwarded from Controller
     * @param conf configuration found by configuration code specified by request parameter(code) in controller
     * @param cookies request cookies
     * @param incomingRequestBody request body
     * @param requestParams request parameters
     * @return response the content of which if filled by requests specified in requestBody
     *
     * For each field in destination node if it is not skippable try to retrieve a response. In case of 4XX responses
     * handle those according to configuration to determine if the following requests should be stopped
     */
    public Mono<ResponseEntity> fwd(HttpServletResponse httpServletResponse, JsonNode conf,
                                    List<Cookie> cookies,
                                    String incomingRequestBody,
                                    Map<String, String> requestParams) {
        Iterator<Map.Entry<String, JsonNode>> fields = conf.get(DESTINATION)
                .fields();
        JsonNode resultBody = toJson(RUUTER_BASE_RESPONSE_STRUCTURE);
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!skippableConfiguration(entry.getValue())) {
                CustomClientResponse response = retrieveTimedResponse(httpServletResponse, resultBody, entry.getKey(), entry.getValue(), requestParams, incomingRequestBody, cookies);
                if (handleResponse(httpServletResponse, resultBody, entry.getKey(), entry.getValue(), requestParams, cookies, response)) {
                    return response != null && response.getCode() != null ? response(response.getBody(), response.getCode()) : response(HttpStatus.BAD_REQUEST);
                }
            }
        }
        return response(resultBody);
    }
}
