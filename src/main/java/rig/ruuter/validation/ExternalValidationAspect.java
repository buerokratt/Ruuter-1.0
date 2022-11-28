package rig.ruuter.validation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;
import rig.ruuter.service.ExternalValidationService;

import javax.servlet.ServletRequest;
import java.io.IOException;

@Aspect
@Configuration
public class ExternalValidationAspect {

    final ExternalValidationService externalValidationService;

    public ExternalValidationAspect(ExternalValidationService externalValidationService) {
        this.externalValidationService = externalValidationService;
    }

    @Before("@annotation(externalValidation)")
    public void before(JoinPoint joinPoint, ExternalValidation externalValidation) throws IOException {
        ServletRequest request = null;
        String requestBody = null;

        if (joinPoint.getArgs().length >= (externalValidation.requestIdx() + 1))
            request = (ServletRequest) joinPoint.getArgs()[externalValidation.requestIdx()];

        if (joinPoint.getArgs().length >= (externalValidation.requestBodyIdx() + 1))
            requestBody = (String) joinPoint.getArgs()[externalValidation.requestBodyIdx()];

        if (!externalValidationService.isValid(request, requestBody)) {
            throw new RuntimeException("External validation failed");
        }
    }
}