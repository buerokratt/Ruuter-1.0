package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import rig.commons.aop.Timed;
import rig.ruuter.enums.Splitter;
import rig.ruuter.util.HelperFunctionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.matches;
import static rig.ruuter.constant.Constant.INCOMING_BODY;
import static rig.ruuter.enums.Splitter.*;
import static rig.ruuter.util.StrUtils.*;

/**
 * Service used for mapping JsonNodes to each other
 */
@Slf4j
@Timed
public class RequestBodyMappingUtils {

    //    finds functions, if functions are recursively inside eachother, will find innermost function
    public static final Pattern FIND_INNERMOST_FUNCTIONS_REGEX = compile(format("\\%s([a-zA-Z0-9]+)\\(((?:`[()]|[^()])*)\\)", FUNCTION_DESIGNATOR.getVal()));
    public static final String REPLACEMENT = "REPLACEMENT_";
    public static final String FILTER_BY_PROPERTY = "filter_by_property";
    public static final String FILTER_PROPERTY_VALUE = "filter_property_value";
    public static final String PROPERTIES = "properties";
    public static final String FROM_PROPERTY = ".from_property";

    private RequestBodyMappingUtils() {
    }

    /**
     * Maps all the mappable data found in the jsonNode and its children - this includes:
     * mapping data from either the incoming body or by config param, mapping functions
     *
     * @param jsonNodeToMap       the jsonNode to map
     * @param bodyParametersNode  bodyParameters from the "sub query" - for mapping data into the node from "sub queries"
     * @param incomingRequestBody body of the incoming request - for mapping data into the node from incoming request
     * @return JsonNode where all the found functions have been executed and the result placed instead of function callouts for the JsonNode itself and its children
     */
    public static JsonNode mapJson(JsonNode jsonNodeToMap, JsonNode bodyParametersNode, JsonNode incomingRequestBody) {
        JsonNode result = toJson("{}");

        jsonNodeToMap.deepCopy().fields().forEachRemaining(field -> {
            if (isHardcodedValue(field.getValue().toString())) {
                ((ObjectNode) result).set(field.getKey(), field.getValue());
            } else if (field.getValue().asText().equals(INCOMING_BODY)) {
                ((ObjectNode) result).set(field.getKey(), incomingRequestBody);
            } else if (hasFunction(field.getValue().asText()) || hasMappingSplitter(field.getValue(), FROM_INCOMING_BODY) || hasMappingSplitter(field.getValue(), FROM_CONFIG_PARAM)) {
                JsonNode mappedNode = mapJsonNodeWithData(field.getValue(), incomingRequestBody, bodyParametersNode);
                if (!isEmpty(mappedNode)) ((ObjectNode) result).set(field.getKey(), mappedNode);
            } else if (field.getValue().getNodeType().equals(JsonNodeType.ARRAY)) {
                JsonNode mappedNode = mapJsonNodeWithArray(field.getValue(), incomingRequestBody, bodyParametersNode);
                if (!isEmpty(mappedNode)) ((ObjectNode) result).set(field.getKey(), mappedNode);
            } else {
                JsonNode childNode = mapJson(field.getValue(), bodyParametersNode, incomingRequestBody);
                if (isEmpty(childNode)) ((ObjectNode) result).set(field.getKey(), field.getValue());
                else ((ObjectNode) result).set(field.getKey(), childNode);
            }
        });

        return result;
    }

    public static JsonNode mapJsonNodeWithData(JsonNode jsonNode, JsonNode incomingRequestBody, JsonNode bodyParametersNode) {
        Map<String, String> mappingStore = new HashMap<>();
        String jsonAsString = jsonNode.asText();
        Matcher matcher = FIND_INNERMOST_FUNCTIONS_REGEX.matcher(jsonAsString);
        String uid = UUID.randomUUID().toString();
        int functionCounter = 0;

        while (matcher.find()) {
            functionCounter += 1;
            List<Object> functionInputs = getFunctionInput(matcher.group(2), mappingStore, incomingRequestBody, bodyParametersNode);
            String fnResult = HelperFunctionUtils.callFunction(matcher.group(1), functionInputs).toString();
            String key = getKey(uid, functionCounter);
            mappingStore.put(key, fnResult);
            jsonAsString = jsonAsString.replace(matcher.group(0), key);
            matcher = FIND_INNERMOST_FUNCTIONS_REGEX.matcher(jsonAsString);
        }
        uid = UUID.randomUUID().toString();
        for (int i = 0; i < jsonNode.toString().chars().filter(num -> num == '{').count(); i++) {
            String firstDataMapping = null;
            String extract = null;
            if (jsonAsString.contains("{" + FROM_CONFIG_PARAM.getVal())) {
                firstDataMapping = getFirstDataMapping(jsonAsString, FROM_CONFIG_PARAM);
                extract = getDataFromNode(firstDataMapping, bodyParametersNode, FROM_CONFIG_PARAM);
            }
            if (jsonAsString.contains("{" + FROM_INCOMING_BODY.getVal())) {
                firstDataMapping = getFirstDataMapping(jsonAsString, FROM_INCOMING_BODY);
                extract = getDataFromNode(firstDataMapping, incomingRequestBody, FROM_INCOMING_BODY);
            }
            if (extract != null) {
                String key = getKey(uid, i);
                mappingStore.put(key, extract);
                jsonAsString = jsonAsString.replace(firstDataMapping, key);
            }
        }
        for (Map.Entry<String, String> entry : mappingStore.entrySet()) {
            jsonAsString = jsonAsString.replace(entry.getKey(), entry.getValue());
        }

        if (Objects.equals(jsonAsString, jsonNode.asText())) return null;
        if (isWrappedBy(jsonAsString, "{", "}") || (isWrappedBy(jsonAsString, "[", "]"))) {
            return toJson(jsonAsString);
        }
        return toJson(wrapQuotes(jsonAsString));
    }

