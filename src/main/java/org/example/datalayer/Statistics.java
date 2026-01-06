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

    /**
     * Сохраняет текущую статистику сессии в statistics.json
     * @param sessionStat объект со статистикой текущей сессии
     * @throws IOException если произошла ошибка при записи файла
     */
    public static void saveCurrentStats(SessionStat sessionStat) throws IOException {
        File statsFile = new File(GameConstants.PathToFiles.STATISTICS_PATH);

        // Создание директорий, если они не существуют
        createParentDirectoryIfNeeded(statsFile);

        // Запись текущей статистики в файл
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(statsFile, sessionStat);
    }

    /**
     * Добавляет статистику текущей сессии в таблицу рекордов (scoreboard.json)
     * @param sessionStat объект со статистикой текущей сессии
     * @throws IOException если произошла ошибка при чтении/записи файла
     */
    public static void updateScoreBoard(SessionStat sessionStat) throws IOException {
        File scoreboardFile = new File(GameConstants.PathToFiles.SCOREBOARD_PATH);

        // Создание файла таблицы рекордов, если он не существует
        createScoreboardFileIfNeeded(scoreboardFile);

        // Чтение существующей таблицы рекордов
        JsonNode allStatJson = objectMapper.readTree(scoreboardFile);
        if (!allStatJson.isObject()) {
            throw new IllegalStateException("scoreboard.json должен быть JSON-объектом");
        }

        ObjectNode rootObject = (ObjectNode) allStatJson;

        // Получение или создание массива со статистикой сессий
        ArrayNode sessionStatsArray = getOrCreateSessionStatsArray(rootObject);

        // Преобразование статистики текущей сессии в JSON
        JsonNode sessionStatJson = objectMapper.valueToTree(sessionStat);

        // Добавление статистики текущей сессии в массив
        sessionStatsArray.add(sessionStatJson);

        // Запись обновленной таблицы рекордов обратно в файл
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(scoreboardFile, rootObject);
    }

    /**
     * Сбрасывает статистику текущей сессии и сохраняет в statistics.json
     * @param sessionStat объект со статистикой текущей сессии
     * @throws IOException если произошла ошибка при записи файла
     */
    public static void resetStatistics(SessionStat sessionStat) throws IOException {
        File statsFile = new File(GameConstants.PathToFiles.STATISTICS_PATH);

        // Создание директорий, если они не существуют
        createParentDirectoryIfNeeded(statsFile);

        // Создание пустого файла статистики, если он не существует
        createEmptyStatsFileIfNeeded(statsFile);

        // Сброс статистики текущей сессии
        sessionStat.reset();

        // Запись сброшенной статистики в файл
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(statsFile, sessionStat);
    }

    /**
     * Создает родительские директории для указанного файла, если они не существуют
     * @param file файл, для которого нужно создать директории
     */
    private static void createParentDirectoryIfNeeded(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    /**
     * Создает файл таблицы рекордов с базовой структурой, если он не существует
     * @param scoreboardFile файл таблицы рекордов
     * @throws IOException если произошла ошибка при записи файла
     */
    private static void createScoreboardFileIfNeeded(File scoreboardFile) throws IOException {
        if (!scoreboardFile.exists()) {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("sessionStats", objectMapper.createArrayNode());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(scoreboardFile, root);
        }
    }

    /**
     * Создает пустой файл статистики, если он не существует
     * @param statsFile файл статистики
     * @throws IOException если произошла ошибка при записи файла
     */
    private static void createEmptyStatsFileIfNeeded(File statsFile) throws IOException {
        if (!statsFile.exists()) {
            ObjectNode emptyStat = objectMapper.createObjectNode();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(statsFile, emptyStat);
        }
    }

    /**
     * Получает или создает массив со статистикой сессий из JSON объекта
     * @param rootObject корневой JSON объект таблицы рекордов
     * @return массив со статистикой сессий
     */
    private static ArrayNode getOrCreateSessionStatsArray(ObjectNode rootObject) {
        JsonNode sessionNode = rootObject.get("sessionStats");
        if (sessionNode != null && sessionNode.isArray()) {
            return (ArrayNode) sessionNode;
        } else {
            ArrayNode arraySessions = objectMapper.createArrayNode();
            rootObject.set("sessionStats", arraySessions);
            return arraySessions;
        }
    }
}