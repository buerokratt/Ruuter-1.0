package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import rig.commons.aop.Timed;
import rig.ruuter.aop.Tag;
import rig.ruuter.configuration.routing.ConfigurationWrapper;
import rig.ruuter.enums.ActionType;
import rig.ruuter.logging.LoggingContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static rig.ruuter.util.JsonUtils.getActionType;
import static rig.ruuter.util.RestUtils.response;

@Slf4j
@Service
@Timed
public class RouterService {

    private final FwdService fwdService;
    private final BlogicService blogicService;
    private final BooleanService booleanService;

    @Autowired
    public RouterService(FwdService fwdService, BlogicService blogicService, BooleanService booleanService) {
        this.fwdService = fwdService;
        this.blogicService = blogicService;
        this.booleanService = booleanService;
    }

    @rig.ruuter.aop.Timed("router.requests")
    public Mono<ResponseEntity> getResponseEntityMono(ConfigurationWrapper config,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      @Tag String code, String body,
                                                      Map<String, String> requestParams) {

        List<Cookie> availableCookies = new ArrayList<>();
        if (request.getCookies() != null) {
            log.debug("Incoming request - no cookies block");
            availableCookies.addAll(Arrays.asList(request.getCookies()));
        }
        log.info("Incoming request code '{}', params '{}', body '{}' and {} cookies",
                code, requestParams, body, availableCookies.size());

        JsonNode conf = config.find(code);
        if (conf == null) {
            log.warn("NO configuration node for code {}", code);
            return response(HttpStatus.NOT_FOUND);
        }

        ActionType action = getActionType(conf);
        LoggingContext.setConfigContext(code);
        try {
            return resolveAction(response, body, requestParams, availableCookies, conf, action)
                    .doFinally(signalType -> LoggingContext.clearContext());
        } catch (Exception e) {
            LoggingContext.clearContext();
            throw e;
        }
    }

    private Mono<ResponseEntity> resolveAction(HttpServletResponse response, String body, Map<String, String> requestParams, List<Cookie> availableCookies, JsonNode conf, ActionType action) {
        if (action != null) {
            switch (action) {
                case DOWNLOAD_JSON:
                    return blogicService.download(response, conf, availableCookies, body, requestParams, true); // return as json
                case DOWNLOAD:
                    return blogicService.download(response, conf, availableCookies, body, requestParams, false); // return as file download
                case BLOGIC:
                    return blogicService.blogic(response, conf, availableCookies, body, requestParams);
                case BOOLEAN:
                    return booleanService.bln(response, conf, availableCookies, body, requestParams);
                case FWD:
                    return fwdService.fwd(response, conf, availableCookies, body, requestParams);
                default:
                    log.info("Unexpected action type found in configuration.");
                    return response(HttpStatus.NOT_FOUND);
            }
        } else {
            return response(HttpStatus.NOT_FOUND);
        }
    }
}
