package rig.ruuter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static rig.ruuter.enums.Splitter.FROM_INCOMING_BODY;
import static rig.ruuter.service.RequestBodyMappingUtils.extract;
import static rig.ruuter.util.JsonUtils.pojoToJson;

@Slf4j
public class StrUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StrUtils() {
    }

    public static String wrapQuotes(String toWrap) {
        return "\"" + toWrap + "\"";
    }

    public static String unWrapQuotes(String toUnwrap) {
        if (toUnwrap == null) {
            return null;
        }

        if (toUnwrap.startsWith("\"")) {
            toUnwrap = toUnwrap.substring(1);
        }

        if (toUnwrap.endsWith("\"")) {
            toUnwrap = toUnwrap.substring(0, toUnwrap.length() - 1);
        }

        return toUnwrap;
    }

    public static JsonNode toJson(String toConvert) {
        JsonNode result = null;

        if (toConvert == null) {
            return null;
        }

        try {
            result = MAPPER.readTree(toConvert);
        } catch (IOException e) {
            try {
                result = MAPPER.readTree(wrapQuotes(toConvert));
            } catch (IOException ef) {
                log.warn("Tried to convert an invalid string to json {}",
                    toConvert);
            }
        }

        return result != null ? result : new TextNode(toConvert);
    }

    public static String toJsonString(Map<String, String> toConvert) {
        if (toConvert == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(toConvert);
        } catch (IOException e) {
            log.warn("Tried to convert an invalid Map to json string {}", toConvert);
        }

        return null;
    }

    public static String findAndReplaceParameters(String uri, Object object) {
        return findAndReplaceParameters(uri, pojoToJson(object));
    }

    public static String findAndReplaceParameters(String uri, JsonNode jsonNode) {
        Map<String, String> extractsMap = Maps.newHashMap();
        String uriWithParams = uri;
        String path;

        if (!uri.contains("{") || !uri.contains("}") || !uri.contains(FROM_INCOMING_BODY.getVal()))
            return uri;

        int start = getParamIdxStart(uri);
        int end = getParamIdxEnd(uri);

        while (start >= 0 && end >= 0) {
            path = uri.substring(start, end + 1);
            JsonNode extract = extract(jsonNode, path, FROM_INCOMING_BODY);
            extractsMap.put(path, extract == null ? path : extract.asText());
            uri = uri.replace(path, "");
            start = getParamIdxStart(uri);
            end = getParamIdxEnd(uri);
        }

        for (String key : extractsMap.keySet()) {
            if (!key.equals(extractsMap.get(key))) {
                try {
                    uriWithParams = uriWithParams.replaceAll(Pattern.quote(key), URLEncoder.encode(extractsMap.get(key), StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException e) {
                    log.info("Unable to encode URI part", e);
                    return uri;
                }
            }
        }

        return uriWithParams;
    }

    public static String unWrapQuotesAndBraces(String toUnwrap) {
        if (toUnwrap == null) {
            return null;
        }

        toUnwrap = unWrapQuotes(toUnwrap);

        if (toUnwrap.startsWith("{")) {
            toUnwrap = toUnwrap.substring(1);
        }

        if (toUnwrap.endsWith("}")) {
            toUnwrap = toUnwrap.substring(0, toUnwrap.length() - 1);
        }

        return toUnwrap;
    }

    private static int getParamIdxStart(String uri) {
        return uri.indexOf("{".concat(FROM_INCOMING_BODY.getVal()).concat("."));
    }

    private static int getParamIdxEnd(String uri) {
        return uri.indexOf("}");
    }

    public static String md5(Object convertableToStr) {
        return DigestUtils.md5Hex(convertableToStr + "");
    }

    public static boolean isWrappedBy(String jsonAsString, String s, String s2) {
        return jsonAsString.startsWith(s) && jsonAsString.endsWith(s2);
    }

}
