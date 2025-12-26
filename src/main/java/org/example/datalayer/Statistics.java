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

        public static void updateStatistics() throws IOException {
            // Чтение и парсинг scoreboard файла
            JsonNode allStatJson = objectMapper.readTree(new File(GameConstants.PathToFiles.SCOREBOARD_PATH));

            // Получение массива sessionStats
            ArrayNode arraySessions;
            if (allStatJson.has("sessionStats")) {
                arraySessions = (ArrayNode) allStatJson.get("sessionStats");
            } else {
                // Создаем новый массив, если он отсутствует
                arraySessions = objectMapper.createArrayNode();
                ((ObjectNode) allStatJson).set("sessionStats", arraySessions);
            }

            // Чтение и парсинг stats файла
            JsonNode statJson = objectMapper.readTree(new File(GameConstants.PathToFiles.STATISTICS_PATH));

            // Добавление новой статистики в массив
            arraySessions.add(statJson);

            // Запись обновленного JSON обратно в файл с форматированием
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(GameConstants.PathToFiles.SCOREBOARD_PATH), allStatJson);
        }
}
