package rig.ruuter.logging;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
class PayloadLoggingValidator {
    private PayloadLoggingConfiguration configuration;
    private List<RulePattern> rulePatterns = List.of();

    PayloadLoggingValidator(PayloadLoggingConfiguration configuration) {
        this.configuration = configuration;
        List<PayloadLoggingRule> rules = configuration
                .getRules();
        if (rules != null) {
            this.rulePatterns = rules
                    .stream()
                    .map(RulePattern::new)
                    .collect(Collectors.toList());
        }
    }

    boolean isLoggingEnabled(String configurationCode, String destination, PayloadType type) {
        if (!configuration.isEnabled()) {
            return false;
        }
        return this.rulePatterns.stream()
                .filter(ruleMatcher -> ruleMatcher.pattern.matches(configurationCode, destination, type))
                .findFirst()
                .map(RulePattern::isEnablingRule)
                .orElse(true);
    }

    private static class RulePattern {
        PayloadLoggingRule rule;
        PayloadLoggingPattern pattern;

        RulePattern(PayloadLoggingRule rule) {
            this.rule = rule;
            this.pattern = PayloadLoggingPattern.fromString(rule.getPattern());
        }

        boolean isEnablingRule() {
            return rule.getEnable() != null;
        }
    }
}
