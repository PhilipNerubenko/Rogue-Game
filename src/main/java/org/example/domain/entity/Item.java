package org.example.domain.entity;

public class Item {
    private String type;
    private String subType;
    private int health;
    private int maxHealth;
    private int agility;
    private int strength;
    private int value;

    private int x = -1;
    private int y = -1;

    // КОНСТАНТА для кулаков
    public static final int FISTS_STRENGTH = 3;

    public Item(String type, String subType, int health, int maxHealth, int agility, int strength, int value) {
        this.type = type;
        this.subType = subType;
        this.health = health;
        this.maxHealth = maxHealth;
        this.agility = agility;
        this.strength = strength;
        this.value = value;
    }

    // ФАБРИЧНЫЙ МЕТОД для создания кулаков
    public static Item createFists() {
        return new Item(
                ItemType.WEAPON.name().toLowerCase(), // "weapon"
                "fists",                              // подтип
                0, 0, 0,                              // нет эффектов
                FISTS_STRENGTH,                       // базовый урон 3
                0                                     // нет стоимости
        );
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
