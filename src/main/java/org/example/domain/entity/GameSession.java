package org.example.domain.entity;

import org.example.domain.model.Room;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private List<Enemy> enemies = new ArrayList<>();
    private Enemy currentCombatEnemy;
    private Player player;
    private int playerX;
    private int playerY;
    private char[][] currentMap;
    private List<Room> rooms;
    private int levelNum;


    // Геттеры и сеттеры

    private List<Item> currentLevelItems = new ArrayList<>();
    public List<Enemy> getEnemies() { return enemies; }
    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }

    public Enemy getCurrentCombatEnemy() { return currentCombatEnemy; }
    public void setCurrentCombatEnemy(Enemy enemy) { this.currentCombatEnemy = enemy; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getPlayerX() { return playerX; }
    public void setPlayerX(int playerX) { this.playerX = playerX; }


    public int getPlayerY() { return playerY; }
    public void setPlayerY(int playerY) { this.playerY = playerY; }

    //
    public List<Item> getCurrentLevelItems() {
        return currentLevelItems;
    }

    public void setCurrentLevelItems(List<Item> items) {
        this.currentLevelItems = items;
    }



    public char[][] getCurrentMap() { return currentMap; }
    public void setCurrentMap(char[][] currentMap) { this.currentMap = currentMap; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }

    public int getLevelNum() {
        return levelNum;
    }

    public void setLevelNum(int levelNum) {
        this.levelNum = levelNum;
    }
}