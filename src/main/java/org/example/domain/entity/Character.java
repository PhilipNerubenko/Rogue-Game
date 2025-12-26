package org.example.domain.entity;

import java.util.List;

public class Character {
    private int maxHealth;
    private int health;
    private int agility;
    private int strength;
    private Inventory inventory;
    private boolean sleepTurns;

    // Основной конструктор (новый подход)
    public Character(int maxHealth, int agility, int strength) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.agility = agility;
        this.strength = strength;
        this.inventory = new Inventory();
        this.sleepTurns = false;
    }

    // Конструктор для обратной совместимости (старый подход)
    public Character(int maxHealth, int health, int agility, int strength, List<Item> backpack) {
        this.maxHealth = maxHealth;
        this.health = health;
        this.agility = agility;
        this.strength = strength;
        this.inventory = new Inventory();
        this.sleepTurns = false;

        // Добавляем предметы из старого рюкзака в новый инвентарь
        if (backpack != null) {
            for (Item item : backpack) {
                this.inventory.add(item);
            }
        }
    }

    // Геттеры и сеттеры

    // Для нового подхода
    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        // Не даем здоровью превысить новый максимум
        if (this.health > maxHealth) {
            this.health = maxHealth;
        }
    }

    // Для обратной совместимости со старым кодом
    public int getMaximumHealth() {
        return maxHealth;
    }

    public void setMaximumHealth(int maximumHealth) {
        setMaxHealth(maximumHealth);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        // Здоровье не может быть меньше 0 и больше максимума
        if (health < 0) this.health = 0;
        else this.health = Math.min(health, maxHealth);
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

    // Новый подход с Inventory
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

    // Методы для обратной совместимости со старым кодом
    public List<Item> getBackpack() {
        // Собираем все предметы из всех слотов
        // ВНИМАНИЕ: Это может быть неэффективно для больших инвентарей
        // Лучше переходить на использование getInventory()
        List<Item> allItems = new java.util.ArrayList<>();
        for (ItemType type : ItemType.values()) {
            allItems.addAll(inventory.getItems(type));
        }
        return allItems;
    }

    public void setBackpack(List<Item> backpack) {
        // Очищаем текущий инвентарь
        this.inventory = new Inventory();
        // Добавляем все предметы
        if (backpack != null) {
            for (Item item : backpack) {
                this.inventory.add(item);
            }
        }
    }

    // Вспомогательные методы

    public void heal(int amount) {
        this.health = Math.min(maxHealth, this.health + amount);
    }

    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public String getStatus() {
        return String.format("HP: %d/%d | STR: %d | AGI: %d",
                health, maxHealth, strength, agility);
    }

    // Методы для работы с инвентарем (удобные обертки)
    public boolean addItem(Item item) {
        return inventory.add(item);
    }

    public Item takeItem(ItemType type, int index) {
        return inventory.take(type, index);
    }

    public int getItemCount(ItemType type) {
        return inventory.count(type);
    }

    public boolean isInventoryFull(ItemType type) {
        return inventory.isFull(type);
    }

    public List<Item> getItemsByType(ItemType type) {
        return inventory.getItems(type);
    }
}