    private static String getKey(String uid, int i) {
        return String.format("%s_%s_%d", REPLACEMENT, uid, i);
    }

    private static List<Object> getFunctionInput(String string, Map<String, String> previousResultStore, JsonNode incomingBody, JsonNode bodyParametersNode) {
        if (string.equals("")) return new ArrayList<>();
        return Arrays.asList(Arrays.stream(string.split("\\$,".replace("$", FROM_CONFIG_PARAM.getVal())))
            .map(s -> previousResultStore.getOrDefault(s, s))
            .map(s -> {
                String extractedValue = s;
                if (hasMappingSplitter(toJson(s), FROM_CONFIG_PARAM))
                    extractedValue = unWrapQuotes(extract(bodyParametersNode, s, FROM_CONFIG_PARAM).toString());
                if (hasMappingSplitter(toJson(s), FROM_INCOMING_BODY)) extractedValue = unWrapQuotes(extract(incomingBody, s, FROM_INCOMING_BODY).toString());
                return extractedValue;
            })
            .toArray());
    }

    public static JsonNode mapJsonNodeWithArray(JsonNode arrayToMap, JsonNode incomingRequestBody, JsonNode bodyParametersNode) {
        ArrayNode resultArray;
        String incBodyPropDesignator = FROM_INCOMING_BODY.getVal() + FROM_PROPERTY;
        String confParamPropDesignator = FROM_CONFIG_PARAM.getVal() + FROM_PROPERTY;


        if (arrayToMap.get(0) != null && arrayToMap.get(0).has(incBodyPropDesignator)) {
            resultArray = mapArrayWithPropertyDesignator(arrayToMap, incomingRequestBody, FROM_INCOMING_BODY, incBodyPropDesignator);
        } else if (arrayToMap.get(0) != null && arrayToMap.get(0).has(confParamPropDesignator)) {
            resultArray = mapArrayWithPropertyDesignator(arrayToMap, bodyParametersNode, FROM_CONFIG_PARAM, confParamPropDesignator);
        } else resultArray = mapArrayWithoutPropertyDesignator(arrayToMap, bodyParametersNode, incomingRequestBody);

        return resultArray;
    }

    private static ArrayNode mapArrayWithPropertyDesignator(JsonNode arrayToMap, JsonNode incomingRequestBody, Splitter splitter, String propDesignator) {
        ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();

        JsonNode firstNode = arrayToMap.get(0).deepCopy();
        JsonNode allNodes = extract(incomingRequestBody, firstNode.get(propDesignator).toString(), splitter);
        if (allNodes == null) {
            log.info("Cannot extract groups. Grouping from null or not an array json.");
            return null;
        }
        if (firstNode.has(FILTER_BY_PROPERTY) && firstNode.has(FILTER_PROPERTY_VALUE)) {
            String filterProp = unWrapQuotesAndBraces(firstNode.get(FILTER_BY_PROPERTY).textValue());
            String filterPropValue = unWrapQuotesAndBraces(firstNode.get(FILTER_PROPERTY_VALUE).textValue());
            for (JsonNode node : allNodes) {
                if (!node.has(filterProp) || !matches(filterPropValue, unWrapQuotesAndBraces(node.get(filterProp).textValue()))) continue;
                resultArray.add(mapJson(firstNode.get(PROPERTIES), node, incomingRequestBody));
            }
        } else {
            for (JsonNode node : allNodes) resultArray.add(mapJson(firstNode.get(PROPERTIES), node, node));
        }

        return resultArray;
    }

    private static ArrayNode mapArrayWithoutPropertyDesignator(JsonNode arrayToMap, JsonNode bodyParametersNode, JsonNode incomingRequestBody) {
        ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();

        for (JsonNode nodeInArray : arrayToMap) {
            if (!nodeInArray.isValueNode()) {
                resultArray.add(mapJson(nodeInArray, bodyParametersNode, incomingRequestBody));
                continue;
            }
            if (hasMappingSplitter(nodeInArray, FROM_CONFIG_PARAM)) resultArray.add(extract(bodyParametersNode, nodeInArray.toString(), FROM_CONFIG_PARAM));
            else if (hasMappingSplitter(nodeInArray, FROM_INCOMING_BODY))
                resultArray.add(extract(incomingRequestBody, nodeInArray.toString(), FROM_INCOMING_BODY));
            else resultArray.add(nodeInArray);
        }

        return resultArray;
    }

