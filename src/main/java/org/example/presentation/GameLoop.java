package org.example.presentation;

import jcurses.system.CharColor;
import org.example.GameInitializer;
import org.example.config.GameConstants;
import org.example.datalayer.Statistics;
import org.example.domain.entity.*;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class GameLoop {

    private final GameSession session;
    private final InputHandler inputHandler;
    private final Renderer renderer;
    private boolean running = false;

    // Сервисы бизнес-логики
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final MovementService movementService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private char[][] asciiMap;

    // Позиция игрока
    private int playerX;
    private int playerY;
    private char symbolUnderPlayer;
    private char symbolAtNewPosition;

    // Сообщения
    private String activeMessageLine1;
    private String activeMessageLine2;
    private String activeMessageLine3;
    private int messageTimer = 0;
    private static final int MESSAGE_DURATION = 2;

    public GameLoop(GameInitializer initializer) {
        this.session = initializer.getSession();
        this.inputHandler = initializer.getInputHandler();
        this.renderer = initializer.getRenderer();
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.movementService = initializer.getMovementService();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];

        this.playerX = 0;
        this.playerY = 0;
        this.symbolAtNewPosition = symbolUnderPlayer;
    }

    public void start() throws IOException {
        generateNewLevel();
        syncPlayerPositionWithEntity();

        sun.misc.Signal.handle(new sun.misc.Signal(SIGINT_STRING), signal -> {
            renderer.shutdown();
            System.exit(0);
        });

        renderer.clearScreen();
        enemyAIService.updateAllGhostEffects(session, playerX, playerY);

        running = true;

        while (running) {
            if (messageTimer > 0) {
                messageTimer--;
            } else {
                activeMessageLine1 = null;
                activeMessageLine2 = null;
            }

            if (session.getPlayer().isSleepTurns()) {
                handleSleepTurn();
                continue;
            }

            // 1. РЕНДЕР
            renderer.clearScreen();
            drawMap();
            drawEnemies();
            renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);

            // Если ожидаем выбор предмета - рендерим меню поверх
            if (inputHandler.isAwaitingSelection()) {
                // Перерисовываем меню выбора
                redrawSelectionMenu();
            } else {
                // Обычный UI
                drawUI();
            }

            if (messageTimer > 0) {
                if (activeMessageLine1 != null) {
                    renderer.drawMessage(MESSAGE_LINE_1, activeMessageLine1, CharColor.YELLOW);
                }
                if (activeMessageLine2 != null) {
                    renderer.drawMessage(MESSAGE_LINE_2, activeMessageLine2, CharColor.YELLOW);
                }
            }

            // 2. ВВОД
            InputCommand command = inputHandler.readCommand();

            if (command.getType() == InputCommand.Type.QUIT) {
                running = false;
                continue;
            }

            // 3. ОБРАБОТКА
            switch (command.getType()) {
                case MOVE:
                    handleMovement(command.getDirection());
                    break;
                case USE_ITEM:
                    handleUseItem(command.getItemType());
                    break;
                case SELECT_INDEX:
                    handleItemSelection(command.getSelectedIndex());
                    break;
                case UNEQUIP_WEAPON:
                    handleUnequipWeapon();
                    break;
                default:
                    break;
            }

            // 4. ОБНОВЛЕНИЕ МИРА
            fogOfWarService.updateVisibility(session.getPlayer().getPosition(), asciiMap);

            List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                    session, combatService, playerX, playerY, asciiMap);
            if (!enemyMessages.isEmpty()) {
                activeMessageLine2 = String.join(", ", enemyMessages);
                messageTimer = MESSAGE_DURATION;
            }

            // Проверяем смерть игрока
            if (session.getPlayer().getHealth() <= 0) {
                handleDeath();
                running = false;
            }
        }

        renderer.shutdown();
    }

    private void handleDeath() throws IOException {
        renderer.drawMessage(DEATH_MESSAGE_Y, DIED, CharColor.RED);
        Statistics.updateScoreBoard();
    }

    private void handleVictory() throws IOException {
        renderer.drawMessage(DEATH_MESSAGE_Y, VICTORY, CharColor.GREEN);
        Statistics.updateScoreBoard();
    }

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

        // Если ожидаем выбор предмета - не рисуем обычный UI
        if (inputHandler.isAwaitingSelection()) {
            return;
        }
        // Отрисовка предметов
        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() >= 0 && item.getY() >= 0) {
                if (fogOfWarService.isVisible(item.getX(), item.getY())) {
                    char symbol = switch (item.getType()) {
                        case "food" -> ',';
                        case "elixir" -> '!';
                        case "scroll" -> '?';
                        case "weapon" -> ')';
                        case "treasure" -> '$';
                        default -> '*';
                    };
                    renderer.drawChar(item.getX(), item.getY(), symbol, CharColor.YELLOW);
                }
            }
        }

        // Подсказка

        String controls = "WASD:move | h:weapon | j:food | k:elixir | e:scroll | q:unequip | ESC:exit";
        renderer.drawString(3, 29, controls, CharColor.CYAN);

        // Статус Бар
        renderer.drawStatusBar(
                session.getPlayer().getHealth(),
                session.getPlayer().getMaxHealth(),
                session.getLevelNum(),
                session.getPlayer().getTreasureValue()
        );

        // Предметы на уровне
        int itemListY = 33;
        renderer.drawString(3, itemListY++, "=== ITEMS ON LEVEL ===", CharColor.CYAN);

        if (session.getCurrentLevelItems().isEmpty()) {
            renderer.drawString(5, itemListY++, "No items on this level", CharColor.WHITE);
        } else {
            int maxToShow = Math.min(5, session.getCurrentLevelItems().size());
            for (int i = 0; i < maxToShow; i++) {
                Item item = session.getCurrentLevelItems().get(i);
                String itemInfo = String.format("%d. %s at (%d,%d)",
                        i + 1,
                        getItemShortName(item),
                        item.getX(),
                        item.getY()
                );
                renderer.drawString(5, itemListY++, itemInfo, CharColor.WHITE);
            }

            if (session.getCurrentLevelItems().size() > 5) {
                renderer.drawString(5, itemListY,
                        "... and " + (session.getCurrentLevelItems().size() - 5) + " more",
                        CharColor.YELLOW);
            }
        }

        drawInventory();
    }

    private void syncPlayerPositionWithEntity() {
        Position pos = session.getPlayer().getPosition();
        this.playerX = pos.getX();
        this.playerY = pos.getY();
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
            handleVictory();
            running = false;
            return;
        }

        // Генерация карты
        char[][] newMap = levelGenerator.createAsciiMap(levelToGenerate);
        session.setCurrentMap(newMap);
        asciiMap = newMap;

        // Предметы из LevelGenerator
        session.getCurrentLevelItems().clear();
        session.getCurrentLevelItems().addAll(levelGenerator.getItems());

        // Находим стартовую позицию
        List<Room> rooms = levelGenerator.getRooms();
        for (Room room : rooms) {
            if (room.isStartRoom()) {
                playerX = room.getX1() + 2;
                playerY = room.getY1() + 2;
                break;
            }
        }

        Position newPlayerPosition = new Position(playerX, playerY);
        symbolUnderPlayer = asciiMap[playerY][playerX];
        session.getPlayer().setPosition(newPlayerPosition);

        // Очищаем и генерируем врагов
        session.getEnemies().clear();
        createEnemies();

        // Обновляем туман войны
        fogOfWarService.reset();
        fogOfWarService.markCellAsExplored(playerX, playerY);
        fogOfWarService.updateVisibility(newPlayerPosition, asciiMap);

        // Сообщение игроку
        activeMessageLine1 = "Level " + levelToGenerate;
        if (levelToGenerate > 1) {
            activeMessageLine2 = "You have gone deeper...";
        }
        messageTimer = MESSAGE_DURATION;
    }

    private void createEnemies() {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();

        int totalRoomsWithEnemies = calculateTotalRoomsWithEnemies(rooms.size(), rand);
        List<Room> shuffledRooms = new ArrayList<>(rooms);
        Collections.shuffle(shuffledRooms, rand);

        int enemiesPlaced = 0;

        for (Room room : shuffledRooms) {
            if (enemiesPlaced >= totalRoomsWithEnemies) break;
            if (room.isStartRoom()) continue;

            enemiesPlaced += createEnemiesInRoom(room, rand, session);
        }
    }

    private int calculateTotalRoomsWithEnemies(int totalRooms, Random rand) {
        int roomsWithEnemies = (int) Math.round(
                totalRooms * (MIN_ENEMY_DENSITY + rand.nextDouble() * DENSITY_RANGE)
        );
        return Math.max(MIN_ROOMS_WITH_ENEMIES, roomsWithEnemies);
    }

    private int createEnemiesInRoom(Room room, Random rand, GameSession session) {
        int enemiesCreated = 0;
        int enemiesInRoom = 1;

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

    private void handleSleepTurn() throws IOException {
        String sleepMsg = "You are sleep! Zzz...";
        session.getPlayer().setSleepTurns(false);
        renderer.drawMessage(UI_START_Y, sleepMsg, CharColor.CYAN);

        renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);

        List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                session, combatService, playerX, playerY, asciiMap);
        if (!enemyMessages.isEmpty()) {
            activeMessageLine2 = String.join(", ", enemyMessages);
            messageTimer = MESSAGE_DURATION;
        }

        renderer.clearScreen();
        drawMap();
        drawEnemies();
        renderer.drawChar(playerX, playerY, GameConstants.Icons.PLAYER, CharColor.YELLOW);
        drawUI();

        if (activeMessageLine2 != null) {
            renderer.drawMessage(MESSAGE_LINE_2, activeMessageLine2, CharColor.YELLOW);
        }

        if (session.getPlayer().getHealth() <= 0) {
            handleDeath();
            running = false;
        }
    }

    private void handleMovement(Direction dir) throws IOException {

        try {
            int newX = playerX + dir.getDx();
            int newY = playerY + dir.getDy();

            // Проверяем границы
            if (newX < 0 || newX >= GameConstants.Map.WIDTH ||
                    newY < 0 || newY >= GameConstants.Map.HEIGHT) {
                return;
            }

            // Проверяем символ на НОВОЙ позиции ПЕРЕД перемещением
            symbolAtNewPosition = asciiMap[newY][newX];

            // Проверяем, есть ли враг
            Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
            if (enemyAtPosition != null) {
                String message = combatService.attackEnemy(session, enemyAtPosition);
                activeMessageLine1 = message;
                messageTimer = MESSAGE_DURATION;

                if (enemyAtPosition.getHealth() <= 0) {
                    combatService.removeEnemy(session, enemyAtPosition, asciiMap);
                }
            } else if (canMoveTo(newX, newY)) {

                // ПЕРВОЕ: Проверяем предмет на новой клетке
                if (isItemSymbol(symbolAtNewPosition)) {
                    // ✅ Используем существующий метод getItemAt()
                    Item item = getItemAt(newX, newY);
                        // Подбираем предмет
                        handleItemPickup(item, newX, newY);

                        // После подбора игрок перемещается на эту клетку
                        // Затираем старую позицию
                        renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);

                        // Помечаем клетку как исследованную
                        fogOfWarService.markCellAsExplored(newX, newY);

                        // Обновляем позицию игрока
                        playerX = newX;
                        playerY = newY;
                        symbolUnderPlayer = '.'; // После подбора на клетке всегда пол

                        // Обновляем позицию в entity
                        session.getPlayer().move(dir);
                        return;
                }

                // ВТОРОЕ: Проверяем выход
                if (symbolAtNewPosition == 'E' || symbolAtNewPosition == EXIT) {
                    generateNewLevel();
                    return;
                }

                // ТРЕТЬЕ: Обычное перемещение (без предмета)
                // Затираем старую позицию
                renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);

                // Помечаем клетку как исследованную
                fogOfWarService.markCellAsExplored(newX, newY);

                // Обновляем локальные координаты
                playerX = newX;
                playerY = newY;
                symbolUnderPlayer = symbolAtNewPosition;

                // Синхронизируем с Player entity
                session.getPlayer().move(dir);
            } else {
            }
        } catch (Exception e) {
            System.err.println("ERROR in handleMovement: " + e.getMessage());
            e.printStackTrace();
            activeMessageLine3 = "Error: " + e.getClass().getSimpleName();
            messageTimer = MESSAGE_DURATION;
        }
    }


    private void handleUseItem(ItemType itemType) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        if (inventory.count(itemType) == 0) {
            activeMessageLine3 = "No " + itemType.name().toLowerCase() + " in inventory!";
            messageTimer = MESSAGE_DURATION;
            return;
        }

        // Просто переходим в режим ожидания выбора
        inputHandler.setAwaitingSelection(true, itemType);

        // Сообщение для пользователя
        activeMessageLine3 = "Select " + itemType.name().toLowerCase() + " (1-9) or ESC to cancel";
        messageTimer = MESSAGE_DURATION;
    }

    private void handleItemPickup(Item item, int x, int y) {

        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        // Для сокровищ - особый случай
        if (item.getType().equalsIgnoreCase("treasure")) {
            // Сокровища не имеют ограничений по количеству
            if (inventory.add(item)) {

                session.getCurrentLevelItems().remove(item);
                asciiMap[y][x] = '.';

                activeMessageLine3 = String.format("Picked up: %d gold", item.getValue());
                messageTimer = MESSAGE_DURATION;

                // Обновляем позицию игрока
                renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);
                fogOfWarService.markCellAsExplored(x, y);
                playerX = x;
                playerY = y;
                symbolUnderPlayer = asciiMap[playerY][playerX];
                session.getPlayer().setPosition(new Position(x, y));
            }
            return;
        }

        // Для обычных предметов
        ItemType type;
        try {
            type = ItemType.valueOf(item.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ItemType.TREASURE;
        }

        if (inventory.isFull(type)) {
            activeMessageLine3 = String.format("%s slot is full! Max %d per type.",
                    type.name(), GameConstants.Player.MAX_PER_TYPE);
            messageTimer = MESSAGE_DURATION;
            return;
        }

        if (inventory.add(item)) {
            session.getCurrentLevelItems().remove(item);
            asciiMap[y][x] = '.';

            activeMessageLine3 = String.format("Picked up: %s (%s)",
                    item.getSubType(), type.name().toLowerCase());
            messageTimer = MESSAGE_DURATION;

            // Обновляем позицию игрока
            renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);
            fogOfWarService.markCellAsExplored(x, y);
            playerX = x;
            playerY = y;
            symbolUnderPlayer = asciiMap[playerY][playerX];
            session.getPlayer().setPosition(new Position(x, y));
        } else {
            activeMessageLine3 = "Failed to add item to inventory";
            messageTimer = MESSAGE_DURATION;
        }
    }

    private void handleItemSelection(int index) {
        if (!inputHandler.isAwaitingSelection()) {
            return;
        }

        ItemType type = inputHandler.getPendingItemType();
        Player player = session.getPlayer();

        try {
            if (type == ItemType.WEAPON) {
                handleWeaponSelection(index);
            } else {
                handleConsumableSelection(type, index);
            }
        } catch (Exception e) {
            System.err.println("ERROR in handleItemSelection: " + e.getMessage());
            activeMessageLine3 = "Error using item: " + e.getMessage();
            messageTimer = MESSAGE_DURATION;
        } finally {
            inputHandler.resetAwaitingState();
        }
    }

    private void handleUnequipWeapon() {
        Player player = session.getPlayer();
        Item currentWeapon = player.getEquippedWeapon();

        if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
            player.unequipWeapon();
            activeMessageLine3 = "Weapon unequipped";
            messageTimer = MESSAGE_DURATION;
        } else {
            activeMessageLine3 = "No weapon equipped!";
            messageTimer = MESSAGE_DURATION;
        }
    }

    private void handleWeaponSelection(int index) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        if (index == 0) {
            // Снять оружие
            player.unequipWeapon();
            activeMessageLine3 = "Weapon unequipped";
            messageTimer = MESSAGE_DURATION;
            return;
        }

        // Индексы начинаются с 1 для пользователя
        int itemIndex = index - 1;
        List<Item> weapons = inventory.getItems(ItemType.WEAPON);

        if (itemIndex < 0 || itemIndex >= weapons.size()) {
            activeMessageLine3 = "Invalid weapon selection!";
            messageTimer = MESSAGE_DURATION;
            return;
        }

        Item weapon = weapons.get(itemIndex);
        player.equip(weapon);

        // Удаляем из инвентаря (так как теперь экипировано)
        inventory.take(ItemType.WEAPON, itemIndex);

        activeMessageLine3 = String.format("Equipped: %s (STR+%d)",
                weapon.getSubType(), weapon.getStrength());
        messageTimer = MESSAGE_DURATION;
    }

    private void handleConsumableSelection(ItemType type, int index) {
        Player player = session.getPlayer();

        // Индексы начинаются с 1 для пользователя
        int itemIndex = index ;

        List<Item> items = player.getInventory().getItems(type);
        if (itemIndex >= 0 && itemIndex < items.size()) {
            Item item = items.get(itemIndex);
        }

        // Используем предмет через метод игрока
        boolean success = player.useItem(type, itemIndex);

        if (success) {
            String itemName = type.name().toLowerCase();
            activeMessageLine3 = "Used " + itemName + " successfully!";
            messageTimer = MESSAGE_DURATION;

            // Обновляем статус игрока в UI
            updatePlayerStatus();
        } else {
            activeMessageLine3 = "Failed to use item!";
            messageTimer = MESSAGE_DURATION;
        }
    }

    private void updatePlayerStatus() {
        // Обновляем отображение статуса игрока
        Player player = session.getPlayer();
        renderer.drawStatusBar(
                player.getHealth(),
                player.getMaxHealth(),
                session.getLevelNum(),
                player.getTreasureValue()
        );
    }

    private boolean isItemSymbol(char symbol) {
        boolean result = symbol == ',' ||   // food
                symbol == '!' ||   // elixir
                symbol == '?' ||   // scroll
                symbol == ')' ||   // weapon
                symbol == '$';     // treasure
        return result;
    }

    private Item getItemAt(int x, int y) {

        for (Item item : session.getCurrentLevelItems()) {
            if (item.getX() == x && item.getY() == y) {
                return item;
            }
        }
        return null;
    }

    private String getItemShortName(Item item) {
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
            case WEAPON -> CharColor.RED;
            case FOOD -> CharColor.GREEN;
            case ELIXIR -> CharColor.BLUE;
            case SCROLL -> CharColor.MAGENTA;
            case TREASURE -> CharColor.YELLOW;
            default -> CharColor.WHITE;
        };
    }

    private void drawInventory() {
        int startY = 31;
        renderer.drawString(43, startY++, "=== INVENTORY ===", CharColor.CYAN);

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
            renderer.drawString(45, startY++, "Empty", CharColor.WHITE);
            return;
        }

        int treasureValue = inventory.getTreasureValue();
        if (treasureValue > 0) {
            renderer.drawString(45, startY++,
                    String.format("💰 Treasure: %d gold", treasureValue),
                    CharColor.YELLOW);
        }

        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            int count = inventory.count(type);
            if (count > 0) {
                String typeName = type.name().charAt(0) +
                        type.name().substring(1).toLowerCase() +
                        (count > 1 ? "s" : "");

                renderer.drawString(45, startY++,
                        String.format("%s: %d", typeName, count),
                        getItemTypeColor(type));

                List<Item> items = inventory.getItems(type);
                int itemsToShow = Math.min(2, items.size());
                for (int i = 0; i < itemsToShow; i++) {
                    Item item = items.get(i);
                    String itemInfo = formatItemInfo(item);
                    renderer.drawString(47, startY++, itemInfo, CharColor.WHITE);
                }

                if (items.size() > 2) {
                    renderer.drawString(47, startY++,
                            String.format("... and %d more", items.size() - 2),
                            CharColor.YELLOW);
                }
            }
        }

        Item equipped = player.getEquippedWeapon();
        if (equipped != null && !equipped.getSubType().equals("fists")) {
            renderer.drawString(43, startY++,
                    String.format("🗡️ Equipped: %s (STR+%d)",
                            equipped.getSubType(),
                            equipped.getStrength()),
                    CharColor.GREEN);
        }

        renderer.drawString(43, startY,
                String.format("Total items: %d", totalItems),
                CharColor.CYAN);
    }

    private String formatItemInfo(Item item) {
        List<String> effects = new ArrayList<>();

        if (item.getHealth() > 0) effects.add("HP+" + item.getHealth());
        if (item.getMaxHealth() > 0) effects.add("MaxHP+" + item.getMaxHealth());
        if (item.getAgility() > 0) effects.add("AGI+" + item.getAgility());
        if (item.getStrength() > 0) effects.add("STR+" + item.getStrength());
        if (item.getValue() > 0) effects.add("💰" + item.getValue());

        String effectsStr = effects.isEmpty() ? "" :
                " (" + String.join(", ", effects) + ")";

        return String.format("- %s%s", item.getSubType(), effectsStr);
    }

    private void redrawSelectionMenu() {
        ItemType pendingType = inputHandler.getPendingItemType();
        if (pendingType == null) return;

        // Очищаем область для меню
        for (int y = 5; y < 15; y++) {
            for (int x = 50; x < 80; x++) {
                renderer.drawChar(x, y, ' ', CharColor.BLACK);
            }
        }

        if (pendingType == ItemType.WEAPON) {
            showWeaponSelection();
        } else {
            showItemSelection(pendingType);
        }
    }

    private void showWeaponSelection() {
        // Используем правильные координаты (в пределах экрана 80x30)
        int menuX = 50;
        int menuY = 5;

        // Очищаем область меню
        for (int y = menuY; y < menuY + 10; y++) {
            for (int x = menuX; x < menuX + 25; x++) {
                renderer.drawChar(x, y, ' ', CharColor.BLACK);
            }
        }

        Player player = session.getPlayer();
        List<Item> weapons = player.getInventory().getItems(ItemType.WEAPON);

        // Рисуем рамку меню
        renderer.drawString(menuX, menuY, "╔══════════════════════╗", CharColor.YELLOW);
        renderer.drawString(menuX, menuY + 1, "║ Select weapon (0-9) ║", CharColor.YELLOW);
        renderer.drawString(menuX, menuY + 2, "╠══════════════════════╣", CharColor.YELLOW);

        int line = 3;
        renderer.drawString(menuX + 2, menuY + line++, "0. Unequip current", CharColor.WHITE);

        for (int i = 0; i < weapons.size() && i < 9; i++) {
            Item weapon = weapons.get(i);
            String text = String.format("%d. %s (STR+%d)",
                    i + 1, weapon.getSubType(), weapon.getStrength());
            renderer.drawString(menuX + 2, menuY + line++, text, CharColor.WHITE);
        }

        renderer.drawString(menuX, menuY + line, "╚══════════════════════╝", CharColor.YELLOW);
        renderer.drawString(menuX + 2, menuY + line + 1, "Press ESC to cancel", CharColor.CYAN);
    }

    private void showItemSelection(ItemType itemType) {
        //inputHandler.setAwaitingSelection(true, itemType);

        Player player = session.getPlayer();
        List<Item> items = player.getInventory().getItems(itemType);

        // Показываем список предметов
        StringBuilder message = new StringBuilder("Select " + itemType.name().toLowerCase() + " (1-9):\n");

        for (int i = 0; i < Math.min(items.size(), 9); i++) {
            Item item = items.get(i);
            message.append(String.format("%d. %s", i + 1, formatItemForSelection(item)));
            if (i < Math.min(items.size(), 9) - 1) {
                message.append("\n");
            }
        }

        String[] lines = message.toString().split("\n");
        for (int i = 0; i < Math.min(lines.length, 3); i++) {
            if (i == 0) {
                renderer.drawMessage(MESSAGE_LINE_1, lines[i], CharColor.YELLOW);
            } else {
                renderer.drawMessage(MESSAGE_LINE_1 + i, lines[i], CharColor.WHITE);
            }

            if (i == 0) {
                renderer.drawMessage(45, lines[i], CharColor.YELLOW);
            } else {
                renderer.drawMessage(45 + i, lines[i], CharColor.WHITE);
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