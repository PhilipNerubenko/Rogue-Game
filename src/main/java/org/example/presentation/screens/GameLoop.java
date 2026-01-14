package org.example.presentation.screens;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.application.GameInitializer;
import org.example.application.usecase.SaveGameUseCase;
import org.example.config.GameConstants;
import org.example.datalayer.entity.SessionStat;
import org.example.datalayer.service.StatisticsService;
import org.example.presentation.input.InputHandler;
import org.example.presentation.views.Renderer;
import org.example.domain.dto.VisibleMapDto;
import org.example.domain.entity.*;
import org.example.domain.enums.EnemyType;
import org.example.domain.enums.ItemType;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.enums.Direction;
import org.example.presentation.dto.InputCommand;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.presentation.input.InputStateManager;

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
    private final InputStateManager inputStateManager;
    private final SaveGameUseCase saveGameUseCase;
    private final Renderer renderer;
    private final MapVisibilityService mapVisibilityService;
    private boolean running = false;

    // Сервисы бизнес-логики
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private char[][] asciiMap;

    // Позиция игрока
    private char symbolUnderPlayer;

    // Сообщения
    private String activeMessageLine1;
    private String activeMessageLine2;
    private String activeMessageLine3;
    private int messageTimer = 0;
    private static final int MESSAGE_DURATION = 2;

    // Статистика текущей сессии
    private final SessionStat currentSessionStat;


    public GameLoop(GameInitializer initializer) {
        this.session = initializer.getSession();
        this.currentSessionStat = initializer.getSessionStat();
        this.inputStateManager = initializer.getInputStateManager();
        this.saveGameUseCase = initializer.getSaveGameUseCase();
        this.renderer = initializer.getRenderer();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.mapVisibilityService = new MapVisibilityService(fogOfWarService);
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.inputHandler = initializer.getInputHandler();
        this.asciiMap = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];
    }

    private Position getPlayerPosition() {
        return session.getPlayer().getPosition();
    }

    public void start() throws IOException {

        // Проверяем, была ли игра загружена
        if (session.getCurrentMap() == null || session.getPlayer() == null) {
            // Новая игра или неполная загрузка
            generateNewLevel();
        } else {
            // Игра была загружена, восстанавливаем состояние
            restoreLoadedGame();
        }

        sun.misc.Signal.handle(new sun.misc.Signal(SIGINT_STRING), signal -> {
            renderer.shutdown();
            System.exit(0);
        });

        renderer.clearScreen();
        Position pos = getPlayerPosition();
        enemyAIService.updateAllGhostEffects(session, pos.getX(), pos.getY());

        running = true;

        while (running) {
            if (messageTimer > 0) {
                messageTimer--;
            } else {
                activeMessageLine1 = null;
                activeMessageLine2 = null;
                activeMessageLine3 = null;
            }

            if (session.getPlayer().isSleepTurns()) {
                handleSleepTurn();
                continue;
            }

            // 1. РЕНДЕР
            renderer.clearScreen();
            drawMap();
            drawEnemies();
            renderer.drawChar(pos.getX(), pos.getY(), GameConstants.Icons.PLAYER, CharColor.YELLOW);

            // Если ожидаем выбор предмета - рендерим меню поверх
            if (inputStateManager.isAwaitingSelection()) {
                // Перерисовываем меню выбора
                redrawSelectionMenu();
            } else {
                // Обычный UI
                drawUI();
            }

            if (messageTimer > 0) {
                if (activeMessageLine1 != null) {
                    renderer.drawMessage(MESSAGE_LINE_1 , activeMessageLine1, CharColor.YELLOW);
                }
                if (activeMessageLine2 != null) {
                    renderer.drawMessage(MESSAGE_LINE_2, activeMessageLine2, CharColor.YELLOW);
                }
                if (activeMessageLine3 != null) {
                    renderer.drawMessage(MESSAGE_LINE_3, activeMessageLine3, CharColor.YELLOW);
                }
            }

            // 2. ВВОД
            InputCommand command = inputHandler.readCommand();

            if (command.getType() == InputCommand.Type.QUIT) {
                saveGameUseCase.saveGame(session, currentSessionStat);
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
                    session, combatService, pos.getX(), pos.getY(), asciiMap);
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



    /**
     * Помечает всю карту как исследованную при загрузке игры
     */
    private void restoreLoadedGame() {
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
            session.getPlayer().setPosition(new Position(px, py));
        }

        symbolUnderPlayer = asciiMap[py][px];

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

        activeMessageLine1 = "Loaded game - Level " + session.getLevelNum();
        messageTimer = MESSAGE_DURATION;
    }

   private void handleDeath() throws IOException {
        renderer.clearScreen();
        renderer.drawString(DEATH_MESSAGE_X, DEATH_MESSAGE_Y, DIED, CharColor.RED);
        Toolkit.readCharacter();
        // Передаем gameSession в updateScoreBoard
        StatisticsService.updateScoreBoard(session, currentSessionStat);
    }

    private void handleVictory() throws IOException {
        renderer.drawMessage(DEATH_MESSAGE_Y, VICTORY, CharColor.GREEN);
        // Передаем gameSession в updateScoreBoard
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        StatisticsService.updateScoreBoard(session, currentSessionStat);
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
        if (inputStateManager.isAwaitingSelection()) {
            return;
        }

        // Подсказка
        String controls = "WASD:move | h:weapon | j:food | k:elixir | e:scroll | q:unequip | ESC:save&exit";
        renderer.drawString(3, GameConstants.Map.HEIGHT + 4, controls, CharColor.CYAN);

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
        renderer.drawString(itemLIstX, itemListY++, "=== ITEMS ON LEVEL ===", CharColor.CYAN);

        if (session.getCurrentLevelItems().isEmpty()) {
            renderer.drawString(itemLIstX, itemListY++, "No items on this level", CharColor.WHITE);
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
                renderer.drawString(itemLIstX, itemListY++, itemInfo, CharColor.WHITE);
            }

            if (session.getCurrentLevelItems().size() > 5) {
                renderer.drawString(itemLIstX, itemListY++,
                        "... and " + (session.getCurrentLevelItems().size() - 5) + " more",
                        CharColor.YELLOW);
            }
        }

        drawInventory();
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
                px = room.getX1() + 2;
                py = room.getY1() + 2;
                break;
            }
        }

        Position newPlayerPosition = new Position(px, py);
        symbolUnderPlayer = asciiMap[py][px];
        session.getPlayer().setPosition(newPlayerPosition);

        // Очищаем и генерируем врагов
        session.getEnemies().clear();
        createEnemies();

        // Обновляем туман войны
        fogOfWarService.reset();
        fogOfWarService.markCellAsExplored(px, py);
        fogOfWarService.updateVisibility(newPlayerPosition, asciiMap);

        // Сообщение игроку
