package rig.ruuter.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import rig.ruuter.configuration.routing.RoutingConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static rig.ruuter.constant.Constant.REQUESTED_METHOD_TYPE;

/**
 * This request interceptor class, if enabled, will check if service endpoint has configuration and if request method
 * for the particular request is either allowed or not. If request method is allowed request will proceed and if not
 * request is being cut.
 */
@Slf4j
public class RequestMethodValidatorInterceptor extends HandlerInterceptorAdapter {

    private final List<String> allowedRequestedMethodTypes;

    private final String defaultRequestedMethodType;

    private final Integer requestedMethodTypeErrorHttpResponseCode;

    private final RoutingConfiguration routingConfiguration;

    public RequestMethodValidatorInterceptor(RoutingConfiguration routingConfiguration,
                                             List<String> allowedRequestedMethodTypes,
                                             String defaultRequestedMethodType,
                                             Integer requestedMethodTypeErrorHttpResponseCode) {
        this.routingConfiguration = routingConfiguration;
        this.allowedRequestedMethodTypes = allowedRequestedMethodTypes;
        this.defaultRequestedMethodType = defaultRequestedMethodType;
        this.requestedMethodTypeErrorHttpResponseCode = requestedMethodTypeErrorHttpResponseCode;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String code = getCode(request.getRequestURI());
        JsonNode conf = null;
        if (code != null)
            conf = this.routingConfiguration.find(code);

        if (conf != null && !requestMethodIsValid(request, conf)) {
            log.error("Request {} method {} is not configured to be allowed",
                    request.getRequestURL(), request.getMethod());
            response.setStatus(requestedMethodTypeErrorHttpResponseCode);
            return false;
        }
        return super.preHandle(request, response, handler);
    }

    private String getCode(String uri) {
        String[] uriArray = uri.replace("/", " ").trim().split(" ");
        return uriArray.length > 0 ? uriArray[uriArray.length - 1] : null;
    }

    private boolean requestMethodIsValid(HttpServletRequest request,
                                         JsonNode conf) {
        String requestMethod = request.getMethod().trim().toUpperCase();

        if (!allowedRequestedMethodTypes.contains(requestMethod))
            return false;

        if (!conf.has(REQUESTED_METHOD_TYPE) && !requestMethod.equals(defaultRequestedMethodType.trim().toUpperCase()))
            return false;

        return conf.has(REQUESTED_METHOD_TYPE) && requestMethod.equals(conf.get(REQUESTED_METHOD_TYPE).asText());
    }

}
