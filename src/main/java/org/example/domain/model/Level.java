package org.example.domain.model;

import java.util.List;

// уровень подземелья , в своей структуре имеет все комнаты и коридоры и параметр уровень игры
public class Level {
    private int levelNumber;
    List<Room> rooms;
    List <Corridor> corridors;
    Room startRoom;
    Room exitRoom;

    public Level(int levelNumber, List<Room> rooms, List<Corridor> corridors, Room startRoom, Room exitRoom) {
        this.levelNumber = levelNumber;
        this.rooms = rooms;
        this.corridors = corridors;
        this.startRoom = startRoom;
        this.exitRoom = exitRoom;
    }
}
