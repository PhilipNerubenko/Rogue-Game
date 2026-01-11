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
import org.example.domain.enums.ItemType;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.FogOfWarService;
import org.example.domain.factory.LevelGenerator;

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
    private FogOfWarService fogOfWarService;

    public AutosaveService() {
        this.objectMapper = new ObjectMapper();

        // Регистрируем модули
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Настраиваем сериализацию и десериализацию Color
        this.objectMapper.registerModule(createColorModule());

        // Создаем директорию для автосохранений
        createAutosaveDirectory();
    }

    /**
     * Создает кастомный модуль для сериализации/десериализации Color
     */
    private SimpleModule createColorModule() {
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

        return colorModule;
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
                .sorted((f1, f2) -> f2.getName().compareTo(f1.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Создает GameState из текущей игровой сессии
     */
    private GameState createGameState(GameSession session, SessionStat sessionStat) {
        GameState gameState = new GameState();

        // Сохраняем состояние игрока
        GameState.PlayerState playerState = createPlayerState(session.getPlayer());

        // Сохраняем состояние уровня
        GameState.LevelState levelState = createLevelState(session);

        // Сохраняем состояние тумана войны
        GameState.FogOfWarState fogOfWarState = createFogOfWarState();

        // Устанавливаем все состояния в gameState
        gameState.setPlayerState(playerState);
        gameState.setLevelState(levelState);
        gameState.setSessionStat(sessionStat);
        gameState.setFogOfWarState(fogOfWarState);

        // Создаем и устанавливаем GameSessionState
        GameState.GameSessionState gameSessionState = new GameState.GameSessionState();
        gameSessionState.setCurrentMap(session.getCurrentMap());
        gameState.setGameSessionState(gameSessionState);

        return gameState;
    }

    /**
     * Создает состояние игрока для сохранения
     */
    private GameState.PlayerState createPlayerState(Player player) {
        GameState.PlayerState playerState = new GameState.PlayerState();

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

        return playerState;
    }

    /**
     * Создает состояние уровня для сохранения
     */
    private GameState.LevelState createLevelState(GameSession session) {
        GameState.LevelState levelState = new GameState.LevelState();

        levelState.setLevelNumber(session.getLevelNum());
        levelState.setAsciiMap(session.getCurrentMap());
        levelState.setItems(new ArrayList<>(session.getCurrentLevelItems()));
        levelState.setEnemies(new ArrayList<>(session.getEnemies()));

        // Сохраняем комнаты из GameSession
        if (session.getRooms() != null) {
            levelState.setRooms(new ArrayList<>(session.getRooms()));
        }

        return levelState;
    }

    /**
     * Создает состояние тумана войны для сохранения
     */
    private GameState.FogOfWarState createFogOfWarState() {
        GameState.FogOfWarState fogOfWarState = new GameState.FogOfWarState();

        if (fogOfWarService != null) {
            Set<Position> exploredCells = fogOfWarService.getAllExploredCells();
            Set<Room> exploredRooms = fogOfWarService.getAllExploredRooms();

            // Преобразуем в списки для сериализации
            List<Position> exploredCellsList = new ArrayList<>(exploredCells);
            List<Room> exploredRoomsList = new ArrayList<>(exploredRooms);

            fogOfWarState.setExploredCells(exploredCellsList);
            fogOfWarState.setExploredRooms(exploredRoomsList);
        } else {
            // Создаем пустые списки, чтобы избежать null в JSON
            fogOfWarState.setExploredCells(new ArrayList<>());
            fogOfWarState.setExploredRooms(new ArrayList<>());
        }

        return fogOfWarState;
    }

    /**
     * Восстанавливает игровую сессию из GameState
     */
    private void restoreGameState(GameState gameState, GameSession session,
                                  SessionStat sessionStat, LevelGenerator levelGenerator) {
        if (gameState == null) {
            System.err.println("[Autosave] ERROR: GameState is null!");
            return;
        }

        // Восстанавливаем статистику
        restoreSessionStatistics(gameState, sessionStat);

        // Восстанавливаем LevelGenerator
        restoreLevelGenerator(gameState, levelGenerator);

        // Восстанавливаем уровень
        restoreLevelState(gameState, session);

        // Восстанавливаем состояние тумана войны
        restoreFogOfWar(gameState);

        // Восстанавливаем игрока
        restorePlayer(gameState, session);
    }

    /**
     * Восстанавливает статистику сессии
     */
    private void restoreSessionStatistics(GameState gameState, SessionStat sessionStat) {
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
    }

    /**
     * Восстанавливает LevelGenerator
     */
    private void restoreLevelGenerator(GameState gameState, LevelGenerator levelGenerator) {
        if (levelGenerator != null && gameState.getLevelState() != null) {
            levelGenerator.restoreFromGameState(
                    gameState.getLevelState().getAsciiMap(),
                    gameState.getLevelState().getRooms(),
                    gameState.getLevelState().getItems()
            );
        }
    }

    /**
     * Восстанавливает состояние уровня
     */
    private void restoreLevelState(GameState gameState, GameSession session) {
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

        // Восстанавливаем комнаты
        if (gameState.getLevelState().getRooms() != null) {
            session.setRooms(new ArrayList<>(gameState.getLevelState().getRooms()));
        }
    }

    /**
     * Восстанавливает состояние тумана войны
     */
    private void restoreFogOfWar(GameState gameState) {
        if (gameState.getFogOfWarState() != null) {
            List<Position> savedCells = gameState.getFogOfWarState().getExploredCells();

            // Восстанавливаем только если fogOfWarService доступен
            if (fogOfWarService != null && savedCells != null) {
                Set<Position> cellsToRestore = new HashSet<>(savedCells);
                Set<Room> roomsToRestore = new HashSet<>(gameState.getFogOfWarState().getExploredRooms());

                fogOfWarService.restoreExploredCells(cellsToRestore);
                fogOfWarService.restoreExploredRooms(roomsToRestore);
            }
        }
    }

    /**
     * Восстанавливает игрока
     */
    private void restorePlayer(GameState gameState, GameSession session) {
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

    /**
     * Устанавливает сервис тумана войны
     */
    public void setFogOfWarService(FogOfWarService fogOfWarService) {
        this.fogOfWarService = fogOfWarService;
    }

//    /**
//     * Получает сервис тумана войны
//     */
//    public FogOfWarService getFogOfWarService() {
//        return fogOfWarService;
//    }
}