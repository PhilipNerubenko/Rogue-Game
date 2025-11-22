package org.example.domain.entity;

import org.example.domain.model.Direction;
import org.example.domain.model.Position;

// Персонаж + позиция + инвентарь
public class Player {
    private Position position;
    private Item equippedWeapon; // null = кулаки
    private Inventory inventory;
    private int maxHealth = 30;
    private int health = 30;
    private int agility = 5;
    private int strength = 5;

    public Player(Position position) {
        this.position = position;
    }

    public Player(Position position, Inventory inventory) {
        this.position = position;
        this.inventory = inventory;
    }
    // ВОТ ЭТОГО МЕТОДА НЕ ХВАТАЕТ:
    public Position getPosition() {
        return position;
    }

    // Остальные getter/setter
    public Item getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(Item weapon) { this.equippedWeapon = weapon; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = agility; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    public void move(Direction dir) { /* TODO */ }
    public void equip(Item weapon) { /* TODO */ }
}
