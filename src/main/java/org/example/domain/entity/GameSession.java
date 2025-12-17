package org.example.domain.entity;

import org.example.domain.model.Level;

import java.util.ArrayList;
import java.util.List;

public class GameSession {


    private List<Enemy> enemies = new ArrayList<>();
    private Enemy currentCombatEnemy;
    private Player player; // ДОБАВЬТЕ ЭТО ПОЛЕ
    private Level level;
    private Inventory inventory;

    public List<Enemy> getEnemies() { return enemies; }
    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }

    public Enemy getCurrentCombatEnemy() { return currentCombatEnemy; }
    public void setCurrentCombatEnemy(Enemy enemy) { this.currentCombatEnemy = enemy; }

    // ДОБАВЬТЕ ЭТИ МЕТОДЫ:
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }


    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    //    private List<Enemy> enemies = new ArrayList<>();
//    private Enemy currentCombatEnemy;
//
//    public List<Enemy> getEnemies() { return enemies; }
//    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }
//
//    public Enemy getCurrentCombatEnemy() { return currentCombatEnemy; }
//    public void setCurrentCombatEnemy(Enemy enemy) { this.currentCombatEnemy = enemy; }
}
