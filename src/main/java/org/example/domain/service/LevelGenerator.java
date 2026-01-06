package org.example.domain.service;

import org.example.config.GameConstants;
import org.example.domain.entity.Item;
import org.example.domain.model.Room;
import org.example.domain.model.Position;

import java.util.*;

/**
 * Генератор уровней для игры.
 * Создает карту с комнатами, коридорами и предметами.
 */
public class LevelGenerator {
    // Константы генерации карты
    private static final int ROOMS_AT_LEVEL = GameConstants.Map.ROOMS;
    private static final int MAX_WIDTH_ROOM_SIZE = GameConstants.Map.WIDTH / 3 - 4;
    private static final int MIN_WIDTH_ROOM_SIZE = MAX_WIDTH_ROOM_SIZE / 4;
    private static final int MAX_HEIGHT_ROOM_SIZE = GameConstants.Map.HEIGHT / 3 - 4;
    private static final int MIN_HEIGHT_ROOM_SIZE = MAX_HEIGHT_ROOM_SIZE / 2 + 1;

    // Основные структуры данных
    private List<Room> rooms;
    private Random rand;
    private List<Item> items = new ArrayList<>();

    // Внутренние карты для быстрого доступа
    private final Map<Position, Room> cellToRoomMap = new HashMap<>();    // Клетка -> комната
    private final Set<Position> corridorCells = new HashSet<>();          // Клетки коридоров
    private final Map<Position, Character> cellTypeMap = new HashMap<>(); // Тип клетки (символ)

    /**
     * Конструктор генератора уровней.
     */
    public LevelGenerator() {
        this.rand = new Random();
    }

    /**
     * Создает ASCII-карту для указанного уровня.
     * @param levelNumber номер уровня
     * @return двумерный массив символов, представляющий карту
     */
    public char[][] createAsciiMap(int levelNumber) {
        // Инициализация пустой карты
        char[][] asciiMap = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];
        initializeEmptyMap(asciiMap);

        // Генерация и размещение комнат
        rooms = createRooms(levelNumber);
        addRoomsOnAsciiMap(asciiMap);

        // Генерация коридоров
        addCorridorsOnAsciiMap(asciiMap);

        // Генерация и размещение предметов
        generateAndPlaceItems(asciiMap, levelNumber);

        // Добавление выхода из уровня
        placeExitOnMap(asciiMap);

        // Обновление внутренних структур
        rebuildInternalMaps(asciiMap);

