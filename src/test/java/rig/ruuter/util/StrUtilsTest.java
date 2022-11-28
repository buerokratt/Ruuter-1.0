package rig.ruuter.util;

import lombok.Data;
import org.junit.jupiter.api.Test;
import rig.ruuter.TestBase;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static rig.ruuter.util.JsonUtils.pojoToJson;
import static rig.ruuter.util.StrUtils.*;

class StrUtilsTest extends TestBase {

    @Test
    void unwrapQuotesTest() {
        String quoted = "\"something\"";
        assertEquals("something", unWrapQuotes(quoted));
    }

    @Test
    void toJsonTest() {
        String wrongJson = "{asdf";
        assertEquals("\"{asdf\"", toJson(wrongJson).toString());
    }

    @Test
    void uriWithPayloadTest() {
        HashMap<String, String> map = new HashMap<>();
        map.put("payload", "value");
        map.put("lang", "et");

        assertEquals("http://localhost:8080/something?payload=value&lang=et",
            findAndReplaceParameters("http://localhost:8080/something?payload={#.payload}&lang={#.lang}", map));
    }

    @Test
    void uriWithPayloadTest2() {
        HashMap<String, String> map = new HashMap<>();
        map.put("payload", "value");
        map.put("lang", "et");
        assertEquals("http://localhost:8080/something?payload=value&payload2=value&lang=et",
            findAndReplaceParameters("http://localhost:8080/something?payload={#.payload}&payload2={#.payload}&lang={#.lang}", pojoToJson(map)));
    }

    @Test
    void uriWithPayloadTest3() {
        HashMap<String, String> map = new HashMap<>();
        map.put("payload", "value");
        map.put("lang", "et");
        assertEquals("http://localhost:8080/something",
            findAndReplaceParameters("http://localhost:8080/something", pojoToJson(map)));
    }

    @Test
    void uriWithPayloadTest4() {
        TestObject testObject = new TestObject();
        assertEquals("http://localhost:8080/something?uuid=foo&no=10",
            findAndReplaceParameters("http://localhost:8080/something?uuid={#.uuid}&no={#.sub#.no}", pojoToJson(testObject)));
    }

    @Data
    static final class TestObject {
        private String uuid = "foo";
        private Sub sub;

        TestObject() {
            this.sub = new Sub();
        }

        @Data
        static final class Sub {
            private int no = 10;
        }

    }

}
