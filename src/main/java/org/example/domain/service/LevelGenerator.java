package org.example.domain.service;


import org.example.config.GameConstants;
import org.example.domain.model.Corridor;
import org.example.domain.model.Level;
import org.example.domain.model.Room;
import org.example.domain.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelGenerator {
    private static final int ROOMS_AT_LEVEL = GameConstants.Map.ROOMS;              //комнат на уровне
    private static final int MAX_WIDTH_ROOM_SIZE = GameConstants.Map.WIDTH/3-4;     //максимальная ширина комнаты
    private static final int MIN_WIDTH_ROOM_SIZE = MAX_WIDTH_ROOM_SIZE/4;           //минимальная  ширина комнаты
    private static final int MAX_HEIGHT_ROOM_SIZE = GameConstants.Map.HEIGHT/3-4;   //максимальная высота комнаты
    private static final int MIN_HEIGHT_ROOM_SIZE = MAX_HEIGHT_ROOM_SIZE/2+1;         //минимальная  высота комнаты
//    private char[][] asciiMap;
    private List<Room> rooms;
    private List<Corridor> corridors;
    private Random rand;

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
        corridors = new ArrayList<>();
        Random rand = new Random();
//        corridors = createCorridor();
        addCorridorsOnAsciiMap(asciiMap);
        return asciiMap;
    }


    public List<Room> getRooms() {
        return rooms;
    }

    public List<Corridor> getCorridors() {
        return corridors;
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


    private void addRoomsOnAsciiMap(char[][] asciiMap){
        //for (int i = 0; i < ROOMS_AT_LEVEL; i++) {
        for (int i = 0; i < 9; i++) {
                for (int x = rooms.get(i).getX1(); x < rooms.get(i).getX2(); x++) {
                    asciiMap[rooms.get(i).getY1()][x] = '~';
                    asciiMap[rooms.get(i).getY2()][x] = '~';
                }
                for (int y = rooms.get(i).getY1(); y <= rooms.get(i).getY2(); y++) {
                    asciiMap[y][rooms.get(i).getX1()] = '|';
                    asciiMap[y][rooms.get(i).getX2()] = '|';
                }
            }
        for (int i = 0; i < 9; i++) {
            for (int x = rooms.get(i).getX1() + 1; x < rooms.get(i).getX2(); x++) {
                for (int y = rooms.get(i).getY1() + 1; y < rooms.get(i).getY2(); y++) {
                    asciiMap[y][x] = '.';
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




//    private List<Corridor> connectRooms(List<Room> rooms){
//        // Соединяем в цепочку (0-1-2-3-...-8)
//        for (int i = 0; i < rooms.size() - 1; i++) {
//            Corridor corridor = createCorridor(rooms.get(i), rooms.get(i + 1));
//            corridors.add(corridor);
//        }
//    }
}