        return asciiMap;
    }

    /**
     * Инициализирует карту пустыми клетками (пробелами).
     */
    private void initializeEmptyMap(char[][] asciiMap) {
        for (int y = 0; y < GameConstants.Map.HEIGHT; y++) {
            for (int x = 0; x < GameConstants.Map.WIDTH; x++) {
                asciiMap[y][x] = ' ';
            }
        }
    }

    /**
     * Генерирует и размещает предметы на карте.
     */
    private void generateAndPlaceItems(char[][] asciiMap, int levelNumber) {
        items = ItemGenerator.generateForLevel(levelNumber);
        Random randomItem = new Random();

        for (Item item : items) {
            boolean placed = false;
            int attempts = 0;

            // Пытаемся разместить предмет в случайной позиции внутри комнаты
            while (!placed && attempts < 100) {
                Room room = rooms.get(randomItem.nextInt(rooms.size()));
                int rx = room.getX1() + 1 + randomItem.nextInt(room.getWidth() - 2);
                int ry = room.getY1() + 1 + randomItem.nextInt(room.getHeight() - 2);

                // Размещаем только на свободном полу
                if (asciiMap[ry][rx] == '.') {
                    item.setPosition(rx, ry);
                    asciiMap[ry][rx] = getItemSymbol(item.getType());
                    placed = true;
                }
                attempts++;
            }
        }
    }

    /**
     * Возвращает символ для типа предмета.
     */
    private char getItemSymbol(String itemType) {
        return switch (itemType) {
            case "food" -> ',';
            case "elixir" -> '!';
            case "scroll" -> '?';
            case "weapon" -> ')';
            case "treasure" -> '$';
            default -> '*';
        };
    }

    /**
     * Размещает выход из уровня в соответствующей комнате.
     */
    private void placeExitOnMap(char[][] asciiMap) {
        for (Room room : rooms) {
            if (room.isExitRoom()) {
                asciiMap[room.getY2() - 2][room.getX2() - 2] = 'E';
            }
        }
    }

    /**
     * Создает комнаты для уровня.
     * @return список комнат
     */
    private List<Room> createRooms(int levelNumber) {
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < ROOMS_AT_LEVEL; i++) {
            Room room = generateRandomRoom(i);
            // Первая комната - стартовая, последняя - с выходом
            if (i == 0) room.setStartRoom(true);
            if (i == ROOMS_AT_LEVEL - 1) room.setExitRoom(true);
            rooms.add(room);
        }
        return rooms;
    }

    /**
     * Генерирует случайную комнату.
     */
    private Room generateRandomRoom(int index) {
        int width = MIN_WIDTH_ROOM_SIZE + rand.nextInt(MAX_WIDTH_ROOM_SIZE - MIN_WIDTH_ROOM_SIZE);
        int height = MIN_HEIGHT_ROOM_SIZE + rand.nextInt(MAX_HEIGHT_ROOM_SIZE - MIN_HEIGHT_ROOM_SIZE);

        // Распределение комнат в сетке 3x3
        int gridX = (index % 3) * (MAX_WIDTH_ROOM_SIZE + 3);
        int gridY = (index / 3) * (MAX_HEIGHT_ROOM_SIZE + 3);

        return new Room(index, new Position(gridX, gridY), width, height);
    }

    /**
     * Добавляет комнаты на карту.
     */
    private void addRoomsOnAsciiMap(char[][] asciiMap) {
        cellToRoomMap.clear();

        // Рисуем стены комнат
        for (Room room : rooms) {
            drawRoomWalls(asciiMap, room);
        }

        // Заполняем внутренности комнат полом
        for (Room room : rooms) {
            fillRoomFloor(asciiMap, room);
        }
    }

    /**
     * Рисует стены комнаты.
     */
    private void drawRoomWalls(char[][] asciiMap, Room room) {
        // Горизонтальные стены
        for (int x = room.getX1(); x <= room.getX2(); x++) {
            setCell(asciiMap, x, room.getY1(), '~', room); // Верхняя стена
            setCell(asciiMap, x, room.getY2(), '~', room); // Нижняя стена
        }

        // Вертикальные стены
        for (int y = room.getY1(); y <= room.getY2(); y++) {
            setCell(asciiMap, room.getX1(), y, '|', room); // Левая стена
            setCell(asciiMap, room.getX2(), y, '|', room); // Правая стена
        }
    }

    /**
     * Заполняет комнату полом (точками).
     */
    private void fillRoomFloor(char[][] asciiMap, Room room) {
        for (int x = room.getX1() + 1; x < room.getX2(); x++) {
            for (int y = room.getY1() + 1; y < room.getY2(); y++) {
                setCell(asciiMap, x, y, '.', room);
            }
        }
    }

    /**
     * Устанавливает символ в клетке и обновляет внутренние структуры.
     */
    private void setCell(char[][] asciiMap, int x, int y, char symbol, Room room) {
        asciiMap[y][x] = symbol;
        Position pos = new Position(x, y);
        cellTypeMap.put(pos, symbol);

        if (room != null) {
            cellToRoomMap.put(pos, room);
        }
    }

    /**
     * Добавляет коридоры, соединяющие комнаты.
     */
    private void addCorridorsOnAsciiMap(char[][] asciiMap) {
        // Соединяем комнаты по сетке 3x3
        for (int yRoom = 0; yRoom < 3; yRoom++) {
            for (int xRoom = 0; xRoom < 3; xRoom++) {
                if (xRoom < 2) {
                    addHorizontalCorridor(asciiMap, xRoom, yRoom);
                }
                if (yRoom < 2) {
                    addVerticalCorridor(asciiMap, xRoom, yRoom);
                }
            }
        }
    }

    /**
     * Добавляет горизонтальный коридор между двумя комнатами.
     */
    private void addHorizontalCorridor(char[][] asciiMap, int xRoom, int yRoom) {
        int first = yRoom * 3 + xRoom;
        int second = first + 1;

        Room room1 = rooms.get(first);
        Room room2 = rooms.get(second);

        // Случайные точки входа/выхода из комнат
        int xStart = room1.getX2();
        int yStart = room1.getY1() + 1 + rand.nextInt(room1.getHeight() - 2);
        int xEnd = room2.getX1();
        int yEnd = room2.getY1() + 1 + rand.nextInt(room2.getHeight() - 2);

        // Точка излома коридора
        int crossLine = xStart + 1 + rand.nextInt(xEnd - xStart - 1);

        // Отмечаем входы/выходы
        asciiMap[yStart][xStart] = '+';
        asciiMap[yEnd][xEnd] = '+';

        // Рисуем три сегмента коридора
        addHorizontalLine(asciiMap, xStart + 1, crossLine + 1, yStart);
        addVerticalLine(asciiMap, yStart, yEnd, crossLine);
        addHorizontalLine(asciiMap, crossLine, xEnd, yEnd);

        // Обновляем структуры данных
        updateCorridorCells(xStart, xEnd, yStart, yEnd);
    }

    /**
     * Добавляет вертикальный коридор между двумя комнатами.
     */
    private void addVerticalCorridor(char[][] asciiMap, int xRoom, int yRoom) {
        int first = yRoom * 3 + xRoom;
        int second = first + 3;

        Room room1 = rooms.get(first);
        Room room2 = rooms.get(second);

        // Случайные точки входа/выхода
        int xStart = room1.getX1() + 1 + rand.nextInt(room1.getWidth() - 2);
        int yStart = room1.getY2();
        int xEnd = room2.getX1() + 1 + rand.nextInt(room2.getWidth() - 2);
        int yEnd = room2.getY1();

        // Точка излома коридора
        int crossLine = yStart + 1 + rand.nextInt(yEnd - yStart - 1);

        // Отмечаем входы/выходы
        asciiMap[yStart][xStart] = '+';
        asciiMap[yEnd][xEnd] = '+';

        // Рисуем три сегмента коридора
        addVerticalLine(asciiMap, yStart + 1, crossLine + 1, xStart);
        addHorizontalLine(asciiMap, xStart, xEnd, crossLine);
        addVerticalLine(asciiMap, crossLine, yEnd, xEnd);
    }

    /**
     * Обновляет клетки коридоров во внутренних структурах.
     */
    private void updateCorridorCells(int x1, int x2, int y1, int y2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Position pos = new Position(x, y);
                corridorCells.add(pos);
                cellTypeMap.put(pos, '#');
            }
        }
    }

    /**
     * Рисует горизонтальную линию символов.
     */
    private void addHorizontalLine(char[][] asciiMap, int from, int to, int y) {
        int start = Math.min(from, to);
        int end = Math.max(from, to);

        for (int x = start; x < end; x++) {
            asciiMap[y][x] = '#';
        }
    }

    /**
     * Рисует вертикальную линию символов.
     */
    private void addVerticalLine(char[][] asciiMap, int from, int to, int x) {
        int start = Math.min(from, to);
        int end = Math.max(from, to);

        for (int y = start; y < end; y++) {
            asciiMap[y][x] = '#';
        }
    }

    /**
     * Восстанавливает состояние из сохраненной игры.
     */
    public void restoreFromGameState(char[][] savedMap, List<Room> savedRooms, List<Item> savedItems) {
        if (savedMap == null) return;

        if (savedRooms != null) {
            this.rooms = new ArrayList<>(savedRooms);
        }

        if (savedItems != null) {
            this.items = new ArrayList<>(savedItems);
        }

        rebuildInternalMaps(savedMap);
    }

    /**
     * Перестраивает внутренние структуры данных на основе карты.
     */
    private void rebuildInternalMaps(char[][] map) {
        cellToRoomMap.clear();
        corridorCells.clear();
        cellTypeMap.clear();

        if (map == null || rooms == null) return;

        // Заполняем карту типов клеток
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                Position pos = new Position(x, y);
                char tile = map[y][x];
                cellTypeMap.put(pos, tile);

                if (tile == '#') {
                    corridorCells.add(pos);
                }
            }
        }

        // Заполняем карту клетка->комната
        for (Room room : rooms) {
            for (int x = room.getX1(); x <= room.getX2(); x++) {
                for (int y = room.getY1(); y <= room.getY2(); y++) {
                    cellToRoomMap.put(new Position(x, y), room);
                }
            }
        }

        // Привязываем клетки коридоров к ближайшим комнатам
        for (Position corridorPos : corridorCells) {
            assignCorridorToRoom(corridorPos);
        }
    }

    /**
     * Привязывает клетку коридора к ближайшей комнате.
     */
    private void assignCorridorToRoom(Position corridorPos) {
        int x = corridorPos.getX();
        int y = corridorPos.getY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                Position adjacentPos = new Position(x + dx, y + dy);
                Room adjacentRoom = cellToRoomMap.get(adjacentPos);

                if (adjacentRoom != null) {
                    cellToRoomMap.put(corridorPos, adjacentRoom);
                    return;
                }
            }
        }
    }

    // ================= ГЕТТЕРЫ =================

    public List<Room> getRooms() {
        return rooms;
    }

    public Random getRand() {
        return rand;
    }

    public List<Item> getItems() {
        return items;
    }

    public Room getRoomAt(int x, int y) {
        return cellToRoomMap.get(new Position(x, y));
    }
}