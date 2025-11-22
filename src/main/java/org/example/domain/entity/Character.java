package org.example.domain.entity;

import org.example.config.GameConstants;

public abstract class Character {
    protected int maxHealth;  // protected для доступа из наследников
    protected int health;
    protected int agility;
    protected int strength;
    protected Inventory inventory;  // Перенесите сюда из Player


    public Character(int maxHealth, int agility, int strength) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.agility = agility;
        this.strength = strength;
        this.inventory = new Inventory();
    }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = agility; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
