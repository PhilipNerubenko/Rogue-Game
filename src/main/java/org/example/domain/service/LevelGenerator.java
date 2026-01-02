package org.example.domain.service;


import org.example.config.GameConstants;
import org.example.domain.entity.Item;
import org.example.domain.model.Room;
import org.example.domain.model.Position;

import java.util.*;

public class LevelGenerator {
    private static final int ROOMS_AT_LEVEL = GameConstants.Map.ROOMS;              //комнат на уровне
    private static final int MAX_WIDTH_ROOM_SIZE = GameConstants.Map.WIDTH/3-4;     //максимальная ширина комнаты
    private static final int MIN_WIDTH_ROOM_SIZE = MAX_WIDTH_ROOM_SIZE/4;           //минимальная  ширина комнаты
    private static final int MAX_HEIGHT_ROOM_SIZE = GameConstants.Map.HEIGHT/3-4;   //максимальная высота комнаты
    private static final int MIN_HEIGHT_ROOM_SIZE = MAX_HEIGHT_ROOM_SIZE/2+1;         //минимальная  высота комнаты
    private List<Room> rooms;
    private Random rand;

    // НОВЫЕ ПОЛЯ: карта клеток -> комната
    private final Map<Position, Room> cellToRoomMap = new HashMap<>();
    // Набор клеток коридоров
    private final Set<Position> corridorCells = new HashSet<>();
    // Тип клетки для быстрого доступа
    private final Map<Position, Character> cellTypeMap = new HashMap<>();

    private  List<Item> items = new ArrayList<>();

    public LevelGenerator() {
        this.rand = new Random();  // Или new Random(seed), если нужен фиксированный seed для воспроизводимости
    }

