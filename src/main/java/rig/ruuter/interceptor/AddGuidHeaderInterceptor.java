package rig.ruuter.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import rig.commons.handlers.DynamicContent;
import static rig.ruuter.constant.Constant.REQUEST_UID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * this interceptor retrieves a unique ID from dynamic content and adds this to the response headers
 * if the request already has such a header then this is added to response instead
 */
@RequiredArgsConstructor
public class AddGuidHeaderInterceptor extends HandlerInterceptorAdapter {

    private final String GUID_KEY;
    private final DynamicContent mdc;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = request.getHeader(REQUEST_UID);
        if (requestId == null) {
            response.setHeader(REQUEST_UID, mdc.get(GUID_KEY));
        }
        else {
            response.setHeader(REQUEST_UID, requestId);
        }

        return super.preHandle(request, response, handler);
    }

}
