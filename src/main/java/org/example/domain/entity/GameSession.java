package org.example.domain.entity;

import org.example.domain.model.Room;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private List<Enemy> enemies = new ArrayList<>();
    private Enemy currentCombatEnemy;
    private Player player;
    private char[][] currentMap;
    private List<Room> rooms; // Добавляем поле для комнат

    private int levelNum;
    private List<Item> currentLevelItems = new ArrayList<>();

    // Геттеры и сеттеры
    public List<Enemy> getEnemies() { return enemies; }
    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public List<Item> getCurrentLevelItems() {
        return currentLevelItems;
    }

    public void setCurrentLevelItems(List<Item> items) {
        this.currentLevelItems = items;
    }

    public char[][] getCurrentMap() { return currentMap; }
    public void setCurrentMap(char[][] currentMap) { this.currentMap = currentMap; }

    // Добавляем геттер и сеттер для комнат
    public List<Room> getRooms() {
        if (rooms == null) {
            rooms = new ArrayList<>();
        }
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public int getLevelNum() {
        return levelNum;
    }

    public void setLevelNum(int levelNum) {
        this.levelNum = levelNum;
    }
}