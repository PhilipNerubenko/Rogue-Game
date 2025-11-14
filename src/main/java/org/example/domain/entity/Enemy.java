package org.example.domain.entity;

public class Enemy {
    private String type;
    private int health;
    private int agility;
    private int strength;
    private int hostility;

    public Enemy(String type, int health, int agility, int strength, int hostility) {
        this.type = type;
        this.health = health;
        this.agility = agility;
        this.strength = strength;
        this.hostility = hostility;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getAgility() {
        return agility;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getHostility() {
        return hostility;
    }

    public void setHostility(int hostility) {
        this.hostility = hostility;
    }
}
