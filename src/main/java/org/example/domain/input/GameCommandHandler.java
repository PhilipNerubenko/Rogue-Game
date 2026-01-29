package org.example.domain.input;

import org.example.config.GameConstants;
import org.example.domain.entity.*;
import org.example.domain.enums.Direction;
import org.example.domain.enums.ItemType;
import org.example.domain.interfaces.Renderer;
import org.example.domain.model.Position;
import org.example.domain.service.*;

import java.io.IOException;
import java.lang.Character;
import java.util.List;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.Colors.*;
import static org.example.config.GameConstants.ScreenConfig.DEATH_MESSAGE_X;
import static org.example.config.GameConstants.ScreenConfig.DEATH_MESSAGE_Y;
import static org.example.config.GameConstants.TextMessages.DIED;
import static org.example.config.GameConstants.TextMessages.MESSAGE_DURATION;
import static org.example.config.GameConstants.TextMessages.VICTORY;
import static org.example.config.GameConstants.Control.ESC_KEY_CODE;

public class GameCommandHandler {

    private final Renderer renderer;
    private final ItemSelectionState stateManager;
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final StatisticsService statisticsService;
    private final FogOfWarService fogOfWarService;
    private final Message message;

    private GameSession session;
    private char[][] asciiMap;
    private SessionStat currentSessionStat;

    public GameCommandHandler(Renderer renderer,
                              ItemSelectionState stateManager,
                              CombatService combatService,
                              EnemyAIService enemyAIService,
                              StatisticsService statisticsService,
                              FogOfWarService fogOfWarService, Message message) {
        this.renderer = renderer;
        this.stateManager = stateManager;
        this.combatService = combatService;
        this.enemyAIService = enemyAIService;
        this.statisticsService = statisticsService;
        this.fogOfWarService = fogOfWarService;
        this.message = message;
    }

    public void bindWorld(GameSession session, char[][] asciiMap, SessionStat stat) {
        this.session = session;
        this.asciiMap = asciiMap;
        this.currentSessionStat = stat;
    }

    public InputCommand processInput() {
        int keyCode = renderer.readCharacter();

        if (keyCode == ESC_KEY_CODE) {
            return handleEscapeKey();
        }

        if (stateManager.isAwaitingSelection()) {
            if (keyCode >= 48 && keyCode <= 57) {
                int index = Character.getNumericValue((char) keyCode);
                return InputCommand.selectIndex(index);
            }
            return InputCommand.none();
        }

        return handleMovementOrAction(keyCode);
    }

    private InputCommand handleEscapeKey() {
        if (stateManager.isAwaitingSelection()) {
            stateManager.resetAwaitingState();
            return InputCommand.none();
        }

        return InputCommand.quit();
    }

    private InputCommand handleMovementOrAction(int keyCode) {
        // Движение
        if (keyCode == 87 || keyCode == 119) return InputCommand.move(Direction.NORTH);
        if (keyCode == 83 || keyCode == 115) return InputCommand.move(Direction.SOUTH);
        if (keyCode == 65 || keyCode == 97) return InputCommand.move(Direction.WEST);
        if (keyCode == 68 || keyCode == 100) return InputCommand.move(Direction.EAST);

        // Использование предметов
        if (keyCode == 104 || keyCode == 72) return InputCommand.useItem(ItemType.WEAPON);
        if (keyCode == 106 || keyCode == 74) return InputCommand.useItem(ItemType.FOOD);
        if (keyCode == 107 || keyCode == 75) return InputCommand.useItem(ItemType.ELIXIR);
        if (keyCode == 101 || keyCode == 69) return InputCommand.useItem(ItemType.SCROLL);

        // Снятие оружия
        if (keyCode == 113 || keyCode == 81) return InputCommand.unequipWeapon();

        // Пропуск хода
        if (keyCode == 46 || keyCode == 32) return InputCommand.none();

        return InputCommand.none();
    }

    public boolean handleMovement(Direction dir) {
        try {
            Position pos = session.getPlayer().getPosition();
            int newX = pos.getX() + dir.getDx();
            int newY = pos.getY() + dir.getDy();

            if (!isValidMove(newX, newY)) return false;

            Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
            if (enemyAtPosition != null) {
                try {
                   message.setActiveMessageLine1(combatService.attackEnemy(session, enemyAtPosition, currentSessionStat, statisticsService));
                    message.setMessageTimer(MESSAGE_DURATION);
                    if (enemyAtPosition.getHealth() <= 0) {
                        renderer.removeEnemy(session, enemyAtPosition, asciiMap);
                        statisticsService.incrementEnemies(currentSessionStat);
                    }
                } catch (IOException e) {
                    message.setActiveMessageLine1("Error in combat!");
                    message.setMessageTimer(MESSAGE_DURATION);
                }
                return false;
            }

            char symbolAtNewPosition = asciiMap[newY][newX];

            // Подбор предмета
            if (isItemSymbol(symbolAtNewPosition)) {
                Item item = getItemAt(newX, newY);
                if (item != null) {
                    handleItemPickup(item, newX, newY);
                    session.getPlayer().move(dir);
                    return false;
                }
            }

            // Проверка выхода с уровня
            if (symbolAtNewPosition == 'E' || symbolAtNewPosition == EXIT) {
                try {
                    statisticsService.incrementMoves(currentSessionStat);
                } catch (IOException e) {
                    System.err.println("Ошибка обновления статистики ходов: " + e.getMessage());
                }
                return true;
            }

            // Перемещение
            session.getPlayer().move(dir);
            fogOfWarService.markCellAsExplored(newX, newY);

            try {
                statisticsService.incrementMoves(currentSessionStat);
            } catch (IOException e) {
                // игнорируем
            }

        } catch (Exception e) {
            message.setActiveMessageLine3("Error: " + e.getClass().getSimpleName());
            message.setMessageTimer(MESSAGE_DURATION);
        }
        return false;
    }

