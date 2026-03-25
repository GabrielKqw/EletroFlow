package com.eletroflow.discord.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationLoader {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public BotConfiguration loadBotConfiguration() throws IOException {
        return objectMapper.readValue(resolve("bot-config.yml").toFile(), BotConfiguration.class);
    }

    public PlanCatalog loadPlanCatalog() throws IOException {
        return objectMapper.readValue(resolve("vip-plans.yml").toFile(), PlanCatalog.class);
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    public Path stateFile() {
        return resolve("discord-state.yml");
    }

    private Path resolve(String fileName) {
        Path external = Path.of("config", fileName);
        if (Files.exists(external)) {
            return external;
        }
        try (InputStream inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration file " + fileName);
            }
            Files.createDirectories(external.getParent());
            Files.copy(inputStream, external);
            return external;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve configuration " + fileName, exception);
        }
    }
}
