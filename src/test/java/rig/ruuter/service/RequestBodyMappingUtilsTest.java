package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import rig.ruuter.TestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static rig.ruuter.util.StrUtils.toJson;

@Slf4j
class RequestBodyMappingUtilsTest extends TestBase {

    private static final JsonNode postBodyStruct = toJson("{\"parameters\":{\"this hardcoded\":{\"hardcoded\":\"kek\",\"kekarayy\":[\"lul\"]},\"callerPersonalCode\":\"{$.data$.personalCode}\",\"paramFromConfShouldBeNull\":\"{$.null$.foobar}\",\"document_id\":\"{#.document_id}\",\"arrayFromINcomingBody\":[{\"#.from_property\":\"{#.recipient_list}\",\"properties\":{\"code\":\"{#.code}\",\"seepeaks olema null\":\"{#.blaah}\"}}],\"confParamsList\":[{\"$.from_property\":\"{$.kek$.recipient_list}\",\"properties\":{\"code\":\"{$.code}\",\"seepeaks olema null\":\"{$.blaah2}\"}}]},\"holeBody\":\"{$incoming_request_body}\"}");
    private static final JsonNode postBodyStructWithAppend = toJson("{\"parameters\":{\"this hardcoded\":{\"hardcoded\":\"kek\",\"kekarayy\":[\"lul\"]},\"callerPersonalCode\":\"bar-{$.data$.personalCode}..baz-{$.data$.personalCode}\",\"paramFromConfShouldBeNull\":\"{$.null$.foobar}\",\"document_id\":\"{#.document_id}\",\"arrayFromINcomingBody\":[{\"#.from_property\":\"{#.recipient_list}\",\"properties\":{\"code\":\"{#.code}\",\"seepeaks olema null\":\"{#.blaah}\"}}],\"confParamsList\":[{\"$.from_property\":\"{$.kek$.recipient_list}\",\"properties\":{\"code\":\"{$.code}\",\"seepeaks olema null\":\"{$.blaah2}\"}}]},\"holeBody\":\"{$incoming_request_body}\"}");
    private static final JsonNode incomingBody = toJson("{\"document_id\":\"testdokid\",\"recipient_list\":[{\"code\":\"testCode1\"},{\"code\":\"testCode2\"}]}");
    private static final JsonNode confParamsBody = toJson("{\"data\":{\"personalCode\":\"TESTCODE\"},\"kek\":{\"recipient_list\":[{\"code\":\"ezhiArrayCode\"}]}}");

    private static final JsonNode postBodyStructWithNull = toJson("{\"option\":\"{$.option}\",\"optionNULL\":\"{$.option34}\",\"option1\":\"{$.option1}\",\"option2\":{\"property\":\"{#.option2#.property}\",\"propertyNULL\":\"{#.option2#.property23}\"},\"option3\":{\"property\":\"{#.option3#.property}\",\"property2\":\"{#.option3#.property2}\",\"property3\":\"{#.option3#.property3}\",\"property4NULL\":\"{#.option3#.property4}\"}}");
    private static final JsonNode bodyWithNull = toJson("{\"option\":\"option\",\"option1\":\"option1\",\"option2\":{\"property\":\"property\"},\"option3\":{\"property\":\"property\",\"property2\":\"property\",\"property3\":\"property\"}}");

    @Test
    void fullChangeTest() {
        JsonNode result = RequestBodyMappingUtils.mapJson(postBodyStruct, confParamsBody, incomingBody);

        assertEquals("{\"parameters\":{\"this hardcoded\":{\"hardcoded\":\"kek\",\"kekarayy\":[\"lul\"]},\"callerPersonalCode\":\"TESTCODE\",\"document_id\":\"testdokid\",\"arrayFromINcomingBody\":[{\"code\":\"testCode1\"},{\"code\":\"testCode2\"}],\"confParamsList\":[{\"code\":\"ezhiArrayCode\"}]},\"holeBody\":{\"document_id\":\"testdokid\",\"recipient_list\":[{\"code\":\"testCode1\"},{\"code\":\"testCode2\"}]}}"
            , result.toString());
    }

    @Test
    void concatinationTest() {
        JsonNode result = RequestBodyMappingUtils.mapJson(postBodyStructWithAppend, confParamsBody, incomingBody);

        assertEquals("{\"parameters\":{\"this hardcoded\":{\"hardcoded\":\"kek\",\"kekarayy\":[\"lul\"]},\"callerPersonalCode\":\"bar-TESTCODE..baz-TESTCODE\",\"document_id\":\"testdokid\",\"arrayFromINcomingBody\":[{\"code\":\"testCode1\"},{\"code\":\"testCode2\"}],\"confParamsList\":[{\"code\":\"ezhiArrayCode\"}]},\"holeBody\":{\"document_id\":\"testdokid\",\"recipient_list\":[{\"code\":\"testCode1\"},{\"code\":\"testCode2\"}]}}"
            , result.toString());
    }

    @Test
    void nullPropertyRemoveTest() {
        JsonNode result = RequestBodyMappingUtils.mapJson(postBodyStructWithNull, bodyWithNull, bodyWithNull);
        assertEquals("{\"option\":\"option\",\"option1\":\"option1\",\"option2\":{\"property\":\"property\"},\"option3\":{\"property\":\"property\",\"property2\":\"property\",\"property3\":\"property\"}}"
            , result.toString());
    }

    @Test
    void valueArrayFromIncomingBodyTest() {
        JsonNode result = RequestBodyMappingUtils.mapJson(toJson("{\"parameters\":{\"personalCode\":\"{#.code}\",\"code\":[\"{#.code}\"]}}"),
            null, toJson("{\"code\":\"codeValue\"}"));
        assertEquals("{\"parameters\":{\"personalCode\":\"codeValue\",\"code\":[\"codeValue\"]}}"
            , result.toString());
    }

    @Test
    void valueArrayFromIncomingParamTest() {
        JsonNode result = RequestBodyMappingUtils.mapJson(toJson("{\"parameters\":{\"personalCode\":\"{$.code}\",\"code\":[\"{$.code}\"]}}"),
            toJson("{\"code\":\"codeValue\"}"), null);
        assertEquals("{\"parameters\":{\"personalCode\":\"codeValue\",\"code\":[\"codeValue\"]}}", result.toString());
    }

}
