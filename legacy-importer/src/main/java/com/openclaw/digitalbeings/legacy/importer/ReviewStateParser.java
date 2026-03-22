package com.openclaw.digitalbeings.legacy.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Parser for legacy review-state.json files.
 * Parses review item from the review queue JSON format.
 */
public final class ReviewStateParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ReviewStateParser() {}

    public static Optional<ParsedReviewItem> parse(Path reviewJsonPath) {
        if (!Files.exists(reviewJsonPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(reviewJsonPath);
            JsonNode root = objectMapper.readTree(content);
            String lane = getTextOrDefault(root, "lane", "default");
            String kind = getTextOrDefault(root, "kind", "unknown");
            String proposal = getTextOrDefault(root, "proposal", "");
            String status = getTextOrDefault(root, "status", "draft");
            String actor = getTextOrDefault(root, "actor", "system");
            return Optional.of(new ParsedReviewItem(lane, kind, proposal, status, actor));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : defaultValue;
    }

    public record ParsedReviewItem(String lane, String kind, String proposal, String status, String actor) {
        public boolean isAccepted() {
            return "accepted".equalsIgnoreCase(status);
        }
    }
}
