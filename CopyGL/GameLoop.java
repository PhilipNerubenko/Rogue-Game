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
import static org.example.config.GameConstants.Icons.OGRE;
import static org.example.config.GameConstants.Icons.SNAKE_MAGE;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

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
    private  boolean running = false;

    // Сервисы бизнес-логики (внедряются извне)
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final MovementService movementService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private  char[][] asciiMap;

    // Позиция игрока (временно, пока не полностью перейдем на Player entity)
    private int playerX;
    private int playerY;
    private char symbolUnderPlayer;
    private char symbolAtNewPosition;
    int newX ;
    int newY ;

    // сообщения и задержка сообщений
    private String activeMessageLine1;
    private String activeMessageLine2;
    private String activeMessageLine3;

    private int messageTimer = 0; // начальный таймер
    private static final int MESSAGE_DURATION = 2; // время жизни сообщения

    public GameLoop(GameInitializer initializer) {
        // Извлекаем зависимости из инициализатора
        this.session = initializer.getSession();
        this.inputHandler = initializer.getInputHandler();
        this.renderer = initializer.getRenderer();
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.movementService = initializer.getMovementService();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap  = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];

        // Инициализация позиции игрока из сессии
        Position playerPos = session.getPlayer().getPosition();
        this.playerX = 0;
        this.playerY = 0;
        this.newX = playerX;
        this.newY = playerY;
        this.symbolAtNewPosition = symbolUnderPlayer;

        //this.symbolUnderPlayer = asciiMap[playerY][playerX];
    }

    public void start() throws IOException {

        // Генерируем ПЕРВЫЙ уровень
        generateNewLevel();

        // 2. Синхронизируем позицию игрока после генерации уровня
        syncPlayerPositionWithEntity();

        // Инициализация JCurses
        sun.misc.Signal.handle(new sun.misc.Signal(SIGINT_STRING), signal -> {
            renderer.shutdown();
            System.out.println(TERMINATE);
            System.exit(0);
        });

        renderer.clearScreen();

        enemyAIService.updateAllGhostEffects(session, playerX, playerY);
        System.out.print(HIDE_CURSOR);

        running = true;

        while (running) {
            // Уменьшаеми таймер сообщений
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
                continue; // Пропускаем остальную обработку ввода
            }



            // 3. ОБРАБОТКА: применяем команду
            switch (command.getType()) {
                case MOVE:
                    handleMovement(command.getDirection());
                    break;

                case USE_ITEM:
                    // Обработка использования предмета (например, нажатие h, j, k, e)
                    handleUseItem(command.getItemType());
                    break;
                case SELECT_INDEX:
                    // Обработка выбора предмета из инвентаря
                    handleItemSelection(command.getSelectedIndex());
                    break;
                default:
                    // Ничего не делаем для других команд
                    break;
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

    private boolean checkVictory(boolean running) throws IOException {
        if (session.getLevelNum() <= 21) {
            renderer.drawMessage(DEATH_MESSAGE_Y, VICTORY, CharColor.GREEN);
            running = false;

            Statistics.updateScoreBoard();

        }
        return running;
    }


//    private void movePlayer(Direction direction) {
//        newX = playerX + direction.getDx();
//        newY = playerY + direction.getDy();
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
//        // === АВТОПОДБОР ПРЕДМЕТА ===
//        Item picked = null;
//        for (Item item : session.getCurrentLevelItems()) {
//            if (item.getX() == newX && item.getY() == newY) {
//                picked = item;
//                break;
//            }
//        }
//
//        if (picked != null) {
//            if (session.getPlayer().getInventoryService().isFull()) {
//                renderer.drawMessage(28, "Inventory is full!", CharColor.RED);
//                return; // не идём, если нет места
//            }
//
//            symbolAtNewPosition = asciiMap[newY][newX];
//            session.getPlayer().getInventoryService().add(picked);
//            session.getCurrentLevelItems().remove(picked);
//            asciiMap[newY][newX] = '.'; // убираем с карты
//            renderer.drawMessage(28, "Picked: " + picked.getSubType(), CharColor.YELLOW);
//        }
//
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
                session.getPlayer().getMaxHealth(), session.getLevelNum(), 0);
        activeMessageLine3 = String.format(
                " DEBAG Now: (%d,%d) '%c' | Next: '%c'",
                playerX, playerY, symbolUnderPlayer, symbolAtNewPosition);
        renderer.drawString(3, 30, activeMessageLine3, CharColor.CYAN);


        // 🔥 Вывод списка предметов на экране
        int itemListY = 32; // Начинаем с 31 строки
        renderer.drawString(3, itemListY++, "=== ITEMS ON LEVEL ===", CharColor.CYAN);

        if (session.getCurrentLevelItems().isEmpty()) {
            renderer.drawString(5, itemListY++, "No items on this level", CharColor.WHITE);
        } else {
            // Показываем первые 5 предметов (чтобы не перегружать экран)
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

        // 🔥 ДОБАВЛЯЕМ ОТОБРАЖЕНИЕ ИНВЕНТАРЯ
        drawInventory();
    }



    private void syncPlayerPositionWithEntity() {
        Position pos = session.getPlayer().getPosition();
        this.playerX = pos.getX();
        this.playerY = pos.getY();
    }

    private void generateNewLevel() throws IOException {
        // Определяем, какой уровень генерировать
        int levelToGenerate;

        if (session.getCurrentMap() == null) {
            // Первый запуск - берем текущий levelNum (должен быть 1)
            levelToGenerate = session.getLevelNum();
        } else {
            // Переход на следующий уровень
            levelToGenerate = session.getLevelNum() + 1;
            session.setLevelNum(levelToGenerate); // УВЕЛИЧИВАЕМ!
        }

        // Проверка на победу (21 уровень по ТЗ)
        if (levelToGenerate > 21) {
            running = checkVictory(running);
            return;
        }

        // Генерация карты ( вместе с  предметами)
        char[][] newMap = levelGenerator.createAsciiMap(levelToGenerate);
        session.setCurrentMap(newMap);
        asciiMap = newMap;

        // 🔥 КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Берем сгенерированные предметы из LevelGenerator
        session.getCurrentLevelItems().clear();
        session.getCurrentLevelItems().addAll(levelGenerator.getItems());

        System.out.println("DEBUG: Added " + session.getCurrentLevelItems().size() +
                " items from LevelGenerator to session");


        // Находим стартовую позицию
        List<Room> rooms = levelGenerator.getRooms();
        for(Room room: rooms){
            if (room.isStartRoom()) {
                // Обновляем позицию игрока
                playerX = room.getX1() + 2;
                playerY = room.getY1() + 2;
            }
        }
        Position newPlaerPosition = new Position(playerX, playerY);

        // Обновляем позицию игрока
        symbolUnderPlayer = asciiMap[playerY][playerX];
        session.getPlayer().setPosition(newPlaerPosition);

        // Очищаем и генерируем врагов
        session.getEnemies().clear();
        createEnemies();

        // Обновляем туман войны
        fogOfWarService.reset();
        fogOfWarService.markCellAsExplored(playerX, playerY);
        fogOfWarService.updateVisibility(newPlaerPosition, asciiMap);

        // Сообщение Игроку
        activeMessageLine1 = "Level " + levelToGenerate;
        if (levelToGenerate > 1) {
            activeMessageLine2 = "You have gone deeper...";
        }
        messageTimer = MESSAGE_DURATION;

        // Для отладки
        // System.out.println("Сгенерирован уровень " + levelToGenerate + ", игрок в " + newPlaerPosition);
    }


    private void createEnemies() {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();

        // Случайная плотность: 40-60% комнат с врагами
        int totalRoomsWithEnemies = calculateTotalRoomsWithEnemies(rooms.size(), rand);

        // Перемешиваем комнаты, чтобы выбрать случайные
        List<Room> shuffledRooms = new ArrayList<>(rooms);
        Collections.shuffle(shuffledRooms, rand);

        int enemiesPlaced = 0;

        for (Room room : shuffledRooms) {
            if (enemiesPlaced >= totalRoomsWithEnemies) break;
            if (room.isStartRoom()) continue; // Пропускаем стартовую комнату

            enemiesPlaced += createEnemiesInRoom(room, rand, session);
        }
    }

    private int calculateTotalRoomsWithEnemies(int totalRooms, Random rand) {
        int roomsWithEnemies = (int) Math.round(totalRooms * (MIN_ENEMY_DENSITY + rand.nextDouble() * DENSITY_RANGE));
        return Math.max(MIN_ROOMS_WITH_ENEMIES, roomsWithEnemies);
    }

    private int createEnemiesInRoom(Room room, Random rand, GameSession session) {
        int enemiesCreated = 0;
        int enemiesInRoom = 1; // Временно по одному врагу в комнате

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
        // Пропускаем ход спящего игрока
        String sleepMsg = "You are sleep! Zzz...";
        session.getPlayer().setSleepTurns(false);
        renderer.drawMessage(UI_START_Y, sleepMsg, CharColor.CYAN);

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
    }

    private void handleMovement(Direction dir) throws IOException {
        int newX = playerX + dir.getDx();
        int newY = playerY + dir.getDy();


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
                if (item != null) {
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
        }
    }

    private void handleUseItem(ItemType itemType) {
        // Показываем меню выбора предмета
        inputHandler.setAwaitingSelection(true, itemType);
        activeMessageLine3 = "Выберите предмет " + itemType + " (1-9) или ESC для отмены";
        messageTimer = MESSAGE_DURATION;
    }


    private void handleItemPickup(Item item, int x, int y) {
        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        // Определяем тип предмета
        ItemType type;
        try {
            type = ItemType.valueOf(item.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ItemType.TREASURE; // fallback
        }

        // Проверяем, полон ли слот для этого типа
        if (inventory.isFull(type)) {
            activeMessageLine3 = String.format("%s slot is full! Max %d per type.",
                    type.name(), GameConstants.Player.MAX_PER_TYPE);
            messageTimer = MESSAGE_DURATION;
            return;
        }

        // Добавляем в инвентарь
        if (inventory.add(item)) {
            // Удаляем с карты
            session.getCurrentLevelItems().remove(item);
            asciiMap[y][x] = '.'; // Заменяем на пол

            // Сообщение игроку
            activeMessageLine3 = String.format("Picked up: %s (%s)",
                    item.getSubType(), type.name().toLowerCase());
            messageTimer = MESSAGE_DURATION;

            // Двигаемся на клетку с предметом
            renderer.drawChar(playerX, playerY, symbolUnderPlayer, CharColor.WHITE);
            fogOfWarService.markCellAsExplored(x, y);
            playerX = x;
            playerY = y;
            symbolUnderPlayer = asciiMap[playerY][playerX];

            // Синхронизируем позицию игрока
            session.getPlayer().setPosition(new Position(x, y));

            // 🔥 ДЕБАГ: выводим содержимое инвентаря
            debugInventory();
        } else {
            activeMessageLine3 = "Failed to add item to inventory";
            messageTimer = MESSAGE_DURATION;
        }
    }


    private void handleItemSelection(int index) {
        // Реализация выбора предмета из меню
        if (inputHandler.isAwaitingSelection()) {
            ItemType type = inputHandler.getPendingItemType();
            // Здесь должна быть логика применения предмета
            activeMessageLine3 = "Использован предмет типа " + type + " под индексом " + index;
            messageTimer = MESSAGE_DURATION;
            inputHandler.resetAwaitingState();
        }
    }


    private boolean isItemSymbol(char symbol) {
        return symbol == ',' ||   // food
                symbol == '!' ||   // elixir
                symbol == '?' ||   // scroll
                symbol == ')' ||   // weapon
                symbol == '$';     // treasure
    }

    private Item getItemAt(int x, int y) {

        System.out.println("DEBUG getItemAt: Looking at (" + x + "," + y + ")"); // 🔥 ОТЛАДКА
        System.out.println("DEBUG: Total items in level: " + session.getCurrentLevelItems().size());

        for (Item item : session.getCurrentLevelItems()) {
            System.out.println("DEBUG: Item at (" + item.getX() + "," + item.getY() + ") type: " + item.getType());
            if (item.getX() == x && item.getY() == y) {
                System.out.println("DEBUG: Found item!"); // 🔥 ОТЛАДКА
                return item;
            }
        }

        System.out.println("DEBUG: No item found"); // 🔥 ОТЛАДКА
        //return null;


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


    // Метод для получения цвета по типу предмета
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
        int startY = 38; // Начинаем ниже предметов уровня

        // Заголовок
        renderer.drawString(3, startY++, "=== INVENTORY ===", CharColor.CYAN);

        Player player = session.getPlayer();
        Inventory inventory = player.getInventory();

        // Проверяем, пуст ли инвентарь
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
            renderer.drawString(5, startY++, "Empty", CharColor.WHITE);
            return;
        }

        // Сокровища
        int treasureValue = inventory.getTreasureValue();
        if (treasureValue > 0) {
            renderer.drawString(5, startY++,
                    String.format("💰 Treasure: %d gold", treasureValue),
                    CharColor.YELLOW);
        }

        // Каждый тип предметов
        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            int count = inventory.count(type);
            if (count > 0) {
                // Форматируем название типа
                String typeName = type.name().charAt(0) +
                        type.name().substring(1).toLowerCase() +
                        (count > 1 ? "s" : "");

                // Отображаем количество
                renderer.drawString(5, startY++,
                        String.format("%s: %d", typeName, count),
                        getItemTypeColor(type));

                // Показываем детали первых 2 предметов каждого типа
                List<Item> items = inventory.getItems(type);
                int itemsToShow = Math.min(2, items.size());
                for (int i = 0; i < itemsToShow; i++) {
                    Item item = items.get(i);
                    String itemInfo = formatItemInfo(item);
                    renderer.drawString(7, startY++, itemInfo, CharColor.WHITE);
                }

                // Если предметов больше 2, показываем "..."
                if (items.size() > 2) {
                    renderer.drawString(7, startY++,
                            String.format("... and %d more", items.size() - 2),
                            CharColor.YELLOW);
                }
            }
        }

        // Экипированное оружие
        Item equipped = player.getEquippedWeapon();
        if (equipped != null && !equipped.getSubType().equals("fists")) {
            renderer.drawString(3, startY++,
                    String.format("🗡️ Equipped: %s (STR+%d)",
                            equipped.getSubType(),
                            equipped.getStrength()),
                    CharColor.GREEN);
        }

        // Общее количество предметов
        renderer.drawString(3, startY,
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



    // Метод для отладки
    private void debugInventory() {
        System.out.println("\n=== DEBUG INVENTORY ===");
        Inventory inventory = session.getPlayer().getInventory();

        for (ItemType type : ItemType.values()) {
            int count = inventory.count(type);
            if (count > 0) {
                System.out.printf("%s: %d items%n", type.name(), count);

                List<Item> items = inventory.getItems(type);
                for (int i = 0; i < items.size(); i++) {
                    Item item = items.get(i);
                    System.out.printf("  %d. %s (SubType: %s, STR: %d, HP: %d)%n",
                            i + 1, item.getType(), item.getSubType(),
                            item.getStrength(), item.getHealth());
                }
            }
        }

        System.out.printf("Treasure value: %d%n", inventory.getTreasureValue());
        System.out.println("======================\n");
    }

}

