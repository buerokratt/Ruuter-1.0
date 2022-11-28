package rig.ruuter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;

@Slf4j
@Component
@Order(1)
public class RequestSizeFilter implements Filter {

    @Value("${maxRequestSizeBytes:1000000000}")
    private Long maxRequestSizeBytes;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final long size = request.getContentLengthLong();

        if (size > maxRequestSizeBytes) {
            ((HttpServletResponse) response).sendError(SC_REQUEST_ENTITY_TOO_LARGE);
        } else {
            chain.doFilter(request, response);
        }
    }
}
