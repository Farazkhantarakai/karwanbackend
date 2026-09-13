package com.aiecomm.camp.modules.theme.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Structural validation of Theme JSON configuration submitted by the builder.
 * The browser is never trusted — this is the authoritative security boundary
 * for shape/size (see architecture rule on backend validation). It does not
 * validate section/setting types against an allow-list of known frontend
 * section types, since that registry lives client-side only.
 */
@Component
public class ThemeConfigurationValidator {

    private static final int MAX_CONFIGURATION_LENGTH = 500_000;

    public void validate(JsonNode configuration) {
        if (configuration == null || configuration.isNull()) {
            throw new IllegalArgumentException("Theme configuration is required");
        }
        if (configuration.toString().length() > MAX_CONFIGURATION_LENGTH) {
            throw new IllegalArgumentException("Theme configuration exceeds the maximum allowed size");
        }
        if (!configuration.hasNonNull("version") || !configuration.get("version").isNumber()) {
            throw new IllegalArgumentException("Theme configuration must include a numeric 'version'");
        }

        requireObject(configuration, "globalSettings", "");
        requireObject(configuration, "sectionGroups", "");
        requireObject(configuration, "templates", "");

        JsonNode sectionGroups = configuration.get("sectionGroups");
        validateSectionGroup(sectionGroups, "header");
        validateSectionGroup(sectionGroups, "footer");

        JsonNode templates = configuration.get("templates");
        templates.fieldNames().forEachRemaining(templateType ->
                validateSectionsAndOrder(templates.get(templateType), "templates." + templateType));
    }

    private void requireObject(JsonNode parent, String field, String path) {
        if (!parent.hasNonNull(field) || !parent.get(field).isObject()) {
            throw new IllegalArgumentException(path + field + " must be an object");
        }
    }

    private void validateSectionGroup(JsonNode sectionGroups, String groupName) {
        if (!sectionGroups.hasNonNull(groupName) || !sectionGroups.get(groupName).isObject()) {
            throw new IllegalArgumentException("sectionGroups." + groupName + " must be an object");
        }
        validateSectionsAndOrder(sectionGroups.get(groupName), "sectionGroups." + groupName);
    }

    private void validateSectionsAndOrder(JsonNode node, String path) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        if (!node.hasNonNull("sections") || !node.get("sections").isObject()) {
            throw new IllegalArgumentException(path + ".sections must be an object");
        }
        if (!node.hasNonNull("order") || !node.get("order").isArray()) {
            throw new IllegalArgumentException(path + ".order must be an array");
        }

        JsonNode sections = node.get("sections");
        Set<String> sectionIds = new HashSet<>();
        sections.fieldNames().forEachRemaining(sectionIds::add);

        Set<String> orderIds = new HashSet<>();
        for (JsonNode idNode : node.get("order")) {
            if (!idNode.isTextual()) {
                throw new IllegalArgumentException(path + ".order must contain only strings");
            }
            orderIds.add(idNode.asText());
        }

        if (!sectionIds.equals(orderIds)) {
            throw new IllegalArgumentException(path + ".order must reference exactly the ids present in " + path + ".sections");
        }

        for (String sectionId : sectionIds) {
            JsonNode section = sections.get(sectionId);
            if (!section.hasNonNull("type") || !section.get("type").isTextual()) {
                throw new IllegalArgumentException(path + ".sections." + sectionId + " must include a string 'type'");
            }
            if (!section.hasNonNull("settings") || !section.get("settings").isObject()) {
                throw new IllegalArgumentException(path + ".sections." + sectionId + " must include an object 'settings'");
            }
        }
    }
}
