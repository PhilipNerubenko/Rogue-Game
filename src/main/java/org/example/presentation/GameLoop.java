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
        this.session = initializer.getSession();
        this.inputHandler = initializer.getInputHandler();
        this.renderer = initializer.getRenderer();
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.movementService = initializer.getMovementService();
        this.inventoryService = initializer.getInventoryService();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap = initializer.getLevelGenerator().createAsciiMap(1);

        // Инициализация позиции игрока из сессии
        this.playerX = session.getPlayer().getPosition().getX();
        this.playerY = session.getPlayer().getPosition().getY();
        this.symbolUnderPlayer = asciiMap[playerY][playerX];
    }

    public void start() {
        // Инициализация JCurses (оставьте как есть)
        sun.misc.Signal.handle(new sun.misc.Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        Toolkit.init();
        System.out.print("\033[?25l");

        boolean running = true;

        while (running) {
            // Рисуем игрока
            Toolkit.printString(new String(new char[]{GameConstants.Icons.PLAYER}), playerX + 3, playerY,
                    new CharColor(CharColor.BLACK, CharColor.YELLOW));

            // Читаем КОМАНДУ (вместо прямого чтения клавиши)
            InputCommand command = inputHandler.readCommand();

            if (command.getType() == InputCommand.Type.QUIT) {
                running = false;
                continue;
            }

            // Затираем старое положение
            Toolkit.printString(new String(String.valueOf(symbolUnderPlayer)), playerX + 3, playerY,
                    new CharColor(CharColor.BLACK, CharColor.WHITE));

            // Обрабатываем команду
            if (command.getType() == InputCommand.Type.MOVE) {
                Direction dir = command.getDirection();
                movePlayer(dir.getDx(), dir.getDy());
            }

            // Обновление врагов (оставьте как есть)
            enemyAIService.moveEnemies(session, playerX, playerY, asciiMap);
            enemyAIService.updateEnemyEffects(session, playerX, playerY);
            drawEnemies();
        }

        Toolkit.shutdown();
        System.out.println("\nProgram finished normally.");
        System.out.print("\033[?25h");
    }

    private void movePlayer(int dx, int dy) {
        int newX = playerX + dx;
        int newY = playerY + dy;

        Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
        if (enemyAtPosition != null) {
            combatService.attackEnemy(session, enemyAtPosition);
            if (enemyAtPosition.getHealth() <= 0) {
                combatService.removeEnemy(session, enemyAtPosition, asciiMap);
            }
            return;
        }

        if (newX >= 0 && newX < GameConstants.Map.WIDTH &&
                newY >= 0 && newY < GameConstants.Map.HEIGHT &&
                asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~' &&
                asciiMap[newY][newX] != ' ') {

            playerX = newX;
            playerY = newY;
            symbolUnderPlayer = asciiMap[playerY][playerX];
        }
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

}