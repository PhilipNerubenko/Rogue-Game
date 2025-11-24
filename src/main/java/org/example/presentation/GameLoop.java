package org.example.presentation;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.GameInitializer;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.model.Position;
import org.example.domain.service.CombatService;
import org.example.domain.service.EnemyAIService;
import org.example.domain.service.EnemyType;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.InventoryService;
import org.example.domain.service.LevelGenerator;
import org.example.domain.service.MovementService;

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

    public void start() {
        // Инициализация JCurses
        sun.misc.Signal.handle(new sun.misc.Signal("INT"), signal -> {
            renderer.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        renderer.clearScreen();
        System.out.print("\033[?25l");

        boolean running = true;

        while (running) {
            // 1. Очищаем и перерисовываем ВСЁ
            renderer.clearScreen();

            // 2. ОБНОВЛЯЕМ ТУМАН (после перемещения игрока)
            fogOfWarService.updateVisibility(session.getPlayer().getPosition(), asciiMap);

            drawMap(); // Рисуем карту
            drawEnemies(); // Рисуем врагов
            // 2. Рендер игрока
            renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);

            // 3. Ввод
            InputCommand command = inputHandler.readCommand();

            if (command.getType() == InputCommand.Type.QUIT) {
                running = false;
                continue;
            }

            // 3. Затираем игрока
            renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);

            // 4. Обработка
            if (command.getType() == InputCommand.Type.MOVE) {
                Direction dir = command.getDirection();
                movePlayer(dir); // Передаем Direction, а не dx/dy
            }

            // 5. Обновление врагов
            enemyAIService.moveEnemies(session, playerX, playerY, asciiMap);
            enemyAIService.updateEnemyEffects(session, playerX, playerY);
            drawEnemies();

            // 6. Обновить экран
            renderer.refresh();
        }

        renderer.shutdown();
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

        if (canMoveTo(newX, newY)) {
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
                asciiMap[y][x] != '|' && asciiMap[y][x] != '~' &&
                asciiMap[y][x] != ' ';
    }

    private void drawEnemies() {
//        for (Enemy enemy : session.getEnemies()) {
//            if (!enemy.isInvisible()) {
//                short color = (short) getEnemyColor(enemy);
//                renderer.drawChar(enemy.getX(), enemy.getY(), enemy.getType().charAt(0), color);
//            }
//        }
        for (Enemy enemy : session.getEnemies()) {
            if (!enemy.isInvisible() && fogOfWarService.isVisible(enemy.getX(), enemy.getY())) {
                short color = (short) getEnemyColor(enemy);
                renderer.drawChar(enemy.getX(), enemy.getY(), enemy.getType().charAt(0), color);
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
    private void drawMap() {
//        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
//            String element = new String(asciiMap[i]);
//            renderer.drawString(0, i, element, CharColor.WHITE); // X=3 — ваше смещение
//        }

        ((JCursesRenderer) renderer).drawMapWithFog(
                asciiMap,
                session.getPlayer(),
                fogOfWarService,
                levelGenerator
        );

        // Подсказка
        renderer.drawString(0, 29, "Use WASD to move, ESC to exit", CharColor.CYAN);
    }
}