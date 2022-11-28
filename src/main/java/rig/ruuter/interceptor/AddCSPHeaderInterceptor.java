package rig.ruuter.interceptor;

import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RequiredArgsConstructor
@Component
public class AddCSPHeaderInterceptor extends HandlerInterceptorAdapter {

    @Value("${headers.contentSecurityPolicy:}")
    private String contentSecurityPolicy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, contentSecurityPolicy);
        return super.preHandle(request, response, handler);
    }
}
