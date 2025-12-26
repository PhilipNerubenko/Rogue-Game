package org.example.presentation;

import jcurses.system.CharColor;
import org.example.GameInitializer;
import org.example.config.GameConstants;
import org.example.datalayer.Statistics;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Item;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.model.Position;
import org.example.domain.service.CombatService;
import org.example.domain.service.EnemyAIService;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.InventoryService;
import org.example.domain.service.LevelGenerator;
import org.example.domain.service.MovementService;

import java.io.IOException;
import java.util.List;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.Icons.OGRE;
import static org.example.config.GameConstants.Icons.SNAKE_MAGE;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.DIED;
import static org.example.config.GameConstants.TextMessages.TERMINATE;

/**
*    GameLoop — это оркестратор игрового процесса, который:
*    Читает ввод от игрока (InputHandler)
*    Отрисовывает экран (Renderer)
*    Связывает UI с бизнес-логикой
*    НЕ содержит правил игры , а только дергает сервисы
*/
public class GameLoop {

    private final GameSession session;
    private final InputHandler inputHandler;
    private final Renderer renderer;

    // Сервисы бизнес-логики (внедряются извне)
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final InventoryService inventoryService;
    private final MovementService movementService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private final char[][] asciiMap;

    // Позиция игрока (временно, пока не полностью перейдем на Player entity)
    private int playerX;
    private int playerY;
    private char symbolUnderPlayer;

    // сообщения и задержка сообщений
    private String activeMessageLine1;
    private String activeMessageLine2;
    private int messageTimer = 0; // начальный таймер
    private static final int MESSAGE_DURATION = 2; // время жизни сообщения

    public GameLoop(GameInitializer initializer) {
        // Извлекаем зависимости из инициализатора
        this.session = initializer.getSession();
        this.inputHandler = initializer.getInputHandler();
        this.renderer = initializer.getRenderer();
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.inventoryService = initializer.getInventoryService();
        this.movementService = initializer.getMovementService();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap = initializer.getAsciiMap();

        // Инициализация позиции игрока из сессии
        Position playerPos = session.getPlayer().getPosition();
        this.playerX = playerPos.getX();
        this.playerY = playerPos.getY();
        this.symbolUnderPlayer = asciiMap[playerY][playerX];
    }

