package org.example.presentation;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.GameInitializer;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Player;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.model.Level;
import org.example.domain.model.Position;
import org.example.domain.service.CombatService;
import org.example.domain.service.EnemyAIService;
import org.example.domain.service.EnemyType;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.InventoryService;
import org.example.domain.service.LevelGenerator;
import org.example.domain.service.MovementService;
import org.example.App.GameResult;
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
    private  Level level;
    private final char[][] asciiMap;

    // Игрок  и его позиция (временно, пока не полностью перейдем на Player entity)
    private Player player;
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
        this.level = session.getLevel();
        this.asciiMap = level.getAsciiMap();

        // Инициализация игрока из сессии
        this.player = session.getPlayer();
        Position playerPos = player.getPosition();
        this.playerX = playerPos.getX();
        this.playerY = playerPos.getY();
        this.symbolUnderPlayer = asciiMap[playerY][playerX];
    }

    public GameResult start() {
        // Инициализация JCurses
        sun.misc.Signal.handle(new sun.misc.Signal("INT"), signal -> {
            renderer.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        renderer.clearScreen();
        System.out.print("\033[?25l");

        // 🔥 КРИТИЧЕСКО: первичное обновление тумана перед стартом
       // fogOfWarService.updateVisibility(session.getPlayer().getPosition(), asciiMap);

        boolean running = true;

        while (running) {
            // 1. РЕНДЕР: рисуем текущее состояние
//            renderer.clearScreen();
            drawMap(); // Рисуем карту с учетом тумана
            drawEnemies(); // Рисуем видимых врагов
            renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);
            renderer.refresh();

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
                    // Атакуем врага
                    combatService.attackEnemy(session, enemyAtPosition);
                    if (enemyAtPosition.getHealth() <= 0) {
                        combatService.removeEnemy(session, enemyAtPosition, asciiMap);
                    }
                    continue; // Ход завершен, переходим к следующей итерации
                }

                // Если можно двигаться - перемещаем
                if (canMoveTo(newX, newY)) {
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
            enemyAIService.moveEnemies(session, playerX, playerY, asciiMap);
            enemyAIService.updateEnemyEffects(session, playerX, playerY);
            drawEnemies(); // Перерисовываем врагов после их перемещения

            // Проверка смерти игрока
            if (player.getHealth() <= 0) {
                player.setAlive(false) ;
                session.setPlayer(player);
            }

            // TODO Проверка завершения уровня
//            if (Позиция игрока совпадает с "E") {
//                 currentLevel  = currentLevel++;
//                if (currentLevel > 21) { // Уровень 22 = победа
//                    return new GameResult(22, collectedTreasures, true, false);
//                }
//                generateNewLevel( с новым уровнем);
//            }

        }

        renderer.shutdown();

        // TODO Игрок умер
//        if (!player.isAlive()) {
//            return new GameResult(session.getLevel(), session.getTerasures,false, false);
//        }

        // Не должно сюда попадать
        //return new GameResult(currentLevel, collectedTreasures, false, false);
        //TODO Заглушка
        return new GameResult(1,1,true,true);
    }

//    private void movePlayer(Direction direction) {
//        int newX = playerX + direction.getDx();
//        int newY = playerY + direction.getDy();
//
//        Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
//        if (enemyAtPosition != null) {
//            combatService.attackEnemy(session, enemyAtPosition);
//            if (enemyAtPosition.getHealth() <= 0) {
//                combatService.removeEnemy(session, enemyAtPosition, asciiMap);
//            }
//            return;
//        }
//
//        if (canMoveTo(newX, newY)) {
//            // Запоминаем, что мы исследовали клетку, на которую встаём
//            fogOfWarService.markCellAsExplored(newX, newY);
//            // Обновляем локальные переменные
//            playerX = newX;
//            playerY = newY;
//            symbolUnderPlayer = asciiMap[playerY][playerX];
//
//            // ✅ Обновляем позицию в сущности через Direction
//            session.getPlayer().move(direction);
//        }
//    }

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

        renderer.drawMapWithFog(
                asciiMap,
                session.getPlayer(),
                fogOfWarService,
                level
        );

        // Подсказка
        renderer.drawString(0, 29, "Use WASD to move, ESC to exit", CharColor.CYAN);
    }

    private void syncPlayerPositionWithEntity() {
        Position pos = session.getPlayer().getPosition();
        this.playerX = pos.getX();
        this.playerY = pos.getY();
    }
}