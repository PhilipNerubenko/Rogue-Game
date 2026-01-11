package org.example.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.domain.enums.ItemType;

/**
 * Сущность предмета в игре.
 * Содержит информацию о типе предмета, его характеристиках и положении на карте.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    // Основные характеристики предмета
    private String type;        // Тип предмета (оружие, сокровище и т.д.)
    private String subType;     // Подтип (конкретный вид предмета)
    private int health;         // Влияние на здоровье при использовании
    private int maxHealth;      // Влияние на максимальное здоровье
    private int agility;        // Влияние на ловкость
    private int strength;       // Влияние на силу/урон
    private int value;          // Ценность предмета (для сокровищ)

    // Координаты на игровой карте (-1 означает отсутствие позиции)
    private int x = -1;
    private int y = -1;

    // Константы
    /** Базовая сила кулаков (используется когда у персонажа нет оружия) */
    public static final int FISTS_STRENGTH = 3;

    /**
     * Основной конструктор для десериализации JSON.
     * Используется Jackson для создания объектов из JSON данных.
     */
    @JsonCreator
    public Item(
            @JsonProperty("type") String type,
            @JsonProperty("subType") String subType,
            @JsonProperty("health") int health,
            @JsonProperty("maxHealth") int maxHealth,
            @JsonProperty("agility") int agility,
            @JsonProperty("strength") int strength,
            @JsonProperty("value") int value) {
        this.type = type;
        this.subType = subType;
        this.health = health;
        this.maxHealth = maxHealth;
        this.agility = agility;
        this.strength = strength;
        this.value = value;
    }

    /** Конструктор по умолчанию для Jackson */
    public Item() {
        this("", "", 0, 0, 0, 0, 0);
    }

    /**
     * Создает предмет "Кулаки".
     * Используется как оружие по умолчанию, когда у персонажа нет другого оружия.
     * @return Объект Item, представляющий кулаки
     */
    public static Item createFists() {
        return new Item(
                ItemType.WEAPON.name().toLowerCase(), // "weapon"
                "fists",                              // подтип
                0, 0, 0,                              // нет бонусов к характеристикам
                FISTS_STRENGTH,                       // базовый урон
                0                                     // нет стоимости
        );
    }

    /**
     * Создает сокровище с указанной ценностью.
     * @param value Ценность сокровища
     * @return Объект Item, представляющий сокровище
     */
    public static Item createTreasure(int value) {
        return new Item(
                ItemType.TREASURE.name().toLowerCase(), // "treasure"
                "gold",                                // подтип
                0, 0, 0, 0,                           // нет влияния на характеристики
                value                                 // ценность сокровища
        );
    }

    // Геттеры и сеттеры
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

    /**
     * Устанавливает позицию предмета на карте.
     * @param x Координата X
     * @param y Координата Y
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}