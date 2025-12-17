package org.example.domain.model;

import java.util.*;

// уровень подземелья , в своей структуре имеет все комнаты и коридоры и параметр уровень игры
public class Level {

    private int levelNumber;
    private List<Room> rooms;
    //private List <Corridor> corridors;
    private Room startRoom;
    private Room exitRoom;
    private char[][] asciiMap;
    private final Map<Position, Room> cellToRoomMap = new HashMap<>();
    // Набор клеток коридоров
    private final Set<Position> corridorCells = new HashSet<>();
    // Тип клетки для быстрого доступа
    private final Map<Position, Character> cellTypeMap = new HashMap<>();





    public Level(int levelNumber) {
        this.levelNumber = levelNumber;
    }

    // Единственный экземпляр класса
    private static volatile Level instance;
//    public Level(List<Room> rooms, List<Corridor> corridors, Room startRoom, Room exitRoom) {
//        //this.levelNumber = levelNumber;
//        this.rooms = rooms;
//        //this.corridors = corridors;
//        this.startRoom = startRoom;
//        this.exitRoom = exitRoom;
//    }

    public char[][] getAsciiMap() {
        return asciiMap;
    }

    public void setAsciiMap(char[][] asciiMap) {
        this.asciiMap = asciiMap;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

//    public void setCorridors(List<Corridor> corridors) {
//        this.corridors = corridors;
//    }

    public void setStartRoom(Room startRoom) {
        this.startRoom = startRoom;
    }

    public void setExitRoom(Room exitRoom) {
        this.exitRoom = exitRoom;
    }

    public List<Room> getRooms() {
        return rooms;
    }

//    public List<Corridor> getCorridors() {
//        return corridors;
//    }

    public Room getStartRoom() { return this.startRoom; }

    public Room getExitRoom() {
        return exitRoom;
    }


    public int getLevelNumber() {
        return levelNumber;
    }

//    public void setLevelNumber(int levelNumber) {
//        this.levelNumber = levelNumber;
//    }

    // Методы доступа:
    public Room getRoomAt(int x, int y) {
        return cellToRoomMap.get(new Position(x, y));
    }

    public boolean isCorridor(int x, int y) {
        return corridorCells.contains(new Position(x, y));
    }

    public Character getCellType(int x, int y) {
        return cellTypeMap.get(new Position(x, y));
    }

    public Map<Position, Room> getCellToRoomMap() {
        return cellToRoomMap;
    }

    public Set<Position> getCorridorCells() {
        return corridorCells;
    }

    public Map<Position, Character> getCellTypeMap() {
        return cellTypeMap;
    }
}