    private static boolean hasFunction(String string) {
        return string != null
            && string.contains(FUNCTION_DESIGNATOR.getVal())
            && string.contains("(")
            && string.contains(")");
    }

    private static boolean hasMappingSplitter(JsonNode jsonNode, Splitter mappingSplitter) {
        return jsonNode.getNodeType().equals(JsonNodeType.STRING)
            && jsonNode.asText().contains("{" + mappingSplitter.getVal())
            && jsonNode.asText().contains("}");
    }

    private static boolean isHardcodedValue(String string) {
        return string != null
            && !hasFunction(string)
            && !string.contains(FROM_INCOMING_BODY.getVal())
            && !string.contains(FROM_CONFIG_PARAM.getVal());
    }

    private static boolean isEmpty(JsonNode jsonNode) {
        return jsonNode == null || jsonNode.toString().equals("");
    }

    private static String getExtractionPath(String string) {
        return string.substring(string.indexOf("{") + 1, string.indexOf("}") + 1);
    }

    public static String getFirstDataMapping(String string, Splitter splitter) {
        return string.substring(string.indexOf("{" + splitter.getVal()), string.indexOf("}") + 1);
    }

    public static String getDataFromNode(String dataToGet, JsonNode nodeToGetDataFrom, Splitter splitter) {
        if (nodeToGetDataFrom == null) return "";
        String toGet = unWrapQuotesAndBraces(getExtractionPath(dataToGet));

        JsonNode nodeToExtractDataFrom = nodeToGetDataFrom.deepCopy();
        String splitRegex = "\\$\\.".replace("$", splitter.getVal());
        String[] mappings = toGet.substring(2).split(splitRegex);
        for (String m : mappings) {
            boolean isMandatory = m.startsWith("!");
            m = m.replaceFirst("!", "");
            if (m.endsWith("]")) {
                nodeToExtractDataFrom = nodeToExtractDataFrom
                    .get(m.substring(0, m.lastIndexOf('[')))
                    .get(Integer.parseInt(m.substring(m.lastIndexOf('[') + 1, m.length() - 1)));
                continue;
            }
            if (nodeToExtractDataFrom == null) return null;
            nodeToExtractDataFrom = nodeToExtractDataFrom.get(m);
            if (isMandatory && (nodeToExtractDataFrom == null || StringUtils.isBlank(nodeToExtractDataFrom.asText()))) {
                throw new IllegalArgumentException("Required field is missing");
            }
        }

        if (nodeToExtractDataFrom == null) return null;
        if (isWrappedBy(nodeToExtractDataFrom.toString(), "{", "}")) return nodeToExtractDataFrom.toString();
        if (isWrappedBy(nodeToExtractDataFrom.toString(), "[", "]")) return nodeToExtractDataFrom.toString();
        return nodeToExtractDataFrom.asText().replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @SuppressWarnings("squid:S2259")
    public static JsonNode extract(JsonNode incomingBody, String extract, Splitter splitter) {
        if (incomingBody == null) return toJson("");
        String toExtract = unWrapQuotesAndBraces(getExtractionPath(extract));
        if (!toExtract.startsWith(splitter.getVal() + ".")) {
            if (toExtract.equals("$incoming_request_body") && splitter.getVal().equals("#")) {
                return incomingBody;
            }
            return toJson("{" + toExtract + "}");
        }
        JsonNode extracted = incomingBody.deepCopy();
        String splitRegex = "\\$\\.".replace("$", splitter.getVal());
        String[] extracts = toExtract.substring(2).split(splitRegex); // split on $.
        for (String extr : extracts) {
            boolean isMandatory = extr.startsWith("!");
            extr = extr.replaceFirst("!", "");
            if (extr.endsWith("]")) {
                extracted = extracted.get(extr.substring(0, extr.lastIndexOf('[')))
                    .get(Integer.parseInt(extr.substring(extr.lastIndexOf('[') + 1, extr.length() - 1)));
            } else {
                if (extracted != null) {
                    extracted = extracted.get(extr);
                    if (isMandatory && (extracted == null || StringUtils.isBlank(extracted.asText()))) {
                        throw new RuntimeException("Required field is missing");
                    }
                } else {
                    return null;
                }
            }
        }

        if (extracted != null && extracted.isValueNode() && !toExtract.equals(unWrapQuotesAndBraces(extract))) {
            return toJson(extract.substring(0, extract.indexOf("{"))
                .concat(extracted.asText())
                .concat(extract.substring(extract.indexOf("}") + 1)));
        } else {
            return extracted;
        }
    }
}
