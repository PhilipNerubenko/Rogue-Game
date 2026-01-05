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
import org.example.domain.model.Room;
import org.example.domain.service.FogOfWarService;
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
    private FogOfWarService fogOfWarService;

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
//            System.out.println("=== STARTING GAME SAVE ===");

            // Создаем объект состояния игры
            GameState gameState = createGameState(session, sessionStat);

            // Генерируем имя файла с timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = AUTOSAVE_PREFIX + timestamp + AUTOSAVE_EXTENSION;
            String filepath = AUTOSAVE_DIR + "/" + filename;

//            System.out.println("[Autosave] Writing to file: " + filepath);

            // Сохраняем в файл
            objectMapper.writeValue(new File(filepath), gameState);

//            System.out.println("Game autosaved to: " + filepath);

            // Проверим содержимое файла
            File savedFile = new File(filepath);
//            System.out.println("[Autosave] File size: " + savedFile.length() + " bytes");

            if (savedFile.length() < 100) {
                System.err.println("[Autosave] WARNING: Saved file is very small, might be corrupted!");
            }

            // Очищаем старые сохранения
            cleanupOldSaves();

//            System.out.println("=== GAME SAVE COMPLETED ===\n");

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
//        System.out.println("=== AUTOSAVE: CREATING GAME STATE ===");

        GameState gameState = new GameState();

        // Сохраняем состояние игрока
        GameState.PlayerState playerState = new GameState.PlayerState();
        Player player = session.getPlayer();

//        System.out.println("[Autosave] Player health: " + player.getHealth() + "/" + player.getMaxHealth());
//        System.out.println("[Autosave] Player position: (" + player.getPosition().getX() + ", " + player.getPosition().getY() + ")");

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
//            System.out.println("[Autosave] Rooms to save: " + session.getRooms().size());
        }

        // Сохраняем состояние тумана войны - КРИТИЧЕСКИ ВАЖНЫЙ БЛОК
        GameState.FogOfWarState fogOfWarState = new GameState.FogOfWarState();

        if (fogOfWarService != null) {
            Set<Position> exploredCells = fogOfWarService.getAllExploredCells();
            Set<Room> exploredRooms = fogOfWarService.getAllExploredRooms();

//            System.out.println("[Autosave] FogOfWarService is available");
//            System.out.println("[Autosave] Explored cells count: " + exploredCells.size());
//            System.out.println("[Autosave] Explored rooms count: " + exploredRooms.size());

            // Детальная информация о нескольких клетках
//            System.out.println("[Autosave] Sample explored cells (first 5):");
            int cellCount = 0;
            for (Position pos : exploredCells) {
                if (cellCount++ < 5) {
//                    System.out.println("[Autosave]   Cell (" + pos.getX() + ", " + pos.getY() + ")");
                }
            }

            // Преобразуем в списки для сериализации
            List<Position> exploredCellsList = new ArrayList<>(exploredCells);
            List<Room> exploredRoomsList = new ArrayList<>(exploredRooms);

            fogOfWarState.setExploredCells(exploredCellsList);
            fogOfWarState.setExploredRooms(exploredRoomsList);

//            System.out.println("[Autosave] FogOfWarState populated with " +
//                    exploredCellsList.size() + " cells and " +
//                    exploredRoomsList.size() + " rooms");
        } else {
//            System.out.println("[Autosave] WARNING: fogOfWarService is NULL! Fog of war will not be saved.");
            // Создаем пустые списки, чтобы избежать null в JSON
            fogOfWarState.setExploredCells(new ArrayList<>());
            fogOfWarState.setExploredRooms(new ArrayList<>());
        }

        // Устанавливаем все состояния в gameState
        gameState.setPlayerState(playerState);
        gameState.setLevelState(levelState);
        gameState.setSessionStat(sessionStat);

        // Создаем и устанавливаем GameSessionState
        GameState.GameSessionState gameSessionState = new GameState.GameSessionState();
        gameSessionState.setCurrentMap(session.getCurrentMap());
        gameState.setGameSessionState(gameSessionState);

        gameState.setFogOfWarState(fogOfWarState);

