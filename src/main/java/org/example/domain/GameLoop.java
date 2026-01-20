package org.example.domain;

import jcurses.system.CharColor;
import org.example.domain.input.GameCommandHandler;
import org.example.config.GameConstants;
import org.example.domain.input.InputCommand;
import org.example.domain.entity.SessionStat;
import org.example.domain.service.StatisticsService;
import org.example.domain.interfaces.Renderer;
import org.example.domain.dto.VisibleMapDto;
import org.example.domain.entity.*;
import org.example.domain.enums.ItemType;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.domain.input.ItemSelectionState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.example.config.GameConstants.Colors.*;
import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.MESSAGE_DURATION;

public class GameLoop {

    private final GameSession session;
    private final AutosaveService autosaveService;
    private final StatisticsService statisticsService;
    private final ItemSelectionState itemSelectionState;
    private final GameCommandHandler gameCommandHandler;
    private final Renderer renderer;
    private final MapVisibilityService mapVisibilityService;
    private boolean running = false;

    // Сервисы бизнес-логики
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final Message message;
    private final LevelGenerator levelGenerator;
    private char[][] asciiMap;

    // Статистика текущей сессии
    private final SessionStat currentSessionStat;

    public GameLoop(GameInitializer initializer) {
        this.session = initializer.getSession();
        this.currentSessionStat = initializer.getSessionStat();
        this.renderer = initializer.getRenderer();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.autosaveService = initializer.getAutosaveService();
        this.statisticsService = initializer.getStatisticsService();
        this.itemSelectionState = initializer.getInputStateManager();
        this.gameCommandHandler = initializer.getGameInputManager();
        this.message = initializer.getMessage();
        this.mapVisibilityService = new MapVisibilityService(fogOfWarService);
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];
    }

    public void start() throws IOException {

        // Проверяем, была ли игра загружена
        if (session.getCurrentMap() == null || session.getPlayer() == null) {
            // Новая игра или неполная загрузка
            generateNewLevel();
        } else {
            // Игра была загружена, восстанавливаем состояние
            initializeLoadedGame();
        }

        gameCommandHandler.bindWorld(session, asciiMap, currentSessionStat);

        renderer.clearScreen();
        Position pos = getPlayerPosition();
        enemyAIService.updateAllGhostEffects(session, pos.getX(), pos.getY());

        running = true;

        while (running) {
            if (message.getMessageTimer() > 0) {
                message.setMessageTimer(message.getMessageTimer() - 1);
            } else {
                message.resetMessage();
            }

            if (session.getPlayer().isSleepTurns()) {
                gameCommandHandler.handleSleepTurn();
                continue;
            }

            // 1. РЕНДЕР
            renderer.clearScreen();
            drawMap();
            drawEnemies();
            renderer.drawChar(pos.getX(), pos.getY(), GameConstants.Icons.PLAYER, COLOR_YELLOW);

            // Если ожидаем выбор предмета - рендерим меню поверх
            if (itemSelectionState.isAwaitingSelection()) {
                // Перерисовываем меню выбора
                redrawSelectionMenu();
            } else {
                // Обычный UI
                drawUI();
            }

            if (message.getMessageTimer() > 0) {
                if (message.getActiveMessageLine1() != null) {
                    renderer.drawMessage(MESSAGE_LINE_1, message.getActiveMessageLine1(), COLOR_YELLOW);
                }
                if (message.getActiveMessageLine2() != null) {
                    renderer.drawMessage(MESSAGE_LINE_2, message.getActiveMessageLine2(), COLOR_YELLOW);
                }
                if (message.getActiveMessageLine3() != null) {
                    renderer.drawMessage(MESSAGE_LINE_3, message.getActiveMessageLine3(), COLOR_YELLOW);
                }
            }

            // 2. ВВОД
            InputCommand command = gameCommandHandler.processInput();

            if (command.getType() == InputCommand.Type.QUIT) {
                autosaveService.saveGame(session, currentSessionStat);
                running = false;
                continue;
            }

            // 3. ОБРАБОТКА
                switch (command.getType()) {
                    case MOVE:
                        boolean isExit = gameCommandHandler.handleMovement(command.getDirection());
                        if (isExit) {
                            generateNewLevel();
                        }
                        break;
                    case USE_ITEM:
                        gameCommandHandler.handleUseItem(command.getItemType());
                        break;
                    case SELECT_INDEX:
                        gameCommandHandler.handleItemSelection(command.getSelectedIndex());
                        break;
                    case UNEQUIP_WEAPON:
                        gameCommandHandler.handleUnequipWeapon();
                        break;
                    default:
                        break;
                }

            // 4. ОБНОВЛЕНИЕ МИРА
            fogOfWarService.updateVisibility(session.getPlayer().getPosition(), asciiMap);

            List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                    session, combatService, pos.getX(), pos.getY(), asciiMap);
            if (!enemyMessages.isEmpty()) {
                message.setActiveMessageLine2(String.join(", ", enemyMessages));
                message.setMessageTimer(MESSAGE_DURATION);
            }

            // Проверяем смерть игрока
            if (session.getPlayer().getHealth() <= 0) {
                gameCommandHandler.handleDeath();
                running = false;
            }
        }

        renderer.shutdown();
    }

    private Position getPlayerPosition() {
        return session.getPlayer().getPosition();
    }

    /**
     * Помечает всю карту как исследованную при загрузке игры
     */
    private void initializeLoadedGame() {
        // Проверка на null
        if (session.getCurrentMap() == null) {
            System.err.println("[GameLoop] ERROR: Saved map is null!");
            try {
                generateNewLevel();
            } catch (IOException e) {
                System.err.println("[GameLoop] ERROR generating new level: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // Копируем карту из сессии
        asciiMap = session.getCurrentMap();

        // Восстанавливаем LevelGenerator
        if (session.getRooms() != null) {
            levelGenerator.restoreFromGameState(
                    asciiMap,
                    session.getRooms(),
                    session.getCurrentLevelItems()
            );
        }

        // Устанавливаем позицию игрока
        Position playerPos = getPlayerPosition();
        if (playerPos == null) {
            try {
                generateNewLevel();
            } catch (IOException e) {
                System.err.println("[GameLoop] ERROR generating new level: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        int px = playerPos.getX();
        int py = playerPos.getY();

        // Проверяем границы позиции
        if (py < 0 || py >= asciiMap.length ||
                px < 0 || px >= asciiMap[py].length) {
            System.err.println("[GameLoop] ERROR: Player position out of bounds!");
            px = Math.max(0, Math.min(px, asciiMap[0].length - 1));
            py = Math.max(0, Math.min(py, asciiMap.length - 1));
            playerPos.setX(px);
            playerPos.setY(py);
        }

        // Обновляем туман войны для загруженной игры
        fogOfWarService.updateForLoadedGame(playerPos, asciiMap);

        // Принудительно добавляем стартовую комнату в исследованные (на случай если сохранение пустое)
        if (fogOfWarService.getAllExploredCells().isEmpty()) {
            if (session.getRooms() != null && !session.getRooms().isEmpty()) {
                Room startRoom = session.getRooms().getFirst();
                for (int x = startRoom.getX1(); x <= startRoom.getX2(); x++) {
                    for (int y = startRoom.getY1(); y <= startRoom.getY2(); y++) {
                        fogOfWarService.markCellAsExplored(x, y);
                    }
                }
            }
        }

        message.setActiveMessageLine1("Loaded game - Level " + session.getLevelNum());
        message.setMessageTimer(MESSAGE_DURATION);
    }

    private void generateNewLevel() throws IOException {
        int levelToGenerate;

        if (session.getCurrentMap() == null) {
            levelToGenerate = session.getLevelNum();
        } else {
            levelToGenerate = session.getLevelNum() + 1;
            session.setLevelNum(levelToGenerate);
        }

        // Проверка на победу
        if (levelToGenerate > 21) {
            gameCommandHandler.handleVictory();
            running = false;
            return;
        }

        // Генерация карты
        char[][] newMap = levelGenerator.createAsciiMap(levelToGenerate);
        session.setCurrentMap(newMap);
        asciiMap = newMap;

        gameCommandHandler.bindWorld(session, asciiMap, currentSessionStat);

        // Сохраняем комнаты в GameSession
        session.setRooms(levelGenerator.getRooms());

        // Предметы из LevelGenerator
        session.getCurrentLevelItems().clear();
        session.getCurrentLevelItems().addAll(levelGenerator.getItems());

        // Находим стартовую позицию
        List<Room> rooms = levelGenerator.getRooms();
        Position pos = getPlayerPosition();
        int px = pos.getX();
        int py = pos.getY();
        for (Room room : rooms) {
            if (room.isStartRoom()) {
                pos.setX(room.getX1() + 2);
                pos.setY(room.getY1() + 2);
                break;
            }
        }

        // Очищаем и генерируем врагов
        session.getEnemies().clear();
        List<Enemy> newEnemies = levelGenerator.generateEnemiesForSession(session, asciiMap);
        session.getEnemies().addAll(newEnemies);

        // Обновляем туман войны
        fogOfWarService.reset();
        fogOfWarService.markCellAsExplored(px, py);
        fogOfWarService.updateVisibility(getPlayerPosition(), asciiMap);

        if (levelToGenerate > 1) {
            message.setActiveMessageLine1("You have gone deeper...");
        }
        message.setMessageTimer(MESSAGE_DURATION);

        // Увеличиваем уровень в статистике
        if (levelToGenerate > 1) {
            statisticsService.incrementLevel(currentSessionStat);
        }
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
            case ZOMBIE -> COLOR_GREEN;
            case VAMPIRE -> COLOR_RED;
//            case GHOST -> COLOR_WHITE;
            case OGRE -> COLOR_YELLOW;
//            case SNAKE_MAGE ->  COLOR_WHITE;
            default -> COLOR_WHITE;
        };
    }

    private void drawMap() {
        if (asciiMap == null || asciiMap.length == 0) {
            System.err.println("ERROR: asciiMap is null or empty!");
            return;
        }

        try {
            VisibleMapDto visibleMap = mapVisibilityService.prepareVisibleMap(
                    asciiMap,
                    session.getPlayer()
            );

            renderer.drawMap(visibleMap);
        } catch (Exception e) {
            System.err.println("ERROR in drawMap: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawUI() {
        // Если ожидаем выбор предмета - не рисуем обычный UI
        if (itemSelectionState.isAwaitingSelection()) {
            return;
        }

        // Подсказка
        String controls = "WASD:move | h:weapon | j:food | k:elixir | e:scroll | q:unequip | ESC:save&exit";
        renderer.drawString(3, GameConstants.Map.HEIGHT + 4, controls, COLOR_CYAN);

        // Статус Бар
        renderer.drawStatusBar(
                session.getPlayer().getHealth(),
                session.getPlayer().getMaxHealth(),
                session.getPlayer().getPosition().getX(),
                session.getPlayer().getPosition().getY(),
                session.getLevelNum(),
                session.getPlayer().getTreasureValue()
        );

        // Предметы на уровне
        int itemLIstX = 84;
        int itemListY = 20;
        renderer.drawString(itemLIstX, itemListY++, "=== ITEMS ON LEVEL ===", COLOR_CYAN);

        if (session.getCurrentLevelItems().isEmpty()) {
            renderer.drawString(itemLIstX, itemListY++, "No items on this level", COLOR_WHITE);
        } else {
            int maxToShow = Math.min(5, session.getCurrentLevelItems().size());
            for (int i = 0; i < maxToShow; i++) {
                Item item = session.getCurrentLevelItems().get(i);
                String itemInfo = String.format("%d. %s at (%d,%d)",
                        i + 1,
                        getItemDisplayName(item),
                        item.getX(),
                        item.getY()
                );
                renderer.drawString(itemLIstX, itemListY++, itemInfo, COLOR_WHITE);
            }

            if (session.getCurrentLevelItems().size() > 5) {
                renderer.drawString(itemLIstX, itemListY++,
                        "... and " + (session.getCurrentLevelItems().size() - 5) + " more",
                        COLOR_YELLOW);
            }
        }

        drawInventory();
    }

    private String getItemDisplayName(Item item) {
        if (item == null) return "null";

        return switch (item.getType().toLowerCase()) {
            case "food" -> "Food";
            case "elixir" -> "Elixir";
            case "scroll" -> "Scroll";
            case "weapon" -> "Weapon";
            case "treasure" -> "Treasure";
            default -> item.getType();
        };
    }

    private short getItemTypeColor(ItemType type) {
        return switch (type) {
            case WEAPON -> COLOR_RED;
            case FOOD -> COLOR_GREEN;
            case ELIXIR -> COLOR_BLUE;
            case SCROLL -> COLOR_MAGENTA;
            case TREASURE -> COLOR_YELLOW;
            default -> COLOR_WHITE;
        };
    }

    private void drawInventory() {
        int startY = 0;
        renderer.drawString(84, startY++, "=== INVENTORY ===", COLOR_CYAN);

        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        boolean isEmpty = true;
        int totalItems = 0;

        for (ItemType type : ItemType.values()) {
            int count = inventory.count(type);
            totalItems += count;
            if (count > 0 && type != ItemType.TREASURE) {
                isEmpty = false;
            }
        }

        if (isEmpty && inventory.getTreasureValue() == 0) {
            renderer.drawString(86, startY++, "Empty", COLOR_WHITE);
            return;
        }

        int treasureValue = inventory.getTreasureValue();
        if (treasureValue > 0) {
            renderer.drawString(86, startY++,
                    String.format("Treasure: %d gold", treasureValue),
                    COLOR_YELLOW);
        }

        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            int count = inventory.count(type);
            if (count > 0) {
                String typeName = type.name().charAt(0) +
                        type.name().substring(1).toLowerCase() +
                        (count > 1 ? "s" : "");

                renderer.drawString(86, startY++,
                        String.format("%s: %d", typeName, count),
                        getItemTypeColor(type));

                List<Item> items = inventory.getItems(type);
                int itemsToShow = Math.min(2, items.size());
                for (int i = 0; i < itemsToShow; i++) {
                    Item item = items.get(i);
                    String itemInfo = formatItemInfo(item);
                    renderer.drawString(88, startY++, itemInfo, COLOR_WHITE);
                }

                if (items.size() > 2) {
                    renderer.drawString(88, startY++,
                            String.format("... and %d more", items.size() - 2),
                            COLOR_YELLOW);
                }
            }
        }

        Item equipped = player.getEquippedWeapon();
        if (equipped != null && !equipped.getSubType().equals("fists")) {
            renderer.drawString(84, startY++,
                    String.format("Weapon Equipped: %s (STR+%d)",
                            equipped.getSubType(),
                            equipped.getStrength()),
                    COLOR_GREEN);
        }

        renderer.drawString(84, startY,
                String.format("Total items: %d", totalItems),
                COLOR_CYAN);
    }

    private String formatItemInfo(Item item) {
        List<String> effects = new ArrayList<>();

        if (item.getHealth() > 0) effects.add("HP+" + item.getHealth());
        if (item.getMaxHealth() > 0) effects.add("MaxHP+" + item.getMaxHealth());
        if (item.getAgility() > 0) effects.add("AGI+" + item.getAgility());
        if (item.getStrength() > 0) effects.add("STR+" + item.getStrength());
        if (item.getValue() > 0) effects.add("Gold" + item.getValue());

        String effectsStr = effects.isEmpty() ? "" :
                " (" + String.join(", ", effects) + ")";

        return String.format("- %s%s", item.getSubType(), effectsStr);
    }

    private void redrawSelectionMenu() {
        ItemType pendingType = itemSelectionState.getPendingItemType();
        if (pendingType == null) return;

        // Очищаем область для меню (правый верхний угол)
        int menuX = 45;
        int menuY = 5;
        int menuWidth = 33;
        int menuHeight = 15;

        // Очищаем область
        for (int y = menuY; y < menuY + menuHeight; y++) {
            for (int x = menuX; x < menuX + menuWidth; x++) {
                renderer.drawChar(x, y, ' ', COLOR_BLACK);
            }
        }

        // Рисуем рамку меню
        String border = "+" + "-".repeat(menuWidth) + "+";
        renderer.drawString(menuX, menuY, border, COLOR_YELLOW);
        renderer.drawString(menuX, menuY + menuHeight - 1, border, COLOR_YELLOW);

        for (int y = menuY + 1; y < menuY + menuHeight - 1; y++) {
            renderer.drawChar(menuX, y, '|', COLOR_YELLOW);
            renderer.drawChar(menuX + menuWidth + 1, y, '|', COLOR_YELLOW);
        }

        // Заголовок
        String title = " Select " + pendingType.name().toLowerCase() + " ";
        int titleX = menuX + (menuWidth - title.length()) / 2;
        renderer.drawString(titleX, menuY + 1, title, COLOR_CYAN);

        if (pendingType == ItemType.WEAPON) {
            showWeaponSelectionMenu(menuX + 2, menuY + 3);
        } else {
            showConsumableSelectionMenu(pendingType, menuX + 2, menuY + 3);
        }

        // Подсказка
        renderer.drawString(menuX + 2, menuY + menuHeight - 2,
                "0-9: select, ESC: cancel", COLOR_WHITE);
    }

    private void showWeaponSelectionMenu(int x, int y) {
        Player player = session.getPlayer();
        List<Item> weapons = player.getInventory().getItems(ItemType.WEAPON);
        Item equippedWeapon = player.getEquippedWeapon();

        int line = y;

        // Опция 0: снять оружие
        String currentWeaponName = (equippedWeapon != null && !equippedWeapon.getSubType().equals("fists"))
                ? equippedWeapon.getSubType() : "fists";
        renderer.drawString(x, line++, String.format("0. Unequip (%s)", currentWeaponName), COLOR_WHITE);

        // Список оружия в инвентаре
        if (weapons.isEmpty()) {
            renderer.drawString(x, line++, "No weapons in inventory", COLOR_YELLOW);
        } else {
            for (int i = 0; i < weapons.size() && i < 9; i++) {
                Item weapon = weapons.get(i);
                String text = String.format("%d. %s (STR+%d)",
                        i + 1, weapon.getSubType(), weapon.getStrength());
                renderer.drawString(x, line++, text, COLOR_WHITE);
            }
        }
    }

    private void showConsumableSelectionMenu(ItemType type, int x, int y) {
        Player player = session.getPlayer();
        List<Item> items = player.getInventory().getItems(type);

        int line = y;

        if (items.isEmpty()) {
            renderer.drawString(x, line++, "No " + type.name().toLowerCase() + " in inventory", COLOR_YELLOW);
        } else {
            for (int i = 0; i < items.size() && i < 9; i++) {
                Item item = items.get(i);
                String text = String.format("%d. %s", i + 1, formatItemForSelection(item));
                renderer.drawString(x, line++, text, COLOR_WHITE);
            }
        }
    }

    private String formatItemForSelection(Item item) {
        List<String> effects = new ArrayList<>();

        if (item.getHealth() > 0) effects.add("HP+" + item.getHealth());
        if (item.getMaxHealth() > 0) effects.add("MaxHP+" + item.getMaxHealth());
        if (item.getAgility() > 0) effects.add("AGI+" + item.getAgility());
        if (item.getStrength() > 0 && !item.getType().equals("weapon")) {
            effects.add("STR+" + item.getStrength());
        }

        String effectsStr = effects.isEmpty() ? "" :
                " (" + String.join(", ", effects) + ")";

        return item.getSubType() + effectsStr;
    }
}