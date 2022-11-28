package rig.ruuter.interceptor;

import brave.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "X-B3-TraceId";

    private final Tracer tracer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = tracer.currentSpan().context().traceIdString();
        if (!StringUtils.isEmpty(traceId)) {
            response.setHeader(HEADER_NAME, tracer.currentSpan().context().traceIdString());
        }
        return true;
    }

}
