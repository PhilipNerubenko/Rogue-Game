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
import java.util.List;
import java.util.Random;

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
    public GameInitializer() {
        this.session = new GameSession();
        this.renderer = new JCursesRenderer();
        this.levelGenerator = new LevelGenerator();
        this.enemyAIService = new EnemyAIService();
        this.combatService = new CombatService();
        this.inputHandler = new InputHandler();
        this.inventoryService = new InventoryService();
        this.fogOfWarService = new FogOfWarService();
        this.movementService = new MovementService();


    }

    public void initialize() {
        // Инициализация сессии
        session.setEnemies(new ArrayList<>());

        // Создание уровня
        char[][] asciiMap = levelGenerator.createAsciiMap(1);
        Room startRoom = levelGenerator.getRooms().getFirst();

        // Создание игрока
        int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
        int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);
        Player player = new Player(new Position(playerX, playerY), new Inventory());
        session.setPlayer(player);

        // Создание врагов
        createEnemies(levelGenerator, playerX, playerY);

        // Отрисовка начального состояния
        drawInitialState(asciiMap);
    }

//    private void createEnemies(LevelGenerator levelGenerator, int playerX, int playerY) {
//        List<Room> rooms = levelGenerator.getRooms();
//        Random rand = levelGenerator.getRand();
//        int playerAgility = 5; // Базовая ловкость игрока
//
//        for (int i = 0; i < EnemyType.values().length && i < rooms.size(); i++) {
//            Room room = rooms.get(i);
//
//            // Не размещаем врага в комнате игрока
//            if (room.isStartRoom()) {
//                continue;
//            }
//
//            // Случайная позиция внутри комнаты
//            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
//            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);
//
//            // Создаем врага на уровне 1
//            Enemy enemy = EnemyType.values()[i].create(1, playerAgility);
//
//            // Устанавливаем позицию
//            enemy.setX(enemyX);
//            enemy.setY(enemyY);
//
//            session.getEnemies().add(enemy);
//        }
//    }

    private void drawInitialState(char[][] asciiMap) {
        // Очистка экрана
        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));

        // Отрисовка карты
        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
            String element = new String(asciiMap[i]);
            Toolkit.printString(element, 0, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
        }

        // Отрисовка врагов
        drawEnemies();

        // Подсказка
        Toolkit.printString("Use WASD to move, ESC to exit", 0, 29, new CharColor(CharColor.CYAN, CharColor.BLACK));
    }

    private void drawEnemies() {
        for (Enemy enemy : session.getEnemies()) {
            if (!enemy.isInvisible()) {
                CharColor color = new CharColor(CharColor.BLACK, (short) getEnemyColor(enemy));
                Toolkit.printString(enemy.getType(), enemy.getX() + 3, enemy.getY(), color);
            }
        }
    }

    private int getEnemyColor(Enemy enemy) {
        return switch (enemy.getType()) {
            case "z" -> CharColor.GREEN;
            case "v" -> CharColor.RED;
            case "g" -> CharColor.WHITE;
            case "O" -> CharColor.YELLOW;
            case "s" -> CharColor.CYAN;
            default -> CharColor.WHITE;
        };
    }

    private void createEnemies(LevelGenerator levelGenerator, int playerX, int playerY) {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();
        int playerAgility = 5; // Базовая ловкость игрока

        for (int i = 0; i < EnemyType.values().length && i < rooms.size(); i++) {
            Room room = rooms.get(i);

            // Не размещаем врага в комнате игрока
            if (room.isStartRoom()) {
                continue;
            }

            // Случайная позиция внутри комнаты
            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);

            // Создаем врага на уровне 1
            Enemy enemy = EnemyType.values()[i].create(1, playerAgility);

            // Устанавливаем позицию
            enemy.setX(enemyX);
            enemy.setY(enemyY);

            session.getEnemies().add(enemy);
        }
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
    public char[][] getAsciiMap() { return levelGenerator.createAsciiMap(1);
    }

}
