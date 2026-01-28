package org.example.datalayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.config.GameConstants;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.Player;
import org.example.domain.entity.SessionStat;
import org.example.domain.enums.ItemType;
import org.example.domain.interfaces.ISessionStatRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionStatRepository implements ISessionStatRepository {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(SessionStat sessionStat) throws IOException {
        File statsFile = new File(GameConstants.PathToFiles.STATISTICS_PATH);
        createParentDirectoryIfNeeded(statsFile);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(statsFile, sessionStat);
    }

    @Override
    public void addToScoreboard(SessionStat sessionStat, GameSession gameSession) throws IOException {
        File scoreboardFile = new File(GameConstants.PathToFiles.SCOREBOARD_PATH);
        createScoreboardFileIfNeeded(scoreboardFile);

        JsonNode allStatJson = objectMapper.readTree(scoreboardFile);
        if (!allStatJson.isObject()) {
            throw new IllegalStateException("scoreboard.json должен быть JSON-объектом");
        }

        ObjectNode rootObject = (ObjectNode) allStatJson;
        ArrayNode sessionStatsArray = getOrCreateSessionStatsArray(rootObject);
        ObjectNode sessionStatJson = createSessionStatJson(sessionStat, gameSession);

        sessionStatsArray.add(sessionStatJson);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(scoreboardFile, rootObject);
    }

    @Override
    public void reset(SessionStat sessionStat) throws IOException {
        sessionStat.reset();
        save(sessionStat);
    }

    @Override
    public List<SessionStat> getAllStats() {
        List<SessionStat> stats = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            File file = new File(GameConstants.PathToFiles.SCOREBOARD_PATH);
            if (!file.exists()) {
                return stats;
            }

            JsonNode root = mapper.readTree(file);
            JsonNode sessionNode = root.get("sessionStats");

            if (sessionNode != null && sessionNode.isArray()) {
                SessionStat[] statArray = mapper.treeToValue(sessionNode, SessionStat[].class);
                stats = Arrays.asList(statArray);
            }
        } catch (IOException e) {
            System.err.println("Error loading scoreboard: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    // Приватные вспомогательные методы

    private ObjectNode createSessionStatJson(SessionStat sessionStat, GameSession gameSession) {
        ObjectNode json = objectMapper.createObjectNode();
        int treasuresValue = 0;
        int totalFood = sessionStat.getFood();
        int totalElixirs = sessionStat.getElixirs();
        int totalScrolls = sessionStat.getScrolls();

        if (gameSession != null && gameSession.getPlayer() != null) {
            Player player = gameSession.getPlayer();
            Inventory inventory = player.getInventory();
            treasuresValue = inventory.getTreasureValue();
        } else {
            treasuresValue = sessionStat.getTreasures();
        }

        json.put("treasures", treasuresValue);
        json.put("levelNum", sessionStat.getLevelNum());
        json.put("enemies", sessionStat.getEnemies());
        json.put("food", totalFood);
        json.put("elixirs", totalElixirs);
        json.put("scrolls", totalScrolls);
        json.put("attacks", sessionStat.getAttacks());
        json.put("missed", sessionStat.getMissed());
        json.put("moves", sessionStat.getMoves());

        return json;
    }

    private void createParentDirectoryIfNeeded(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private void createScoreboardFileIfNeeded(File scoreboardFile) throws IOException {
        if (!scoreboardFile.exists()) {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("sessionStats", objectMapper.createArrayNode());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(scoreboardFile, root);
        }
    }

    private ArrayNode getOrCreateSessionStatsArray(ObjectNode rootObject) {
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