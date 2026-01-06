package org.example.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class Character {
    // Основные характеристики персонажа
    private int maxHealth;
    private int health;
    private int agility;
    private int strength;
    private Inventory inventory;
    private boolean sleepTurns; // Флаг состояния сна (пропуск ходов)

    /**
     * Основной конструктор для нового персонажа.
     * @param maxHealth максимальное здоровье
     * @param agility ловкость персонажа
     * @param strength сила персонажа
     */
    public Character(int maxHealth, int agility, int strength) {
        this.maxHealth = maxHealth;
        this.health = maxHealth; // Начинаем с полного здоровья
        this.agility = agility;
        this.strength = strength;
        this.inventory = new Inventory();
        this.sleepTurns = false;
    }

    /**
     * Конструктор для обратной совместимости со старым кодом.
     * @param maxHealth максимальное здоровье
     * @param health текущее здоровье
     * @param agility ловкость персонажа
     * @param strength сила персонажа
     * @param backpack старый формат инвентаря (список предметов)
     */
    public Character(int maxHealth, int health, int agility, int strength, List<Item> backpack) {
        this.maxHealth = maxHealth;
        this.health = health;
        this.agility = agility;
        this.strength = strength;
        this.inventory = new Inventory();
        this.sleepTurns = false;

        // Миграция данных из старого формата инвентаря в новый
        if (backpack != null) {
            for (Item item : backpack) {
                this.inventory.add(item);
            }
        }
    }

    // ============ Геттеры и сеттеры ============

    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Устанавливает максимальное здоровье.
     * Если текущее здоровье превышает новый максимум, оно уменьшается до максимума.
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (this.health > maxHealth) {
            this.health = maxHealth;
        }
    }

    /**
     * Метод для обратной совместимости со старым кодом.
     * @return максимальное здоровье
     */
    public int getMaximumHealth() {
        return maxHealth;
    }

    /**
     * Метод для обратной совместимости со старым кодом.
     */
    public void setMaximumHealth(int maximumHealth) {
        setMaxHealth(maximumHealth);
    }

    public int getHealth() {
        return health;
    }

    /**
     * Устанавливает текущее здоровье.
     * Значение ограничивается диапазоном от 0 до maxHealth.
     */
    public void setHealth(int health) {
        if (health < 0) {
            this.health = 0;
        } else {
            this.health = Math.min(health, maxHealth);
        }
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

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isSleepTurns() {
        return sleepTurns;
    }

    public void setSleepTurns(boolean sleepTurns) {
        this.sleepTurns = sleepTurns;
    }

    // ============ Методы обратной совместимости ============

    /**
     * Возвращает все предметы в инвентаре как единый список (старый формат).
     * ВНИМАНИЕ: Может быть неэффективно для больших инвентарей.
     * Рекомендуется использовать getInventory() для новой логики.
     */
    public List<Item> getBackpack() {
        List<Item> allItems = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            allItems.addAll(inventory.getItems(type));
        }
        return allItems;
    }

    /**
     * Заменяет текущий инвентарь списком предметов (старый формат).
     */
    public void setBackpack(List<Item> backpack) {
        this.inventory = new Inventory();
        if (backpack != null) {
            for (Item item : backpack) {
                this.inventory.add(item);
            }
        }
    }

    // ============ Основные методы персонажа ============

    /**
     * Восстанавливает здоровье на указанное количество.
     * Здоровье не может превысить максимум.
     */
    public void heal(int amount) {
        this.health = Math.min(maxHealth, this.health + amount);
    }

    /**
     * Наносит урон персонажу.
     * Здоровье не может стать ниже 0.
     */
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    /**
     * Проверяет, жив ли персонаж.
     */
    public boolean isAlive() {
        return health > 0;
    }

    /**
     * Проверяет, мертв ли персонаж.
     */
    public boolean isDead() {
        return health <= 0;
    }

    /**
     * Возвращает строковое представление состояния персонажа.
     */
    public String getStatus() {
        return String.format("HP: %d/%d | STR: %d | AGI: %d",
                health, maxHealth, strength, agility);
    }

    // ============ Упрощенные методы для работы с инвентарем ============

    /**
     * Добавляет предмет в инвентарь.
     * @return true если предмет успешно добавлен
     */
    public boolean addItem(Item item) {
        return inventory.add(item);
    }

    /**
     * Берет предмет из инвентаря.
     * @param type тип предмета
     * @param index индекс предмета в слоте
     * @return взятый предмет или null
     */
    public Item takeItem(ItemType type, int index) {
        return inventory.take(type, index);
    }

    /**
     * Возвращает количество предметов указанного типа.
     */
    public int getItemCount(ItemType type) {
        return inventory.count(type);
    }

    /**
     * Проверяет, заполнен ли слот для указанного типа предметов.
     */
    public boolean isInventoryFull(ItemType type) {
        return inventory.isFull(type);
    }

    /**
     * Возвращает список предметов указанного типа.
     */
    public List<Item> getItemsByType(ItemType type) {
        return inventory.getItems(type);
    }
}