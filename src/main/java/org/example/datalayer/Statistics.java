package org.example.datalayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.config.GameConstants;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;
import org.example.domain.entity.Player;

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
     * @param gameSession игровая сессия (для получения актуальных данных)
     * @param sessionStat объект со статистикой текущей сессии
     * @throws IOException если произошла ошибка при чтении/записи файла
     */
    public static void updateScoreBoard(GameSession gameSession, SessionStat sessionStat) throws IOException {
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

        // Создаем новый JSON объект для текущей статистики
        // Берем данные из gameSession, если доступны, иначе из sessionStat
        ObjectNode sessionStatJson = createSessionStatJson(gameSession, sessionStat);

        // Добавление статистики текущей сессии в массив
        sessionStatsArray.add(sessionStatJson);

        // Запись обновленной таблицы рекордов обратно в файл
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(scoreboardFile, rootObject);
    }

    /**
     * Создает JSON объект из SessionStat и GameSession
     * Использует GameSession для получения актуальных данных о предметах и сокровищах
     */
    private static ObjectNode createSessionStatJson(GameSession gameSession, SessionStat sessionStat) {
        ObjectNode json = objectMapper.createObjectNode();

        // Для treasures берем из gameSession (актуальные сокровища в инвентаре)
        int treasuresValue = 0;

        // Для enemies, attacks, missed, moves - используем sessionStat
        json.put("levelNum", sessionStat.getLevelNum());
        json.put("enemies", sessionStat.getEnemies());
        json.put("attacks", sessionStat.getAttacks());
        json.put("missed", sessionStat.getMissed());
        json.put("moves", sessionStat.getMoves());

        // Для food, elixirs, scrolls - вычисляем общее количество
        // Собрано = Использовано (из sessionStat) + Осталось в инвентаре (из gameSession)
        int totalFood = sessionStat.getFood();
        int totalElixirs = sessionStat.getElixirs();
        int totalScrolls = sessionStat.getScrolls();

        if (gameSession != null && gameSession.getPlayer() != null) {
            Player player = gameSession.getPlayer();
            Inventory inventory = player.getInventory();

            // 1. Получаем сокровища из инвентаря
            treasuresValue = inventory.getTreasureValue();

            // 2. Добавляем то, что осталось в инвентаре для food, elixirs, scrolls
            int foodInInventory = inventory.count(ItemType.FOOD);
            int elixirsInInventory = inventory.count(ItemType.ELIXIR);
            int scrollsInInventory = inventory.count(ItemType.SCROLL);

            totalFood += foodInInventory;
            totalElixirs += elixirsInInventory;
            totalScrolls += scrollsInInventory;

            // Отладочный вывод
            System.out.println("=== DEBUG: Statistics calculation ===");
            System.out.println("Treasures from inventory: " + treasuresValue);
            System.out.println("SessionStat treasures: " + sessionStat.getTreasures());
            System.out.println("SessionStat food: " + sessionStat.getFood() + " + Inventory food: " + foodInInventory + " = " + totalFood);
            System.out.println("SessionStat elixirs: " + sessionStat.getElixirs() + " + Inventory elixirs: " + elixirsInInventory + " = " + totalElixirs);
            System.out.println("SessionStat scrolls: " + sessionStat.getScrolls() + " + Inventory scrolls: " + scrollsInInventory + " = " + totalScrolls);
            System.out.println("=== END DEBUG ===");
        } else {
            // Если gameSession недоступен, используем значение из sessionStat
            treasuresValue = sessionStat.getTreasures();
            System.out.println("WARNING: GameSession is null, using SessionStat treasures: " + treasuresValue);
        }

        json.put("treasures", treasuresValue);
        json.put("food", totalFood);
        json.put("elixirs", totalElixirs);
        json.put("scrolls", totalScrolls);

        return json;
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