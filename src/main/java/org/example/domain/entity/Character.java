package org.example.domain.entity;

import java.util.List;

public class Character {
    private int maximumHealth;
    private int health;
    private int agility;
    private int strength;
    List<Item> backpack;

    public Character(int maximumHealth, int health, int agility, int strength, List<Item> backpack) {
        this.maximumHealth = maximumHealth;
        this.health = health;
        this.agility = agility;
        this.strength = strength;
        this.backpack = backpack;
    }

    public int getMaximumHealth() {
        return maximumHealth;
    }

    public void setMaximumHealth(int maximumHealth) {
        this.maximumHealth = maximumHealth;
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

    public List<Item> getBackpack() {
        return backpack;
    }

    public void setBackpack(List<Item> backpack) {
        this.backpack = backpack;
    }
}
