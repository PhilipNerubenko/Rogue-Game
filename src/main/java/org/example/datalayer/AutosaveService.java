package org.example.datalayer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.config.GameConstants;
import org.example.domain.entity.*;
import org.example.domain.model.Position;
import org.example.domain.service.LevelGenerator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления автосохранениями игры
 */
public class AutosaveService {

    private static final String AUTOSAVE_DIR = GameConstants.PathToFiles.DATA_DIR + "/autosaves";
    private static final String AUTOSAVE_PREFIX = "autosave_";
    private static final String AUTOSAVE_EXTENSION = ".json";
    private static final int MAX_AUTOSAVES = 10;

    private final ObjectMapper objectMapper;

    public AutosaveService() {
        this.objectMapper = new ObjectMapper();

        // Регистрируем модули
        this.objectMapper.registerModule(new JavaTimeModule());

        // Настраиваем игнорирование неизвестных свойств
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Создаем кастомный модуль для Color
        SimpleModule colorModule = new SimpleModule();
        colorModule.addSerializer(Color.class, new StdSerializer<Color>(Color.class) {
            @Override
            public void serialize(Color value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("rgb", value.getRGB());
                gen.writeEndObject();
            }
        });

        colorModule.addDeserializer(Color.class, new StdDeserializer<Color>(Color.class) {
            @Override
            public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonNode node = p.getCodec().readTree(p);
                int rgb = node.get("rgb").asInt();
                return new Color(rgb);
            }
        });

        this.objectMapper.registerModule(colorModule);