    public void handleUseItem(ItemType type) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        if (inventory.count(type) == 0) {
            message.setActiveMessageLine3("No " + type.name().toLowerCase() + " in inventory!");
            message.setMessageTimer(MESSAGE_DURATION);
            return;
        }

        stateManager.setAwaitingSelection(type);
        message.setActiveMessageLine3("Select " + type.name().toLowerCase() + " (1-9) or ESC to cancel");
        message.setMessageTimer(MESSAGE_DURATION);
    }

    public void handleItemSelection(int index) {
        if (!stateManager.isAwaitingSelection()) return;

        ItemType type = stateManager.getPendingItemType();

        try {
            boolean success;
            if (type == ItemType.WEAPON) {
                success = handleWeaponSelection(index);
            } else {
                success = handleConsumableSelection(type, index);
            }

            if (success) {
                // обновление статистики
                try {
                    switch (type) {
                        case FOOD -> statisticsService.incrementFood(currentSessionStat);
                        case ELIXIR -> statisticsService.incrementElixirs(currentSessionStat);
                        case SCROLL -> statisticsService.incrementScrolls(currentSessionStat);
                    }
                } catch (IOException ignored) {}

            }
        } finally {
            stateManager.resetAwaitingState();
        }
    }

    public boolean handleWeaponSelection(int index) {
        Player player = session.getPlayer();
        Inventory inv = player.getInventory();
        Item currentWeapon = player.getEquippedWeapon();

        if (index == 0) {
            // Снять текущее оружие (опция 0)
            if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
                Item droppedWeapon = player.unequipWeapon();

                Position playerPos = player.getPosition();
                Position freeCell = findFreeAdjacentCell(playerPos);

                if (freeCell != null) {
                    dropItemOnMap(droppedWeapon, freeCell);
                    message.setActiveMessageLine3("Weapon unequipped and dropped");
                } else {
                    message.setActiveMessageLine3("No space to drop weapon!");
                }

                message.setMessageTimer(MESSAGE_DURATION);
                return true;
            }
            return false;
        }

        // Экипировать новое оружие
        int itemIndex = index - 1;
        List<Item> weapons = inv.getItems(ItemType.WEAPON);
        if (itemIndex < 0 || itemIndex >= weapons.size()) return false;

        Item newWeapon = weapons.get(itemIndex);

        // Сначала снимаем старое оружие и кладем на пол
        if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
            Position playerPos = player.getPosition();
            Position freeCell = findFreeAdjacentCell(playerPos);

            if (freeCell != null) {
                dropItemOnMap(currentWeapon, freeCell);
            } else {
                // Если нет места, старое оружие пропадает
                message.setActiveMessageLine3("No space to drop old weapon, it disappears!");
            }
        }

        // Теперь экипируем новое оружие
        inv.take(ItemType.WEAPON, itemIndex); // Удаляем из инвентаря
        player.equip(newWeapon); // Экипируем

        message.setActiveMessageLine3("Equipped: " + newWeapon.getSubType());
        message.setMessageTimer(MESSAGE_DURATION);
        return true;
    }

    public boolean handleConsumableSelection(ItemType type, int index) {
        Player player = session.getPlayer();
        Inventory inv = player.getInventory();
        Item item = inv.take(type, index - 1);

        if (item == null) return false;

        player.applyItemEffects(item);
        message.setActiveMessageLine3("Used " + type.name().toLowerCase() + " (" + item.getSubType() + ")");
        message.setMessageTimer(MESSAGE_DURATION);
        return true;
    }

    public void handleUnequipWeapon() {
        Player player = session.getPlayer();
        Item droppedWeapon = player.unequipWeapon();

        if (droppedWeapon != null) {
            Position playerPos = player.getPosition();
            Position freeCell = findFreeAdjacentCell(playerPos);

            if (freeCell != null) {
                dropItemOnMap(droppedWeapon, freeCell);
                message.setActiveMessageLine3("Weapon dropped on the floor");
            } else {
                // Если нет свободной клетки, оружие пропадает
                message.setActiveMessageLine3("No space to drop weapon!");
            }
        } else {
            message.setActiveMessageLine3("No weapon to unequip");
        }

        message.setMessageTimer(MESSAGE_DURATION);
    }

    public void handleItemPickup(Item item, int x, int y) {
        Player player = session.getPlayer();
        Inventory inv = player.getInventory();

        if (item.getType().equalsIgnoreCase("treasure")) {
            inv.add(item);
            session.getCurrentLevelItems().remove(item);
            asciiMap[y][x] = GameConstants.Icons.FLOOR;
            message.setActiveMessageLine3("Picked up: " + item.getValue() + " gold");
            message.setMessageTimer(MESSAGE_DURATION);
            try {
                statisticsService.addTreasures(item.getValue(), currentSessionStat);
            } catch (IOException ignored) {}
            return;
        }

        ItemType type;
        try {
            type = ItemType.valueOf(item.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ItemType.TREASURE;
        }

        if (inv.isFull(type)) {
            message.setActiveMessageLine3(type.name() + " slot is full!");
            message.setMessageTimer(MESSAGE_DURATION);
            return;
        }

        inv.add(item);
        session.getCurrentLevelItems().remove(item);
        asciiMap[y][x] = GameConstants.Icons.FLOOR;
        message.setActiveMessageLine3("Picked up: " + item.getSubType());
        message.setMessageTimer(MESSAGE_DURATION);
    }

    public void handleDeath() throws IOException {
        renderer.clearScreen();
        renderer.drawMessage(DEATH_MESSAGE_X, DIED, COLOR_RED);
        renderer.readCharacter();
        statisticsService.addToScoreboard(currentSessionStat, session);
    }

    public void handleVictory() throws IOException {
        renderer.clearScreen();
        renderer.drawMessage(DEATH_MESSAGE_Y, VICTORY, COLOR_GREEN);
        renderer.readCharacter();
        statisticsService.addToScoreboard(currentSessionStat, session);
    }

    public boolean handleSleepTurn() throws IOException {
        Player player = session.getPlayer();
        Position pos = player.getPosition();

        message.setActiveMessageLine1("You are asleep... Zzz");
        message.setMessageTimer(MESSAGE_DURATION);

        List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                session, combatService, pos.getX(), pos.getY(), asciiMap);

        if (!enemyMessages.isEmpty()) {
            message.setActiveMessageLine2(String.join(", ", enemyMessages));
            message.setMessageTimer(MESSAGE_DURATION);
        }

        player.decrementSleep();

        if (player.getHealth() <= 0) {
            handleDeath();
            return false;
        }
        return true;
    }

    private boolean isItemSymbol(char symbol) {
        return symbol == FOOD || symbol == ELIXIR || symbol == SCROLL || symbol == WEAPON || symbol == TREASURES;
    }

    private Item getItemAt(int x, int y) {
        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() == x && item.getY() == y) return item;
        }
        return null;
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < GameConstants.Map.WIDTH &&
                y >= 0 && y < GameConstants.Map.HEIGHT &&
                asciiMap[y][x] != W_WALL &&
                asciiMap[y][x] != H_WALL &&
                asciiMap[y][x] != EMPTINESS;
    }

    /**
     * Проверяет наличие предмета на указанной позиции
     */
    private boolean isItemAtPosition(int x, int y) {
        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() == x && item.getY() == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Находит свободную соседнюю клетку для размещения предмета
     * @param playerPos позиция игрока
     * @return позиция свободной клетки или null, если не найдено
     */
    private Position findFreeAdjacentCell(Position playerPos) {
        // Проверяем соседние клетки (север, юг, запад, восток)
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int px = playerPos.getX();
        int py = playerPos.getY();

        for (int[] dir : directions) {
            int x = px + dir[0];
            int y = py + dir[1];

            // Проверяем границы и что клетка - пол (FLOOR)
            if (x >= 0 && x < GameConstants.Map.WIDTH &&
                    y >= 0 && y < GameConstants.Map.HEIGHT &&
                    asciiMap[y][x] == GameConstants.Icons.FLOOR &&
                    !isItemAtPosition(x, y)) {
                return new Position(x, y);
            }
        }
        return null; // не нашли свободной клетки
    }

    /**
     * Размещает предмет на карту
     * @param item предмет для размещения
     * @param pos позиция на карте
     */
    private void dropItemOnMap(Item item, Position pos) {
        int x = pos.getX();
        int y = pos.getY();

        // Устанавливаем координаты предмета
        item.setX(x);
        item.setY(y);

        // Добавляем в список предметов уровня
        session.getCurrentLevelItems().add(item);

        // Обновляем символ на карте
        char symbol = getSymbolForItem(item);
        asciiMap[y][x] = symbol;
    }

    /**
     * Возвращает символ для отображения предмета на карте
     * @param item предмет
     * @return символ ASCII для карты
     */
    private char getSymbolForItem(Item item) {
        return switch (item.getType().toLowerCase()) {
            case "weapon" -> GameConstants.Icons.WEAPON;
            case "food" -> GameConstants.Icons.FOOD;
            case "elixir" -> GameConstants.Icons.ELIXIR;
            case "scroll" -> GameConstants.Icons.SCROLL;
            case "treasure" -> GameConstants.Icons.TREASURES;
            default -> GameConstants.Icons.FLOOR; // на случай неизвестного типа
        };
    }
}
