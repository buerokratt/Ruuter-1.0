package rig.ruuter.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Slf4j
@Data
public class CustomClientResponse {

    private HttpStatus code;
    private String body;
    private Boolean errorReported;
    private HttpHeaders headers;

    public static CustomClientResponse getInstance() {
        return new CustomClientResponse(null, null);
    }

    public CustomClientResponse(HttpStatus code, String body) {
        this(code, body, null);
    }

    public CustomClientResponse(HttpStatus code, String body, HttpHeaders headers){
        this.code = code;
        this.body = body;
        this.headers = headers;
        this.errorReported = false;
    }

    public void errorReported() {
        errorReported = Boolean.TRUE;
    }

    public static Boolean statusOk(CustomClientResponse response) {
        return !response.getCode().isError();
    }
}
