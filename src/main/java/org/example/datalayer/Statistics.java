package org.example.datalayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.config.GameConstants;

import java.io.File;
import java.io.IOException;

public class Statistics {
        private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void updateScoreBoard() throws IOException {
        File scoreboardFile = new File(GameConstants.PathToFiles.SCOREBOARD_PATH);
        File statsFile = new File(GameConstants.PathToFiles.STATISTICS_PATH);

        // Создание scoreboard.json если его нет
        if (!scoreboardFile.exists()) {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("sessionStats", objectMapper.createArrayNode());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(scoreboardFile, root);
        }

        // Создание statistics.json если его нет
        if (!statsFile.exists()) {
            ObjectNode emptyStat = objectMapper.createObjectNode();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(statsFile, emptyStat);
        }

        // Чтение scoreboard
        JsonNode allStatJson = objectMapper.readTree(scoreboardFile);
        if (!allStatJson.isObject()) {
            throw new IllegalStateException("scoreboard.json должен быть JSON-объектом");
        }

        ObjectNode rootObject = (ObjectNode) allStatJson;

        // Получение или создание sessionStats
        ArrayNode arraySessions;
        JsonNode sessionNode = rootObject.get("sessionStats");
        if (sessionNode != null && sessionNode.isArray()) {
            arraySessions = (ArrayNode) sessionNode;
        } else {
            arraySessions = objectMapper.createArrayNode();
            rootObject.set("sessionStats", arraySessions);
        }

        // Чтение statistics
        JsonNode statJson = objectMapper.readTree(statsFile);

        // Добавление статистики
        arraySessions.add(statJson);

        // Запись обратно
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(scoreboardFile, rootObject);

    }

    public static void resetStatistics() throws IOException {
        File statsFile = new File(GameConstants.PathToFiles.STATISTICS_PATH);

        File parentDir = statsFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (!statsFile.exists()) {
            ObjectNode emptyStat = objectMapper.createObjectNode();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(statsFile, emptyStat);
        }
        SessionStat zeroStat = new SessionStat(
                0,  // treasures
                1,  // level
                0,  // enemies
                0,  // food
                0,  // elixirs
                0,  // scrolls
                0,  // attacks
                0,  // missed
                0   // moves
        );

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(statsFile, zeroStat);
    }
}
