package org.example.presentation;

//import org.example.domain.GameSession;
//import org.example.domain.service.CombatService;
//import org.example.domain.service.EnemyAIService;
//import org.example.domain.service.InventoryService;
//import org.example.domain.service.MovementService;
//import org.example.domain.service.FogOfWarService;
//import org.example.domain.model.InputCommand;
//import org.example.domain.model.Level;

import org.example.domain.entity.*;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.model.Level;
import org.example.domain.model.Position;
import org.example.domain.service.*;

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

    public GameLoop(GameSession session,
                    InputHandler inputHandler,
                    Renderer renderer,
                    CombatService combatService,
                    EnemyAIService enemyAIService,
                    InventoryService inventoryService,
                    MovementService movementService,
                    FogOfWarService fogOfWarService) {
        this.session = session;
        this.inputHandler = inputHandler;
        this.renderer = renderer;
        this.combatService = combatService;
        this.enemyAIService = enemyAIService;
        this.inventoryService = inventoryService;
        this.movementService = movementService;
        this.fogOfWarService = fogOfWarService;
    }

///**
// * Запускает игровой цикл. Блокирующий вызов.
// */
//public void start() {
//    session.start();
//    renderer.clearScreen();
//
//    // Главный цикл
//    while (session.isRunning() && !session.isGameOver()) {
//        // 1. RENDER - Отрисовать текущее состояние
//        render();
//
//        // 2. INPUT - Прочитать команду игрока
//        InputCommand command = inputHandler.readCommand();
//
//        // 3. UPDATE - Обработать команду и обновить мир
//        processCommand(command);
//
//        // 4. AI - Обновить врагов
//        updateEnemies();
//
//        // 5. EFFECTS - Обновить эффекты (эликсиры, туман войны)
//        updateEffects();
//    }
//
//    // Игра закончилась
//    renderer.drawGameOver(session.getStatistics());
//    renderer.shutdown();
//}
//
//private void render() {
//    Level level = session.getCurrentLevel();
//
//    // Карта с туманом войны
//    fogOfWarService.update(session.getPlayer(), level);
//    renderer.drawLevel(level, fogOfWarService);
//
//    // Враги (только если видны)
//    enemyAIService.getVisibleEnemies().forEach(renderer::drawEnemy);
//
//    // Игрок
//    renderer.drawPlayer(session.getPlayer());
//
//    // UI
//    renderer.drawStatusBar(session);
//    renderer.drawInventory(session.getPlayer().getInventory());
//
//    renderer.refresh();
//}
//
//private void processCommand(InputCommand command) {
//    if (command == null) return;
//
//    switch (command.getType()) {
//        case MOVE:
//            handleMoveCommand(command.getDirection());
//            break;
//        case USE_ITEM:
//            handleUseItemCommand(command.getItemType());
//            break;
//        case QUIT:
//            session.stop();
//            break;
//    }
//}
//
//private void handleMoveCommand(Direction direction) {
//    Player player = session.getPlayer();
//    Position currentPos = player.getPosition();
//    Position newPos = currentPos.move(direction);
//
//    // Проверка: можно ли туда идти?
//    if (movementService.canMoveTo(newPos, session.getCurrentLevel())) {
//        // Проверка: есть ли враг?
//        Enemy enemy = enemyAIService.getEnemyAt(newPos);
//        if (enemy != null) {
//            // Начать бой
//            combatService.playerAttacks(player, enemy);
//            if (enemy.isDead()) {
//                session.getEnemies().remove(enemy);
//                session.getStatistics().addDefeatedEnemy();
//            }
//        } else {
//            // Свободная клетка — переместиться
//            player.setPosition(newPos);
//            session.getStatistics().incrementSteps();
//
//            // Проверить, есть ли предмет на полу
//            Item item = session.getCurrentLevel().getItemAt(newPos);
//            if (item != null) {
//                inventoryService.tryPickupItem(player, item);
//            }
//        }
//    }
//}
//
//private void handleUseItemCommand(ItemType type) {
//    // Показать список предметов этого типа
//    Inventory inventory = session.getPlayer().getInventory();
//    if (inventory.count(type) == 0) return;
//
//    // Запросить индекс (1-9) у игрока
//    int index = inputHandler.readNumber() - 1;
//    Item item = inventoryService.useItem(session.getPlayer(), type, index);
//
//    // Применить эффект
//    if (item != null) {
//        item.applyTo(session.getPlayer());
//        renderer.drawMessage("Использовано: " + item.getName());
//    }
//}
//
//private void updateEnemies() {
//    // Для каждого врага: обновить ИИ
//    session.getEnemies().forEach(enemy -> {
//        enemyAIService.update(enemy, session);
//    });
//}
//
//private void updateEffects() {
//    // Обновить туман войны, эликсиры, эффекты врагов
//    fogOfWarService.update(session.getPlayer(), session.getCurrentLevel());
//}
}