package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.*;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.presentation.InputHandler;
import org.example.presentation.JCursesRenderer;
import org.example.presentation.Renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.Map.MAP_LEVEL;
import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class GameInitializer {
    private GameSession session;
    private Renderer renderer;
    private InputHandler inputHandler;
    private CombatService combatService;
    private EnemyAIService enemyAIService;
    private MovementService movementService;
    private InventoryService inventoryService;
    private FogOfWarService fogOfWarService;
    private LevelGenerator levelGenerator;

    // Основной конструктор для dependency injection
    public GameInitializer(GameSession session, Renderer renderer, LevelGenerator levelGenerator,
                           EnemyAIService enemyAIService, CombatService combatService,
                           InputHandler inputHandler, InventoryService inventoryService,
                           FogOfWarService fogOfWarService, MovementService movementService) {
        this.session = session;
        this.renderer = renderer;
        this.levelGenerator = levelGenerator;
        this.enemyAIService = enemyAIService;
        this.combatService = combatService;
        this.inputHandler = inputHandler;
        this.inventoryService = inventoryService;
        this.fogOfWarService = fogOfWarService;
        this.movementService = movementService;
    }

    // Упрощенный конструктор для основного использования
    public GameInitializer() {
        this.session = new GameSession();
        this.levelGenerator = new LevelGenerator(session);
        this.fogOfWarService = new FogOfWarService(levelGenerator);
        this.renderer = new JCursesRenderer();
        this.enemyAIService = new EnemyAIService();
        this.combatService = new CombatService();
        this.inputHandler = new InputHandler();
        this.inventoryService = new InventoryService();
        this.movementService = new MovementService();
    }

    public void initialize() {
        // Инициализация сессии
        session.setEnemies(new ArrayList<>());

        // Создание уровня
        char[][] asciiMap = levelGenerator.createAsciiMap(GameConstants.Map.MAP_LEVEL);
        session.setCurrentMap(asciiMap);
        session.setRooms(levelGenerator.getRooms());

        // Создание игрока

        Position playerPos = createPlayerPosition();
        Player player = new Player(playerPos, new Inventory());

        int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
        int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);

//        int playerX = startRoom.getX1() + 2; // Центр комнаты
//        int playerY = startRoom.getY1() + 2;
        Player player = new Player(new Position(playerX, playerY));
        session.setPlayer(player);

        // Создание врагов
        createEnemies();

        // Отрисовка начального состояния
        drawInitialState();
    }

    private Position createPlayerPosition() {
        Room startRoom = levelGenerator.getRooms().getFirst();
        int playerX = startRoom.getX1() + 2;
        int playerY = startRoom.getY1() + 2;
        return new Position(playerX, playerY);
    }

    private void createEnemies() {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();

        // Случайная плотность: 40-60% комнат с врагами
        int totalRoomsWithEnemies = calculateTotalRoomsWithEnemies(rooms.size(), rand);

        // Перемешиваем комнаты, чтобы выбрать случайные
        List<Room> shuffledRooms = new ArrayList<>(rooms);
        Collections.shuffle(shuffledRooms, rand);

        int enemiesPlaced = 0;

        for (Room room : shuffledRooms) {
            if (enemiesPlaced >= totalRoomsWithEnemies) break;
            if (room.isStartRoom()) continue; // Пропускаем стартовую комнату

            enemiesPlaced += createEnemiesInRoom(room, rand, session);
        }
    }

    private int calculateTotalRoomsWithEnemies(int totalRooms, Random rand) {
        int roomsWithEnemies = (int) Math.round(totalRooms * (MIN_ENEMY_DENSITY + rand.nextDouble() * DENSITY_RANGE));
        return Math.max(MIN_ROOMS_WITH_ENEMIES, roomsWithEnemies);
    }

    private int createEnemiesInRoom(Room room, Random rand, GameSession session) {
        int enemiesCreated = 0;
        int enemiesInRoom = 1; // Временно по одному врагу в комнате

        for (int j = 0; j < enemiesInRoom; j++) {
            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);

            EnemyType randomType = EnemyType.values()[rand.nextInt(EnemyType.values().length)];
            Enemy enemy = randomType.create(1);
            enemy.setX(enemyX);
            enemy.setY(enemyY);

            session.getEnemies().add(enemy);
            enemiesCreated++;
        }

        return enemiesCreated;
    }

    private void drawInitialState() {
        // Очистка экрана
        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));

        // Отрисовка карты с туманом
        renderer.drawMapWithFog(
                session.getCurrentMap(),
                session.getPlayer(),
                fogOfWarService,
                levelGenerator
        );

        // Отображение интерфейса
        displayInterface();

        // Отрисовка врагов
        drawEnemies();
        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);
        // Подсказка управления
        App.printLine(MESSAGE_LINE_3, CONTROL_HINT, hintColor, MAP_WIDTH);
    }

    private void displayInterface() {
        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);
        App.printLine(MESSAGE_LINE_3, CONTROL, hintColor, MAP_WIDTH);

        Player player = session.getPlayer();
        CharColor statusColor = new CharColor(CharColor.YELLOW, CharColor.BLACK);
        String healthStatus = String.format("HP: %d/%d",
                player.getHealth(), player.getMaximumHealth());
        App.printLine(STATUS_LINE_Y, healthStatus, statusColor, 30);
    }

    private void drawEnemies() {
        for (Enemy enemy : session.getEnemies()) {
            if (shouldDrawEnemy(enemy)) {
                drawEnemy(enemy);
            }
        }
    }

    private boolean shouldDrawEnemy(Enemy enemy) {
        return !enemy.isInvisible() &&
                fogOfWarService.isVisible(enemy.getX(), enemy.getY());
    }

    private void drawEnemy(Enemy enemy) {
        CharColor color = new CharColor(CharColor.BLACK, (short) getEnemyColor(enemy));
        Toolkit.printString(String.valueOf(enemy.getType()), enemy.getX() + MAP_OFFSET_X, enemy.getY(), color);
    }

    private static int getEnemyColor(Enemy enemy) {
        return switch (enemy.getType()) {
            case ZOMBIE -> CharColor.GREEN;
            case VAMPIRE -> CharColor.RED;
            case GHOST -> CharColor.WHITE;
            case OGRE -> CharColor.YELLOW;
            case SNAKE_MAGE -> CharColor.CYAN;
            default -> CharColor.WHITE;
        };
    }

    // Геттеры для всех компонентов
    public GameSession getSession() { return session; }
    public Renderer getRenderer() { return renderer; }
    public InputHandler getInputHandler() { return inputHandler; }
    public CombatService getCombatService() { return combatService; }
    public EnemyAIService getEnemyAIService() { return enemyAIService; }
    public MovementService getMovementService() { return movementService; }
    public InventoryService getInventoryService() { return inventoryService; }
    public FogOfWarService getFogOfWarService() { return fogOfWarService; }
    public LevelGenerator getLevelGenerator() { return levelGenerator; }

    public char[][] getAsciiMap() {
        // Возвращаем карту из сессии, а не создаем новую
        if (session.getCurrentMap() != null) {
            return session.getCurrentMap();
        }
        // Fallback на случай, если карта еще не создана
        return levelGenerator.createAsciiMap(MAP_LEVEL);
    }
}