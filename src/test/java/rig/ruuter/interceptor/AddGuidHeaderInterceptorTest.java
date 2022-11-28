package rig.ruuter.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import rig.commons.handlers.DynamicContent;
import rig.commons.handlers.LogHandler;
import rig.commons.handlers.MDCwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static rig.ruuter.constant.Constant.REQUEST_UID;
import static rig.ruuter.constant.Constant.REQ_GUID;


class AddGuidHeaderInterceptorTest {

    private LogHandler handler;
    private AddGuidHeaderInterceptor headerInterceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private static final Object object = new Object();
    private static final String GUID = REQ_GUID;
    private static final DynamicContent mdc = new MDCwrapper();

    @BeforeEach
    public void setUp() {
        handler = LogHandler.builder().build();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        headerInterceptor = new AddGuidHeaderInterceptor(GUID, mdc);
    }

    @Test
    void testThatGuidAddedAsHeader() throws Exception {
        handler.preHandle(request, response, object);
        headerInterceptor.preHandle(request, response, object);
        assertEquals(MDC.get(GUID), response.getHeader(REQUEST_UID));
    }

    @Test
    void testThatRequestHeaderForwardedToResponse() throws Exception {
        request.addHeader(REQUEST_UID, "123");
        handler.preHandle(request, response, object);
        headerInterceptor.preHandle(request, response, object);
        assertEquals("123", response.getHeader(REQUEST_UID));
    }
}
