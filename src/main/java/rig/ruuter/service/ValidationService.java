package rig.ruuter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rig.commons.aop.Timed;
import rig.ruuter.json.validation.EmptyList;
import rig.ruuter.json.validation.NumberParamLimits;
import rig.ruuter.json.validation.StringParamLimits;

import java.util.List;

import static rig.ruuter.constant.MatchInputConstants.ALLOWED_ATTRIBUTES;

/**
 * Service to provide input validation
 */
@Slf4j
@Service
@Timed
public class ValidationService {

    /**
     * @param input input to be validated (matched)
     * @param validateAgainst input will be validated(matched) against this
     * @return true/false depending if validation succeeded, for success all attributes in {@code input}
     * must match ones in the {@code validateAgainst}
     */
    public boolean mustMatchAll(JsonNode input, JsonNode validateAgainst) {
        List<String> validateList = extractAttrList(extractNodeToMatch(validateAgainst));
        for (String attr : extractAttrList(extractNodeToMatch(input))) {
            if (!validateList.contains(attr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param input input to be validated (matched)
     * @param validateAgainst input will be validated(matched) against this
     * @return true/false depending if validation succeeded, for success it is enough for any attribute in {@code input}
     * to match with one  in the {@code validateAgainst}
     */
    public boolean mustMatchAny(JsonNode input, JsonNode validateAgainst) {
        List<String> validateList = extractAttrList(extractNodeToMatch(validateAgainst));
        for (String attr : extractAttrList(extractNodeToMatch(input))) {
            if (validateList.contains(attr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param input input to be validated (matched)
     * @param validateAgainst input will be validated(matched) against this
     * @return true/false depending if validation succeeded, for success all attributes in {@code input}
     * must match ones in the {@code validateAgainst}, also their JsonNodeTypes must match
     * (com.fasterxml.jackson.databind.node.JsonNodeType)
     * also the nodes can only contain a preset of allowed attributes (hardcoded constants)
     * @see rig.ruuter.constant.MatchInputConstants#ALLOWED_ATTRIBUTES
     */
    public boolean mustMatchExact(JsonNode input, JsonNode validateAgainst) {
        JsonNode inputNode = extractNodeToMatch(input);
        JsonNode validateNode = extractNodeToMatch(validateAgainst);

        if (inputNode == null || validateNode == null)
            return false;

        if (!inputNode.getNodeType().equals(validateNode.getNodeType()))
            return false;

        List<String> attrList = extractAttrList(inputNode);
        List<String> validateList = extractAttrList(validateNode);

        return attrList.size() == validateList.size() && attrList.equals(validateList);
    }

    private List<String> extractAttrList(JsonNode jsonNode) {
        final List<String> result = Lists.newArrayList();

        if (jsonNode == null || (jsonNode.isArray() && jsonNode.size() == 0))
            return result;

        switch (jsonNode.getNodeType()) {
            case ARRAY:
                ArrayNode values = (ArrayNode) jsonNode;
                values.forEach(node -> result.add(removeNonAlphaNumeric(node.asText())));
                break;
            case STRING:
            case NUMBER:
                result.add(removeNonAlphaNumeric(jsonNode.asText()));
                break;
            default:
                log.warn("Value {} with type {} not supported. Skipping in comparison",
                        jsonNode, jsonNode.getNodeType());
        }

        return result;
    }

    private JsonNode extractNodeToMatch(JsonNode jsonNode) {
        for (String attr : ALLOWED_ATTRIBUTES) {
            if (jsonNode.has(attr)) {
                return jsonNode.get(attr);
            }
        }
        return null;
    }

    private String removeNonAlphaNumeric(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "");
    }

    public boolean validateLength(StringParamLimits params) {
        if (params.getInput() == null) return false;
        int inputLength = params.getInput().trim().length();
        return (params.getMin() == null || inputLength >= params.getMin()) &&
                (params.getMax() == null || inputLength <= params.getMax());
    }

    public boolean validateNumberValue(NumberParamLimits params) {
        return (params.getMin() == null || params.getInput() >= params.getMin()) &&
                (params.getMax() == null || params.getInput() <= params.getMax());
    }

    public boolean validateEmptyList(EmptyList params) {
        return !params.getList().isEmpty();
    }
}
