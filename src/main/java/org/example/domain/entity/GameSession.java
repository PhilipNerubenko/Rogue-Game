package org.example.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class GameSession {


    private List<Enemy> enemies = new ArrayList<>();
    private Enemy currentCombatEnemy;
    private Player player; // ДОБАВЬТЕ ЭТО ПОЛЕ

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



//    private List<Enemy> enemies = new ArrayList<>();
//    private Enemy currentCombatEnemy;
//
//    public List<Enemy> getEnemies() { return enemies; }
//    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }
//
//    public Enemy getCurrentCombatEnemy() { return currentCombatEnemy; }
//    public void setCurrentCombatEnemy(Enemy enemy) { this.currentCombatEnemy = enemy; }
}