//        System.out.println("[Autosave] GameState created successfully");
//        System.out.println("[Autosave] FogOfWarState in GameState: " +
//                (gameState.getFogOfWarState() != null));
        if (gameState.getFogOfWarState() != null) {
//            System.out.println("[Autosave] Cells in FogOfWarState: " +
//                    gameState.getFogOfWarState().getExploredCells().size());
        }
//        System.out.println("=== AUTOSAVE: GAME STATE CREATED ===\n");

        return gameState;
    }

    /**
     * Восстанавливает игровую сессию из GameState
     */
    private void restoreGameState(GameState gameState, GameSession session,
                                  SessionStat sessionStat, LevelGenerator levelGenerator) {
//        System.out.println("=== AUTOSAVE: RESTORING GAME STATE ===");

        if (gameState == null) {
            System.err.println("[Autosave] ERROR: GameState is null!");
            return;
        }

//        System.out.println("[Autosave] GameState timestamp: " + gameState.getTimestamp());
//        System.out.println("[Autosave] Level in save: " + gameState.getLevelState().getLevelNumber());

        // Восстанавливаем статистику
        if (gameState.getSessionStat() != null) {
//            System.out.println("[Autosave] Restoring statistics...");
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
//            System.out.println("[Autosave] Restoring LevelGenerator...");
            levelGenerator.restoreFromGameState(
                    gameState.getLevelState().getAsciiMap(),
                    gameState.getLevelState().getRooms(),
                    gameState.getLevelState().getItems()
            );
        }

        // Восстанавливаем уровень
        session.setLevelNum(gameState.getLevelState().getLevelNumber());
        session.setCurrentMap(gameState.getLevelState().getAsciiMap());

//        System.out.println("[Autosave] Level number: " + session.getLevelNum());
//        System.out.println("[Autosave] Map dimensions: " +
//                (session.getCurrentMap() != null ?
//                        session.getCurrentMap().length + "x" + session.getCurrentMap()[0].length : "null"));

        // Восстанавливаем предметы и врагов
        session.getCurrentLevelItems().clear();
        if (gameState.getLevelState().getItems() != null) {
//            System.out.println("[Autosave] Items to restore: " + gameState.getLevelState().getItems().size());
            session.getCurrentLevelItems().addAll(gameState.getLevelState().getItems());
        }

        session.getEnemies().clear();
        if (gameState.getLevelState().getEnemies() != null) {
//            System.out.println("[Autosave] Enemies to restore: " + gameState.getLevelState().getEnemies().size());
            session.getEnemies().addAll(gameState.getLevelState().getEnemies());
        }

        // Восстанавливаем комнаты
        if (gameState.getLevelState().getRooms() != null) {
//            System.out.println("[Autosave] Rooms to restore: " + gameState.getLevelState().getRooms().size());
            session.setRooms(new ArrayList<>(gameState.getLevelState().getRooms()));
        }

        // КРИТИЧЕСКИ ВАЖНО: Восстанавливаем состояние тумана войны
//        System.out.println("[Autosave] Checking FogOfWarState in save...");
        if (gameState.getFogOfWarState() != null) {
//            System.out.println("[Autosave] FogOfWarState found in save!");
//            System.out.println("[Autosave] Explored cells in save: " +
//                    gameState.getFogOfWarState().getExploredCells().size());
//            System.out.println("[Autosave] Explored rooms in save: " +
//                    gameState.getFogOfWarState().getExploredRooms().size());

            // Проверяем несколько клеток из сохранения
            List<Position> savedCells = gameState.getFogOfWarState().getExploredCells();
            if (savedCells != null && !savedCells.isEmpty()) {
//                System.out.println("[Autosave] Sample cells from save (first 3):");
                for (int i = 0; i < Math.min(3, savedCells.size()); i++) {
                    Position pos = savedCells.get(i);
//                    System.out.println("[Autosave]   Cell (" + pos.getX() + ", " + pos.getY() + ")");
                }
            }

            // Восстанавливаем только если fogOfWarService доступен
            if (fogOfWarService != null) {
//                System.out.println("[Autosave] FogOfWarService is available, restoring data...");

                Set<Position> cellsToRestore = new HashSet<>(savedCells);
                Set<Room> roomsToRestore = new HashSet<>(gameState.getFogOfWarState().getExploredRooms());

                fogOfWarService.restoreExploredCells(cellsToRestore);
                fogOfWarService.restoreExploredRooms(roomsToRestore);

//                System.out.println("[Autosave] Fog of war state restored successfully");
            } else {
//                System.err.println("[Autosave] ERROR: fogOfWarService is null! Cannot restore fog of war.");
            }
        } else {
//            System.out.println("[Autosave] WARNING: No FogOfWarState in save file!");
//            System.out.println("[Autosave] This might be an old save or fog of war was not saved.");
        }

        // Восстанавливаем игрока
//        System.out.println("[Autosave] Restoring player...");
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

//        System.out.println("[Autosave] Player created at position: (" +
//                playerState.getPositionX() + ", " + playerState.getPositionY() + ")");
//        System.out.println("[Autosave] Player health: " + playerState.getHealth() + "/" + playerState.getMaxHealth());

        player.setSleepTurns(playerState.isSleepTurns());

        // Восстанавливаем инвентарь
        Inventory inventory = new Inventory();
        if (playerState.getInventoryItems() != null) {
//            System.out.println("[Autosave] Inventory items to restore: " + playerState.getInventoryItems().size());
            for (Item item : playerState.getInventoryItems()) {
                inventory.add(item);
            }
        }
        player.setInventory(inventory);

        session.setPlayer(player);

//        System.out.println("=== AUTOSAVE: GAME STATE RESTORED ===\n");
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
     * Сохраняет состояние тумана войны
     */
    public void saveFogOfWarState(FogOfWarService fog, String filename) {
        try {
            File fogFile = new File(AUTOSAVE_DIR + "/" + filename + "_fog.json");

            // Создаем объект для сохранения состояния тумана
            Map<String, Object> fogState = new HashMap<>();

            // Сохраняем исследованные клетки
            Set<String> exploredPositions = new HashSet<>();
            for (Position pos : fog.getAllExploredCells()) {
                exploredPositions.add(pos.getX() + "," + pos.getY());
            }
            fogState.put("exploredCells", exploredPositions);

            // Сохраняем исследованные комнаты
            List<Integer> exploredRoomIds = new ArrayList<>();
            for (Room room : fog.getAllExploredRooms()) {
                exploredRoomIds.add(getRoomId(room));
            }
            fogState.put("exploredRooms", exploredRoomIds);

            // Записываем в файл
            objectMapper.writeValue(fogFile, fogState);

        } catch (IOException e) {
            System.err.println("Failed to save fog of war state: " + e.getMessage());
        }
    }

    /**
     * Загружает состояние тумана войны
     */
    public void loadFogOfWarState(FogOfWarService fog, String filename) {
        try {
            File fogFile = new File(AUTOSAVE_DIR + "/" + filename + "_fog.json");
            if (!fogFile.exists()) {
                return; // Файл не существует, оставляем состояние по умолчанию
            }

            // Читаем состояние
            Map<String, Object> fogState = objectMapper.readValue(fogFile, Map.class);

            // Восстанавливаем исследованные клетки
            Set<String> exploredPositions = (Set<String>) fogState.get("exploredCells");
            if (exploredPositions != null) {
                for (String posStr : exploredPositions) {
                    String[] coords = posStr.split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    fog.markCellAsExplored(x, y);
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to load fog of war state: " + e.getMessage());
        }
    }

    /**
     * Получает ID комнаты (нужно добавить поле id в класс Room или использовать хэш)
     */
    private int getRoomId(Room room) {
        // Простой способ получить уникальный ID для комнаты
        return Objects.hash(room.getX1(), room.getY1(), room.getWidth(), room.getHeight());
    }

    /**
     * Устанавливает сервис тумана войны
     */
    public void setFogOfWarService(FogOfWarService fogOfWarService) {
        this.fogOfWarService = fogOfWarService;
    }

    /**
     * Получает сервис тумана войны
     */
    public FogOfWarService getFogOfWarService() {
        return fogOfWarService;
    }
}