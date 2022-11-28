package rig.ruuter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import rig.ruuter.service.BlogicService;
import rig.ruuter.service.BooleanService;
import rig.ruuter.service.FileHandlingService;
import rig.ruuter.service.FwdService;

import java.io.File;
import java.io.IOException;

import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
public class TestBase {

    protected static final String CONFIGURATION = "{\"search\":{\"action_type\":\"FWD\",\"destination\":{\"XM\":{\"endpoint\":\"http://localhost:8080/someurl?query={$payload}&ilm={$requestBodyParam}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"stop\"}}}},\"UPDATE_MY_THINGS\":{\"action_type\":\"BOOLEAN\",\"request_types\":{\"verify\":\"sync\",\"destination\":\"async\"},\"verify\":{\"TURVIS\":{\"endpoint\":\"/turvis/?to_verify={$payload}\",\"ok\":\"proceed\",\"nok\":\"stop\"},\"VOLLI\":{\"endpoint\":\"/volli/?ik={$jwt|ikood}&permission_for=XM&action_type=update\",\"ok\":\"proceed\",\"nok\":\"stop\"}},\"destination\":{\"XM\":{\"endpoint\":\"/xtee_rest_send/?registry=MNT&service_code=888&ikood={$jwt|ikood}&payload={$payload}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"ignore\"}},\"XTEE_LKF\":{\"endpoint\":\"/xtee_rest_send/?registry=LKF&service_code=888&ikood={$jwt|ikood}&payload={$payload}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"ignore\"}}}},\"CCA\":{\"action_type\":\"BLOGIC\",\"request_types\":{\"verify\":\"async\"},\"verify\":{\"TURVIS\":{\"endpoint\":\"/turvis/?to_verify={$payload}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"proceed_with_mock\"}},\"VOLLI\":{\"endpoint\":\"/volli/?ik={$jwt|ikood}&permission_for=XM&action_type=update\",\"response\":{\"ok\":\"proceed\",\"nok\":\"proceed_with_mock\"}}},\"destination\":{\"proceed\":{\"endpoint\":\"/xtee_rest_send/?registry=mnt&service_code=777&ikood={$jwt|ikood}&payload={$payload}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"stop\"}},\"proceed_with_mock\":{\"endpoint\":\"/xtee_rest_send_mock/?registry=mnt&service_code=777&ikood={$jwt|ikood}&payload={$payload}\",\"response\":{\"ok\":\"proceed\",\"nok\":\"proceed\"}}}}}";
    protected static JsonNode destinationNode = toJson("{\"connect_timeout_ms\": \"10000\"}");

    @MockBean
    protected FwdService fwdService;

    @MockBean
    protected BooleanService booleanService;

    @MockBean
    protected BlogicService blogicService;

    @MockBean
    protected FileHandlingService fileService;


    @Value("${ruuter.version}")
    protected String ruuterVersion;

    protected static void deleteDeployConfig() {
        try {
            FileUtils.deleteDirectory(new File("src/test/resources/test_deployed"));
        } catch (IOException e) {
            log.error("Couldn't delete config deploy folder", e);
        }
    }

    @Configuration
    public static class BaseContext {
        @Bean
        InetUtils inetUtils() {
            return new InetUtils(new InetUtilsProperties());
        }

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}
