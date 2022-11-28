package rig.ruuter.validation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;
import rig.ruuter.service.CsrfValidationService;

import javax.servlet.ServletRequest;
import java.io.IOException;

@Aspect
@Configuration
public class CsrfCheckAspect {

    final CsrfValidationService csrfValidationService;

    public CsrfCheckAspect(CsrfValidationService csrfValidationService) {
        this.csrfValidationService = csrfValidationService;
    }

    @Before("@annotation(csrfCheck)")
    public void before(JoinPoint joinPoint, CsrfCheck csrfCheck) throws IOException {
        ServletRequest request = null;
        String requestBody = null;

        if (joinPoint.getArgs().length >= (csrfCheck.requestIdx() + 1))
            request = (ServletRequest) joinPoint.getArgs()[csrfCheck.requestIdx()];

        if (joinPoint.getArgs().length >= (csrfCheck.requestBodyIdx() + 1))
            requestBody = (String) joinPoint.getArgs()[csrfCheck.requestBodyIdx()];

        if (!csrfValidationService.isValid(request, requestBody)) {
            throw new RuntimeException("CSRF validation failed");
        }
    }
}