package org.example.datalayer;

public class SessionStat {
    private int treasures;
    private int level;
    private int enemies;
    private int food;
    private int elixirs;
    private int scrolls;
    private int attacks;
    private int missed;
    private int moves;

    public SessionStat(int treasures, int level, int enemies, int food, int elixirs, int scrolls, int attacks, int missed, int moves) {
        this.treasures = treasures;
        this.level = level;
        this.enemies = enemies;
        this.food = food;
        this.elixirs = elixirs;
        this.scrolls = scrolls;
        this.attacks = attacks;
        this.missed = missed;
        this.moves = moves;
    }

    public int getTreasures() {
        return treasures;
    }

    public void setTreasures(int treasures) {
        this.treasures = treasures;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getEnemies() {
        return enemies;
    }

    public void setEnemies(int enemies) {
        this.enemies = enemies;
    }

    public int getFood() {
        return food;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public int getElixirs() {
        return elixirs;
    }

    public void setElixirs(int elixirs) {
        this.elixirs = elixirs;
    }

    public int getScrolls() {
        return scrolls;
    }

    public void setScrolls(int scrolls) {
        this.scrolls = scrolls;
    }

    public int getAttacks() {
        return attacks;
    }

    public void setAttacks(int attacks) {
        this.attacks = attacks;
    }

    public int getMissed() {
        return missed;
    }

    public void setMissed(int missed) {
        this.missed = missed;
    }

    public int getMoves() {
        return moves;
    }

    public void setMoves(int moves) {
        this.moves = moves;
    }
}
