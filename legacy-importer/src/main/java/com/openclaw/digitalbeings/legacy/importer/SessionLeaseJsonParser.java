package com.openclaw.digitalbeings.legacy.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Parser for legacy runtime session and lease JSON files.
 * Parses session metadata from JSONL session files.
 */
public final class SessionLeaseJsonParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SessionLeaseJsonParser() {}

    public static Optional<ParsedSession> parse(Path sessionJsonPath) {
        if (!Files.exists(sessionJsonPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(sessionJsonPath);
            JsonNode root = objectMapper.readTree(content);
            String sessionId = getTextOrDefault(root, "session_id", "unknown");
            String hostType = getTextOrDefault(root, "host_type", "unknown");
            String actor = getTextOrDefault(root, "actor", "system");
            String status = getTextOrDefault(root, "status", "active");
            return Optional.of(new ParsedSession(sessionId, hostType, actor, status));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : defaultValue;
    }

    public record ParsedSession(String sessionId, String hostType, String actor, String status) {
        public boolean isActive() {
            return "active".equalsIgnoreCase(status);
        }
    }
}
