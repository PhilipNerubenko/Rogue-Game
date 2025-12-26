package org.example.datalayer;


public class SessionStat implements Comparable<SessionStat>{
    private int treasures;
    private int levelNum;

    private int enemies;
    private int food;
    private int elixirs;
    private int scrolls;
    private int attacks;
    private int missed;
    private int moves;

    public SessionStat() {}

    public SessionStat(int treasures, int levelN, int enemies, int food, int elixirs, int scrolls, int attacks, int missed, int moves) {
        this.treasures = treasures;
        this.levelNum = levelN;
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


    public int getLevelNum() {
        return levelNum;
    }

    public void setLevelNum(int levelN) {
        this.levelNum = levelN;

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


    @Override
    public int compareTo(SessionStat o) {
        if (this.treasures != o.treasures) return Long.compare(o.treasures, this.treasures);
        if (this.levelNum != o.levelNum) return Long.compare(o.levelNum, this.levelNum);
        return Long.compare(this.moves, o.moves); // меньше ходов — лучше
    }

}