    public char[][] createAsciiMap(int levelNumber){

        char[][] asciiMap = new char[GameConstants.Map.HEIGHT][GameConstants.Map.WIDTH];
        //заполняем пробелами
        for (int y =0; y < GameConstants.Map.HEIGHT; y++) {
            for (int x = 0; x < GameConstants.Map.WIDTH; x++) {
                asciiMap[y][x] = ' ';
            }
        }

        rooms = new ArrayList<>();
        rooms = createRooms(levelNumber);
        addRoomsOnAsciiMap(asciiMap);
        Random rand = new Random();
        addCorridorsOnAsciiMap(asciiMap);

        // === ГЕНЕРАЦИЯ И РАЗМЕЩЕНИЕ ПРЕДМЕТОВ ===
        items = ItemGenerator.generateForLevel(levelNumber);
        Random randomItem = new Random();

        for (Item item : items) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < 100) {
                Room room = rooms.get(randomItem.nextInt(rooms.size()));
                int rx = room.getX1() + 1 + randomItem.nextInt(room.getWidth() - 2);
                int ry = room.getY1() + 1 + randomItem.nextInt(room.getHeight() - 2);

                if (asciiMap[ry][rx] == '.') {
                    item.setPosition(rx, ry);

                    char symbol = switch (item.getType()) {
                        case "food"     -> ',';
                        case "elixir"   -> '!';
                        case "scroll"   -> '?';
                        case "weapon"   -> ')';
                        case "treasure" -> '$';
                        default         -> '*';
                    };

                    asciiMap[ry][rx] = symbol;
                    placed = true;
                }
                attempts++;
            }
        }

        //добавляем выход если комната isExitRoom
        for(int i = 0; i < ROOMS_AT_LEVEL; i++){
        Room curentRoom = rooms.get(i);
            if (curentRoom.isExitRoom()){
                asciiMap[curentRoom.getY2() - 2][curentRoom.getX2() -2] = 'E';
            }
        }
        return asciiMap;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public Random getRand() {
        return rand;
    }

    private List<Room> createRooms(int levelNumber){
        // Генерируем 9 комнат в 3x3 сетке
        for (int i = 0; i < ROOMS_AT_LEVEL; i++) {
            Room room = generateRandomRoom(i);
            if (i == 0) room.setStartRoom(true);                      // Первая комната = старт
            if (i == ROOMS_AT_LEVEL - 1) room.setExitRoom(true);      // Последняя = выход
            rooms.add(room);
        }
        return  rooms;
    }

    private Room generateRandomRoom(int index) {
        int width = MIN_WIDTH_ROOM_SIZE + (int)(Math.random() * (MAX_WIDTH_ROOM_SIZE - MIN_WIDTH_ROOM_SIZE));
        int height = MIN_HEIGHT_ROOM_SIZE + (int)(Math.random() * (MAX_HEIGHT_ROOM_SIZE - MIN_HEIGHT_ROOM_SIZE));

        // Позиция в "сетке" 3x3
        int gridX = (index % 3) * (MAX_WIDTH_ROOM_SIZE +3);
        int gridY = (index / 3) * (MAX_HEIGHT_ROOM_SIZE+3);
        return new Room( index, new Position(gridX, gridY), width, height);
    }


    private void addRoomsOnAsciiMap(char[][] asciiMap) {
        cellToRoomMap.clear(); // Очистка перед генерацией
        for (int i = 0; i < 9; i++) {
            Room room = rooms.get(i);
            // Заполняем границы
            for (int x = room.getX1(); x <= room.getX2(); x++) {
                // Верхняя и нижняя стена
                setCell(asciiMap, x, room.getY1(), '~', room);
                setCell(asciiMap, x, room.getY2(), '~', room);
            }
            for (int y = room.getY1(); y <= room.getY2(); y++) {
                // Левая и правая стена
                setCell(asciiMap, room.getX1(), y, '|', room);
                setCell(asciiMap, room.getX2(), y, '|', room);
            }
        }

        // Заполняем внутренности комнат
        for (int i = 0; i < 9; i++) {
            Room room = rooms.get(i);
            for (int x = room.getX1() + 1; x < room.getX2(); x++) {
                for (int y = room.getY1() + 1; y < room.getY2(); y++) {
                    setCell(asciiMap, x, y, '.', room); // Пол
                }
            }
        }
    }

    private void addCorridorsOnAsciiMap(char[][] asciiMap) {
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

    private void addHorizontalCorridor(char[][] asciiMap, int xRoom, int yRoom) {
        int first = yRoom * 3 + xRoom;
        int second = first + 1;
        int xStart = rooms.get(first).getX2();;
        int yStart = rand.nextInt(rooms.get(first).getY2() - rooms.get(first).getY1() - 1) + rooms.get(first).getY1() + 1;
        int xEnd = rooms.get(second).getX1();
        int yEnd = rand.nextInt(rooms.get(second).getY2() - rooms.get(second).getY1() - 1) + rooms.get(second).getY1() + 1;
        int crossLine = rand.nextInt(xEnd - xStart - 1) + xStart + 1;
        asciiMap[yStart][xStart] = '+';
        asciiMap[yEnd][xEnd] = '+';
        addHorizontalLine(asciiMap, xStart + 1, crossLine + 1, yStart);
        addVerticalLine(asciiMap, yStart, yEnd, crossLine);
        addHorizontalLine(asciiMap, crossLine, xEnd, yEnd);
        // После генерации corridor cells:
        for (int x = Math.min(xStart, xEnd); x <= Math.max(xStart, xEnd); x++) {
            for (int y = Math.min(yStart, yEnd); y <= Math.max(yStart, yEnd); y++) {
                corridorCells.add(new Position(x, y));
                cellTypeMap.put(new Position(x, y), '#');
            }
        }
    }

    // Вспомогательный метод для установки клетки
    private void setCell(char[][] asciiMap, int x, int y, char symbol, Room room) {
        asciiMap[y][x] = symbol;
        Position pos = new Position(x, y);
        cellTypeMap.put(pos, symbol);
        if (room != null) {
            cellToRoomMap.put(pos, room);
        }
    }

    private void addVerticalCorridor(char[][] asciiMap, int xRoom, int yRoom) {
        int first = yRoom * 3 + xRoom;
        int second = first + 3;
        int xStart = rand.nextInt(rooms.get(first).getX2() - rooms.get(first).getX1() - 1) + rooms.get(first).getX1() + 1;
        int yStart = rooms.get(first).getY2();
        int xEnd = rand.nextInt(rooms.get(second).getX2() - rooms.get(second).getX1() - 1) + rooms.get(second).getX1() + 1;
        int yEnd = rooms.get(second).getY1();
        int crossLine = rand.nextInt(yEnd - yStart - 1) + yStart + 1;
        asciiMap[yStart][xStart] = '+';
        asciiMap[yEnd][xEnd] = '+';
        addVerticalLine(asciiMap, yStart + 1, crossLine + 1, xStart);
        addHorizontalLine(asciiMap, xStart, xEnd, crossLine);
        addVerticalLine(asciiMap, crossLine, yEnd, xEnd);
    }


    private void addHorizontalLine(char[][] asciiMap, int from, int where, int y) {
        int start = Math.min(from, where);
        int end = Math.max(from, where);
        for (int x = start; x < end; x++) {
            asciiMap[y][x] = '#';
        }
    }

    private void addVerticalLine(char[][] asciiMap, int from, int where, int x) {
        int start = Math.min(from, where);
        int end = Math.max(from, where);
        for (int y = start; y < end; y++) {
            asciiMap[y][x] = '#';
        }
    }

    // Методы доступа:
    public Room getRoomAt(int x, int y) {
        return cellToRoomMap.get(new Position(x, y));
    }

    public List<Item> getItems() {
        return items;
    }
}
