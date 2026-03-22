package com.openclaw.digitalbeings.legacy.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Parser for legacy being.yaml files.
 * Parses the displayName and actor from the being.yaml format.
 */
public final class BeingYamlParser {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private BeingYamlParser() {}

    public static Optional<ParsedBeing> parse(Path beingYamlPath) {
        if (!Files.exists(beingYamlPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(beingYamlPath);
            BeingYaml yaml = yamlMapper.readValue(content, BeingYaml.class);
            return Optional.of(new ParsedBeing(yaml.displayName, yaml.actor));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public record ParsedBeing(String displayName, String actor) {}

    // Internal DTO for YAML parsing
    private static class BeingYaml {
        public String displayName;
        public String actor;
        public BeingYaml() {}
    }
}