    public void start() throws IOException {
        // Инициализация JCurses
        sun.misc.Signal.handle(new sun.misc.Signal(SIGINT_STRING), signal -> {
            renderer.shutdown();
            System.out.println(TERMINATE);
            System.exit(0);
        });

        renderer.clearScreen();

        enemyAIService.updateAllGhostEffects(session, playerX, playerY);
        System.out.print(HIDE_CURSOR);

        boolean running = true;

        while (running) {
            // Уменьшаеми таймер сообщений
            if (messageTimer > 0) {
                messageTimer--;
            } else {
                activeMessageLine1 = null;
                activeMessageLine2 = null;
            }

            if (session.getPlayer().isSleepTurns()) {
                // Пропускаем ход спящего игрока
                String sleepMsg = "You are sleep! Zzz...";
                session.getPlayer().setSleepTurns(false);
                renderer.drawMessage(UI_START_Y, sleepMsg, CharColor.CYAN);

                // Ждем подтверждения (любую клавишу)
                //Toolkit.readCharacter();

                // Затираем старое положение игрока
                renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);

                // Ход врагов (игрок пропускает ход)
                List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(session, combatService, playerX, playerY, asciiMap);
                if (!enemyMessages.isEmpty()) {
                    activeMessageLine2 = String.join(", ", enemyMessages);
                    messageTimer = MESSAGE_DURATION;
                }

                // Перерисовываем
                renderer.clearScreen();
                drawMap();
                drawEnemies();
                renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);

                // Обновляем HP
                drawUI();
                if (activeMessageLine2 != null) {
                    renderer.drawMessage(MESSAGE_LINE_2, activeMessageLine2, CharColor.YELLOW);
                }

                running = checkDeath(running);
                continue; // Пропускаем остальную обработку ввода
            }
            // 1. РЕНДЕР: рисуем текущее состояние
            renderer.clearScreen();
            drawMap(); // Рисуем карту с учетом тумана
            drawEnemies(); // Рисуем видимых врагов
            renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);
            drawUI(); // Рисует подсказку и панель со здоровьем
            if (messageTimer > 0) {
                if (activeMessageLine1 != null) {
                    renderer.drawMessage(MESSAGE_LINE_1, activeMessageLine1, CharColor.YELLOW);
                }
                if (activeMessageLine2 != null) {
                    renderer.drawMessage(MESSAGE_LINE_2, activeMessageLine2, CharColor.YELLOW);
                }
            }

            // 2. ВВОД: читаем команду игрока
            InputCommand command = inputHandler.readCommand();

            if (command.getType() == InputCommand.Type.QUIT) {
                running = false;
                continue;
            }

            // 3. ОБРАБОТКА: применяем команду
            if (command.getType() == InputCommand.Type.MOVE) {
                Direction dir = command.getDirection();

                // Вычисляем новую позицию
                int newX = playerX + dir.getDx();
                int newY = playerY + dir.getDy();

                // Проверяем, есть ли враг
                Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
                if (enemyAtPosition != null) {
                    String message = combatService.attackEnemy(session, enemyAtPosition);
                    activeMessageLine1 = message;
                    messageTimer = MESSAGE_DURATION;

                    if (enemyAtPosition.getHealth() <= 0) {
                        combatService.removeEnemy(session, enemyAtPosition, asciiMap);
                    }
                } else if (canMoveTo(newX, newY)) { // Если можно двигаться - перемещаем
                    // Затираем старую позицию (возвращаем символ под игроком)
                    renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);
                    //Помечаем клетку как исследованную
                    fogOfWarService.markCellAsExplored(newX, newY);
                    // Обновляем локальные координаты
                    playerX = newX;
                    playerY = newY;
                    symbolUnderPlayer = asciiMap[playerY][playerX];

                    // 🔥 СИНХРОНИЗИРУЕМ с Player entity
                    session.getPlayer().move(dir);
                }
            }

            // 4. ОБНОВЛЕНИЕ МИРА: туман и враги
            // 🔥 ОБНОВЛЯЕМ ТУМАН ПОСЛЕ перемещения игрока (с актуальной позицией)
            fogOfWarService.updateVisibility(session.getPlayer().getPosition(), asciiMap);

            // Обновляем врагов (теперь с актуальными координатами)
            List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(session, combatService, playerX, playerY, asciiMap);
            if (!enemyMessages.isEmpty()) {
                activeMessageLine2 = String.join(", ", enemyMessages);
                messageTimer = MESSAGE_DURATION;
            }
            running = checkDeath(running);
        }

        renderer.shutdown();
    }

    private boolean checkDeath(boolean running) throws IOException {
        if (session.getPlayer().getHealth() <= 0) {
            renderer.drawMessage(DEATH_MESSAGE_Y, DIED, CharColor.RED);
            running = false;
            Statistics.updateScoreBoard();
        }
        return running;
    }

    private void movePlayer(Direction direction) {
        int newX = playerX + direction.getDx();
        int newY = playerY + direction.getDy();

        Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
        if (enemyAtPosition != null) {
            combatService.attackEnemy(session, enemyAtPosition);
            if (enemyAtPosition.getHealth() <= 0) {
                combatService.removeEnemy(session, enemyAtPosition, asciiMap);
            }
            return;
        }

        // === АВТОПОДБОР ПРЕДМЕТА ===
        Item picked = null;
        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() == newX && item.getY() == newY) {
                picked = item;
                break;
            }
        }

        if (picked != null) {
            if (session.getPlayer().getInventoryService().isFull()) {
                renderer.drawMessage(28, "Inventory is full!", CharColor.RED);
                return; // не идём, если нет места
            }

            session.getPlayer().getInventoryService().add(picked);
            session.getCurrentLevelItems().remove(picked);
            asciiMap[newY][newX] = '.'; // убираем с карты
            renderer.drawMessage(28, "Picked: " + picked.getSubType(), CharColor.YELLOW);
        }


        if (canMoveTo(newX, newY)) {
            // Запоминаем, что мы исследовали клетку, на которую встаём
            fogOfWarService.markCellAsExplored(newX, newY);
            // Обновляем локальные переменные
            playerX = newX;
            playerY = newY;
            symbolUnderPlayer = asciiMap[playerY][playerX];

            // ✅ Обновляем позицию в сущности через Direction
            session.getPlayer().move(direction);
        }
    }

    // Вспомогательный метод для читаемости
    private boolean canMoveTo(int x, int y) {
        return x >= 0 && x < GameConstants.Map.WIDTH &&
                y >= 0 && y < GameConstants.Map.HEIGHT &&
                asciiMap[y][x] != W_WALL && asciiMap[y][x] != H_WALL &&
                asciiMap[y][x] != EMPTINESS;
    }

    private void drawEnemies() {
        for (Enemy enemy : session.getEnemies()) {
            if (!enemy.isInvisible() && fogOfWarService.isVisible(enemy.getX(), enemy.getY())) {
                short color = (short) getEnemyColor(enemy);
                renderer.drawChar(enemy.getX(), enemy.getY(), enemy.getType(), color);
            }
        }
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

    private void drawMap() {

        renderer.drawMapWithFog(
                asciiMap,
                session.getPlayer(),
                fogOfWarService,
                levelGenerator
        );


    }

    private void drawUI() {

        // === Отрисовка предметов ===
        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() >= 0 && item.getY() >= 0) {
                if (fogOfWarService.isVisible(item.getX(), item.getY())) {  // ТОЛЬКО В СВЕТЕ!
                    char symbol = switch (item.getType()) {
                        case "food"     -> ',';
                        case "elixir"   -> '!';
                        case "scroll"   -> '?';
                        case "weapon"   -> ')';
                        case "treasure" -> '$';
                        default         -> '*';
                    };
                    renderer.drawChar(item.getX(), item.getY(), symbol, CharColor.YELLOW);
                }
            }
        }

        // Подсказка
        renderer.drawString(3, 29, "Use WASD to move, ESC to exit", CharColor.CYAN);
        // Статус Бар
        renderer.drawStatusBar(session.getPlayer().getHealth(),
                session.getPlayer().getMaxHealth(), 1, 0);
    }

    private void syncPlayerPositionWithEntity() {
        Position pos = session.getPlayer().getPosition();
        this.playerX = pos.getX();
        this.playerY = pos.getY();
    }
}