//        activeMessageLine1 = "Level " + levelToGenerate;
        if (levelToGenerate > 1) {
            activeMessageLine2 = "You have gone deeper...";
        }
        messageTimer = MESSAGE_DURATION;

        // Увеличиваем уровень в статистике
        if (levelToGenerate > 1) {
            currentSessionStat.incrementLevel();
        }
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

            // Более Злые монстры с ростом уровня.
            Enemy enemy = randomType.create(session.getLevelNum());
            enemy.setX(enemyX);
            enemy.setY(enemyY);

            session.getEnemies().add(enemy);
            enemiesCreated++;
        }

        return enemiesCreated;
    }

    private void handleSleepTurn() throws IOException {
        activeMessageLine1 = "You are asleep... Zzz";
        messageTimer = MESSAGE_DURATION;
        Position pos = getPlayerPosition();

        List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                session, combatService, pos.getX(), pos.getY(), asciiMap);

        if (!enemyMessages.isEmpty()) {
            activeMessageLine2 = String.join(", ", enemyMessages);
            messageTimer = MESSAGE_DURATION;
        }

        session.getPlayer().decrementSleep();

        if (session.getPlayer().getHealth() <= 0) {
            handleDeath();
            running = false;
        }
    }

    private void handleMovement(Direction dir) {
        try {
            Position pos = getPlayerPosition();
            int newX = pos.getX() + dir.getDx();
            int newY = pos.getY() + dir.getDy();

            // Проверяем границы
            if (newX < 0 || newX >= GameConstants.Map.WIDTH ||
                    newY < 0 || newY >= GameConstants.Map.HEIGHT) {
                return;
            }

            // Проверяем символ на НОВОЙ позиции ПЕРЕД перемещением
            char symbolAtNewPosition = asciiMap[newY][newX];

            // Проверяем, есть ли враг
            Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
            if (enemyAtPosition != null) {
                // Блок try-catch для обработки IOException
                try {
                    activeMessageLine1 = combatService.attackEnemy(session, enemyAtPosition, currentSessionStat);
                    messageTimer = MESSAGE_DURATION;

                    if (enemyAtPosition.getHealth() <= 0) {
                        combatService.removeEnemy(session, enemyAtPosition, asciiMap);
                        currentSessionStat.incrementEnemies();
                    }
                } catch (IOException e) {
                    System.err.println("ERROR updating enemy stats: " + e.getMessage());
                    activeMessageLine1 = "Error in combat!";
                    messageTimer = MESSAGE_DURATION;
                }
            } else if (canMoveTo(newX, newY)) {

                // ПЕРВОЕ: Проверяем предмет на новой клетке
                if (isItemSymbol(symbolAtNewPosition)) {
                    Item item = getItemAt(newX, newY);
                    if (item != null) {
                        // Подбираем предмет
                        handleItemPickup(item, newX, newY);

                        // После подбора игрок перемещается на эту клетку
                        // Затираем старую позицию
                        renderer.drawChar(pos.getX(), pos.getY(), symbolUnderPlayer, CharColor.WHITE);

                        // Помечаем клетку как исследованную
                        fogOfWarService.markCellAsExplored(newX, newY);

                        // Обновляем позицию игрока
                        pos.setX(newX);
                        pos.setY(newY);
                        symbolUnderPlayer = '.'; // После подбора на клетке всегда пол

                        // Обновляем позицию в entity
                        session.getPlayer().move(dir);
                        try {
                            currentSessionStat.incrementMoves();
                        } catch (IOException e) {
                            System.err.println("ERROR updating move stats: " + e.getMessage());
                        }
                        return;
                    }
                }

                // ВТОРОЕ: Проверяем выход
                if (symbolAtNewPosition == 'E' || symbolAtNewPosition == EXIT) {
                    generateNewLevel();
                    try {
                        currentSessionStat.incrementMoves();
                    } catch (IOException e) {
                        System.err.println("ERROR updating move stats: " + e.getMessage());
                    }
                    return;
                }

                // ТРЕТЬЕ: Обычное перемещение (без предмета)
                // Затираем старую позицию
                renderer.drawChar(pos.getX(), pos.getY(), symbolUnderPlayer, CharColor.WHITE);

                // Помечаем клетку как исследованную
                fogOfWarService.markCellAsExplored(newX, newY);

                // Обновляем локальные координаты
                pos.setX(newX);
                pos.setY(newY);
                symbolUnderPlayer = symbolAtNewPosition;

                // Синхронизируем с Player entity
                session.getPlayer().move(dir);
                try {
                    currentSessionStat.incrementMoves();
                } catch (IOException e) {
                    System.err.println("ERROR updating move stats: " + e.getMessage());
                }
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
        inputStateManager.setAwaitingSelection(itemType);

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
        } else {
            activeMessageLine3 = "Failed to add item to inventory";
        }
        messageTimer = MESSAGE_DURATION;
    }

    private void handleItemSelection(int index) {
        if (!inputStateManager.isAwaitingSelection()) {
            return;
        }

        ItemType type = inputStateManager.getPendingItemType();

        try {
            boolean success;

            if (type == ItemType.WEAPON) {
                success = handleWeaponSelection(index);
            } else {
                success = handleConsumableSelection(type, index);
            }

            if (success) {
                // Обновляем статистику с обработкой исключений
                try {
                    switch (type) {
                        case FOOD -> currentSessionStat.incrementFood();
                        case ELIXIR -> currentSessionStat.incrementElixirs();
                        case SCROLL -> currentSessionStat.incrementScrolls();
                        default -> {}
                    }
                } catch (IOException e) {
                    System.err.println("Statistics update failed: " + e.getMessage());
                }

                // Обновляем статус игрока
                updatePlayerStatus();
            }

        } catch (Exception e) {
            System.err.println("ERROR in handleItemSelection: " + e.getMessage());
            activeMessageLine3 = "Error using item: " + e.getMessage();
            messageTimer = MESSAGE_DURATION;
        } finally {
            inputStateManager.resetAwaitingState();
        }
    }

    private void handleUnequipWeapon() {
        Player player = session.getPlayer();
        Item currentWeapon = player.getEquippedWeapon();

        if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
            // Просто вызываем unequipWeapon - он сам добавит оружие в инвентарь
            player.unequipWeapon();
            activeMessageLine3 = "Weapon unequipped and added to inventory";
        } else {
            activeMessageLine3 = "No weapon equipped!";
        }
        messageTimer = MESSAGE_DURATION;
    }

    private boolean handleWeaponSelection(int index) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        if (index == 0) {
            // Снять оружие
            Item currentWeapon = player.getEquippedWeapon();
            if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
                // НЕ добавляем в инвентарь здесь - это сделает player.unequipWeapon()
                player.unequipWeapon();
                activeMessageLine3 = "Weapon unequipped and added to inventory";
                messageTimer = MESSAGE_DURATION;
                return true;
            } else {
                activeMessageLine3 = "No weapon equipped!";
                messageTimer = MESSAGE_DURATION;
                return false;
            }
        }

        // Индексы начинаются с 1 для пользователя
        int itemIndex = index - 1;  // Преобразуем в индекс массива
        List<Item> weapons = inventory.getItems(ItemType.WEAPON);

        if (itemIndex < 0 || itemIndex >= weapons.size()) {
            activeMessageLine3 = "Invalid weapon selection!";
            messageTimer = MESSAGE_DURATION;
            return false;
        }

        Item weapon = weapons.get(itemIndex);

        // Если уже экипировано оружие, снимаем его
        Item currentWeapon = player.getEquippedWeapon();
        if (currentWeapon != null && !currentWeapon.getSubType().equals("fists")) {
            // Снимаем текущее оружие
            player.unequipWeapon(); // Это добавит оружие в инвентарь
        }

        // Экипируем новое оружие
        player.equip(weapon);

        // Удаляем из инвентаря (так как теперь экипировано)
        inventory.take(ItemType.WEAPON, itemIndex);

        activeMessageLine3 = String.format("Equipped: %s (STR+%d)",
                weapon.getSubType(), weapon.getStrength());
        messageTimer = MESSAGE_DURATION;
        return true;
    }

    private boolean handleConsumableSelection(ItemType type, int index) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        // Индексы начинаются с 1 для пользователя
        int itemIndex = index - 1; // Преобразуем в индекс массива

        // Получаем предмет из инвентаря
        Item item = inventory.take(type, itemIndex);

        if (item == null) {
            activeMessageLine3 = "No item at selected position!";
            messageTimer = MESSAGE_DURATION;
            return false;
        }

        // Применяем эффекты предмета
        player.applyItemEffects(item);

        String itemName = type.name().toLowerCase();
        activeMessageLine3 = "Used " + itemName + " (" + item.getSubType() + ") successfully!";
        messageTimer = MESSAGE_DURATION;

        return true;
    }

    private boolean isItemSymbol(char symbol) {
        return symbol == ',' ||   // food
                symbol == '!' ||   // elixir
                symbol == '?' ||   // scroll
                symbol == ')' ||   // weapon
                symbol == '$';     // treasure
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
        int startY = 0;
        renderer.drawString(84, startY++, "=== INVENTORY ===", CharColor.CYAN);

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
            renderer.drawString(86, startY++, "Empty", CharColor.WHITE);
            return;
        }

        int treasureValue = inventory.getTreasureValue();
        if (treasureValue > 0) {
            renderer.drawString(86, startY++,
                    String.format("Treasure: %d gold", treasureValue),
                    CharColor.YELLOW);
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
                    renderer.drawString(88, startY++, itemInfo, CharColor.WHITE);
                }

                if (items.size() > 2) {
                    renderer.drawString(88, startY++,
                            String.format("... and %d more", items.size() - 2),
                            CharColor.YELLOW);
                }
            }
        }

        Item equipped = player.getEquippedWeapon();
        if (equipped != null && !equipped.getSubType().equals("fists")) {
            renderer.drawString(84, startY++,
                    String.format("Weapon Equipped: %s (STR+%d)",
                            equipped.getSubType(),
                            equipped.getStrength()),
                    CharColor.GREEN);
        }

        renderer.drawString(84, startY,
                String.format("Total items: %d", totalItems),
                CharColor.CYAN);
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
        ItemType pendingType = inputStateManager.getPendingItemType();
        if (pendingType == null) return;

        // Очищаем область для меню (правый верхний угол)
        int menuX = 45;
        int menuY = 5;
        int menuWidth = 33;
        int menuHeight = 15;

        // Очищаем область
        for (int y = menuY; y < menuY + menuHeight; y++) {
            for (int x = menuX; x < menuX + menuWidth; x++) {
                renderer.drawChar(x, y, ' ', CharColor.BLACK);
            }
        }

        // Рисуем рамку меню
        String border = "+" + "-".repeat(menuWidth ) + "+";
        renderer.drawString(menuX, menuY, border, CharColor.YELLOW);
        renderer.drawString(menuX, menuY + menuHeight - 1, border, CharColor.YELLOW);

        for (int y = menuY + 1; y < menuY + menuHeight -1; y++) {
            renderer.drawChar(menuX, y, '|', CharColor.YELLOW);
            renderer.drawChar(menuX + menuWidth + 1, y, '|', CharColor.YELLOW);
        }

        // Заголовок
        String title = " Select " + pendingType.name().toLowerCase() + " ";
        int titleX = menuX + (menuWidth - title.length()) / 2;
        renderer.drawString(titleX, menuY + 1, title, CharColor.CYAN);

        if (pendingType == ItemType.WEAPON) {
            showWeaponSelectionMenu(menuX + 2, menuY + 3);
        } else {
            showConsumableSelectionMenu(pendingType, menuX + 2, menuY + 3);
        }

        // Подсказка
        renderer.drawString(menuX + 2, menuY + menuHeight - 2,
                "0-9: select, ESC: cancel", CharColor.WHITE);
    }

    private void showWeaponSelectionMenu(int x, int y) {
        Player player = session.getPlayer();
        List<Item> weapons = player.getInventory().getItems(ItemType.WEAPON);
        Item equippedWeapon = player.getEquippedWeapon();

        int line = y;

        // Опция 0: снять оружие
        String currentWeaponName = (equippedWeapon != null && !equippedWeapon.getSubType().equals("fists"))
                ? equippedWeapon.getSubType() : "fists";
        renderer.drawString(x, line++, String.format("0. Unequip (%s)", currentWeaponName), CharColor.WHITE);

        // Список оружия в инвентаре
        if (weapons.isEmpty()) {
            renderer.drawString(x, line++, "No weapons in inventory", CharColor.YELLOW);
        } else {
            for (int i = 0; i < weapons.size() && i < 9; i++) {
                Item weapon = weapons.get(i);
                String text = String.format("%d. %s (STR+%d)",
                        i + 1, weapon.getSubType(), weapon.getStrength());
                renderer.drawString(x, line++, text, CharColor.WHITE);
            }
        }
    }

    private void showConsumableSelectionMenu(ItemType type, int x, int y) {
        Player player = session.getPlayer();
        List<Item> items = player.getInventory().getItems(type);

        int line = y;

        if (items.isEmpty()) {
            renderer.drawString(x, line++, "No " + type.name().toLowerCase() + " in inventory", CharColor.YELLOW);
        } else {
            for (int i = 0; i < items.size() && i < 9; i++) {
                Item item = items.get(i);
                String text = String.format("%d. %s", i + 1, formatItemForSelection(item));
                renderer.drawString(x, line++, text, CharColor.WHITE);
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

    private void updatePlayerStatus() {
        // Обновляем отображение статуса игрока
        Player player = session.getPlayer();
        renderer.drawStatusBar(
                player.getHealth(),
                player.getMaxHealth(),
                player.getPosition().getX(),
                player.getPosition().getY(),
                session.getLevelNum(),
                player.getTreasureValue()
        );
    }
}