        // Создаем директорию для автосохранений, если она не существует
        createAutosaveDirectory();
    }

    /**
     * Сохраняет текущее состояние игры
     */
    public boolean saveGame(GameSession session, SessionStat sessionStat) {
        if (session.getPlayer() == null || session.getPlayer().isDead()) {
            System.err.println("Cannot save: player is dead or not initialized");
            return false;
        }

        try {
            // Создаем объект состояния игры
            GameState gameState = createGameState(session, sessionStat);

            // Генерируем имя файла с timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = AUTOSAVE_PREFIX + timestamp + AUTOSAVE_EXTENSION;
            String filepath = AUTOSAVE_DIR + "/" + filename;

            // Сохраняем в файл
            objectMapper.writeValue(new File(filepath), gameState);

            System.out.println("Game autosaved to: " + filepath);

            // Очищаем старые сохранения
            cleanupOldSaves();

            return true;

        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Загружает последнее сохранение
     */
    public GameState loadLatestSave() {
        try {
            List<File> saveFiles = getAutosaveFilesSorted();
            if (saveFiles.isEmpty()) {
                System.err.println("No save files found");
                return null;
            }

            File latestSave = saveFiles.get(0);
            System.out.println("Loading save: " + latestSave.getName());

            return objectMapper.readValue(latestSave, GameState.class);

        } catch (IOException e) {
            System.err.println("Failed to load save: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Загружает и восстанавливает игру из последнего сохранения
     */
    public boolean loadAndRestoreGame(GameSession session, SessionStat sessionStat,
                                      LevelGenerator levelGenerator) {
        GameState gameState = loadLatestSave();
        if (gameState == null) {
            return false;
        }

        try {
            restoreGameState(gameState, session, sessionStat, levelGenerator);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to restore game state: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Очищает старые сохранения (оставляет только MAX_AUTOSAVES самых новых)
     */
    public void cleanupOldSaves() {
        try {
            List<File> saveFiles = getAutosaveFilesSorted();

            if (saveFiles.size() > MAX_AUTOSAVES) {
                for (int i = MAX_AUTOSAVES; i < saveFiles.size(); i++) {
                    File fileToDelete = saveFiles.get(i);
                    if (fileToDelete.delete()) {
                        System.out.println("Deleted old save: " + fileToDelete.getName());
                    } else {
                        System.err.println("Failed to delete: " + fileToDelete.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up old saves: " + e.getMessage());
        }
    }

    /**
     * Получает список всех файлов автосохранений, отсортированных по дате (новые первыми)
     */
    public List<File> getAutosaveFilesSorted() {
        File dir = new File(AUTOSAVE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(AUTOSAVE_PREFIX) && name.endsWith(AUTOSAVE_EXTENSION));

        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .sorted((f1, f2) -> {
                    // Сортируем по имени файла (timestamp в имени)
                    return f2.getName().compareTo(f1.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Создает GameState из текущей игровой сессии
     */

    private GameState createGameState(GameSession session, SessionStat sessionStat) {
        GameState gameState = new GameState();

        // Сохраняем состояние игрока
        GameState.PlayerState playerState = new GameState.PlayerState();
        Player player = session.getPlayer();

        playerState.setMaxHealth(player.getMaxHealth());
        playerState.setHealth(player.getHealth());
        playerState.setAgility(player.getAgility());
        playerState.setStrength(player.getStrength());

        Position playerPos = player.getPosition();
        playerState.setPositionX(playerPos.getX());
        playerState.setPositionY(playerPos.getY());
        playerState.setSleepTurns(player.isSleepTurns());

        // Сохраняем инвентарь
        Inventory inventory = player.getInventory();
        List<Item> allItems = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            allItems.addAll(inventory.getItems(type));
        }
        playerState.setInventoryItems(allItems);

        // Сохраняем экипированное оружие
        playerState.setEquippedWeapon(player.getEquippedWeapon());

        // Сохраняем состояние уровня
        GameState.LevelState levelState = new GameState.LevelState();
        levelState.setLevelNumber(session.getLevelNum());
        levelState.setAsciiMap(session.getCurrentMap());
        levelState.setItems(new ArrayList<>(session.getCurrentLevelItems()));
        levelState.setEnemies(new ArrayList<>(session.getEnemies()));

        // Сохраняем комнаты из GameSession
        if (session.getRooms() != null) {
            levelState.setRooms(new ArrayList<>(session.getRooms()));
        }

        // Сохраняем состояние сессии
        GameState.GameSessionState gameSessionState = new GameState.GameSessionState();
        gameSessionState.setCurrentMap(session.getCurrentMap());

        // Устанавливаем все состояния в gameState
        gameState.setPlayerState(playerState);
        gameState.setLevelState(levelState);
        gameState.setSessionStat(sessionStat);
        gameState.setGameSessionState(gameSessionState);

        return gameState;
    }

    /**
     * Восстанавливает игровую сессию из GameState
     */
    private void restoreGameState(GameState gameState, GameSession session,
                                  SessionStat sessionStat, LevelGenerator levelGenerator) {
        // Восстанавливаем статистику
        if (gameState.getSessionStat() != null) {
            sessionStat.setTreasures(gameState.getSessionStat().getTreasures());
            sessionStat.setLevelNum(gameState.getSessionStat().getLevelNum());
            sessionStat.setEnemies(gameState.getSessionStat().getEnemies());
            sessionStat.setFood(gameState.getSessionStat().getFood());
            sessionStat.setElixirs(gameState.getSessionStat().getElixirs());
            sessionStat.setScrolls(gameState.getSessionStat().getScrolls());
            sessionStat.setAttacks(gameState.getSessionStat().getAttacks());
            sessionStat.setMissed(gameState.getSessionStat().getMissed());
            sessionStat.setMoves(gameState.getSessionStat().getMoves());
        }

        // Восстанавливаем LevelGenerator
        if (levelGenerator != null && gameState.getLevelState() != null) {
            levelGenerator.restoreFromGameState(
                    gameState.getLevelState().getAsciiMap(),
                    gameState.getLevelState().getRooms(),
                    gameState.getLevelState().getItems()
            );
        }

        // Восстанавливаем уровень
        session.setLevelNum(gameState.getLevelState().getLevelNumber());
        session.setCurrentMap(gameState.getLevelState().getAsciiMap());

        // Восстанавливаем предметы и врагов
        session.getCurrentLevelItems().clear();
        if (gameState.getLevelState().getItems() != null) {
            session.getCurrentLevelItems().addAll(gameState.getLevelState().getItems());
        }

        session.getEnemies().clear();
        if (gameState.getLevelState().getEnemies() != null) {
            session.getEnemies().addAll(gameState.getLevelState().getEnemies());
        }

        // Восстанавливаем игрока - КРИТИЧЕСКИ ВАЖНО: используем конструктор с параметрами
        GameState.PlayerState playerState = gameState.getPlayerState();

        // Создаем игрока с загруженными характеристиками
        Player player = new Player(
                new Position(playerState.getPositionX(), playerState.getPositionY()),
                playerState.getEquippedWeapon(),
                playerState.getMaxHealth(),
                playerState.getHealth(),
                playerState.getAgility(),
                playerState.getStrength()
        );

        player.setSleepTurns(playerState.isSleepTurns());

        // Восстанавливаем инвентарь
        Inventory inventory = new Inventory();
        if (playerState.getInventoryItems() != null) {
            for (Item item : playerState.getInventoryItems()) {
                inventory.add(item);
            }
        }
        player.setInventory(inventory);

        session.setPlayer(player);

        // Обновляем генератор уровня
        if (levelGenerator != null) {
            // Можно обновить комнаты и предметы в генераторе уровня
            // В текущей реализации генератор будет пересоздан при загрузке
        }
    }

    /**
     * Создает директорию для автосохранений
     */
    private void createAutosaveDirectory() {
        try {
            Path path = Paths.get(AUTOSAVE_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created autosave directory: " + AUTOSAVE_DIR);
            }
        } catch (IOException e) {
            System.err.println("Failed to create autosave directory: " + e.getMessage());
        }
    }

    /**
     * Проверяет, есть ли доступные сохранения
     */
    public boolean hasSaves() {
        return !getAutosaveFilesSorted().isEmpty();
    }

    /**
     * Получает информацию о доступных сохранениях
     */
    public List<String> getSaveInfo() {
        List<File> saves = getAutosaveFilesSorted();
        List<String> info = new ArrayList<>();

        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        for (File save : saves) {
            try {
                String filename = save.getName();
                // Извлекаем timestamp из имени файла
                String timestampStr = filename.substring(AUTOSAVE_PREFIX.length(),
                        filename.length() - AUTOSAVE_EXTENSION.length());

                LocalDateTime timestamp = LocalDateTime.parse(timestampStr,
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

                String displayTime = timestamp.format(displayFormat);

                GameState gameState = objectMapper.readValue(save, GameState.class);
                String levelInfo = "Level " + gameState.getLevelState().getLevelNumber();

                info.add(String.format("%s - %s - %s", displayTime, levelInfo, filename));

            } catch (Exception e) {
                info.add(save.getName() + " [corrupted]");
            }
        }

        return info;
    }
}