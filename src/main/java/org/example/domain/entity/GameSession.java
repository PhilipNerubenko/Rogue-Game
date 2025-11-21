package org.example.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private List<Enemy> enemies = new ArrayList<>();
    private Enemy currentCombatEnemy;

    public List<Enemy> getEnemies() { return enemies; }
    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }

    public Enemy getCurrentCombatEnemy() { return currentCombatEnemy; }
    public void setCurrentCombatEnemy(Enemy enemy) { this.currentCombatEnemy = enemy; }
}
