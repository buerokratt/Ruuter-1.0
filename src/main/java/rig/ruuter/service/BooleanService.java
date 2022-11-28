package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import static rig.ruuter.constant.Constant.*;
import static rig.ruuter.util.CustomClientResponse.statusOk;
import static rig.ruuter.util.RestUtils.response;
import static rig.ruuter.util.StrUtils.toJson;


@Slf4j
@Service
@Timed
public class BooleanService extends BaseService {

    @Autowired
    private FwdService fwdService;

    /**
     * @param httpServletResponse forwarded from Controller
     * @param conf configuration found by configuration code specified by request parameter(code) in controller
     * @param cookies request cookies
     * @param incomingRequestBody request body
     * @param requestParams request parameters
     * @return  response the content of which if filled by requests specified in requestBody

     * if there are verifiable fields that are not skippable and have endpoint try to return result for  such
     * if such field is configured to stop then stop getting additional responses after getting NOK or null response for first such field
     *
     * if verifiable fields are done and there was no stop then return result from the forwarding service using the same parameters
     */
    public Mono<ResponseEntity> bln(HttpServletResponse httpServletResponse, JsonNode conf,
                                    List<Cookie> cookies,
                                    String incomingRequestBody,
                                    Map<String, String> requestParams) {
        Iterator<Map.Entry<String, JsonNode>> fields = conf.get(VERIFY)
                .fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode verify = entry.getValue();
            if (!skippableConfiguration(verify) && verify.has(ENDPOINT)) {
                CustomClientResponse response = retrieveTimedResponse(httpServletResponse, toJson(""), entry.getKey(), verify, requestParams, incomingRequestBody, cookies);
                // stop kui konfis on see seadistatud
                if ((response == null || !statusOk(response)) && STOP.equals(conf.get(VERIFY).get(RESPONSE)
                        .get(RESP_NOK).asText())) {
                    log.info("Boolean verification for {} returned null or status code not 200 OK.", verify);
                    return response(response != null ? response.getCode() : HttpStatus.FORBIDDEN);
                }
            }
        }
        return fwdService.fwd(httpServletResponse, conf, cookies, incomingRequestBody, requestParams);
    }

}
