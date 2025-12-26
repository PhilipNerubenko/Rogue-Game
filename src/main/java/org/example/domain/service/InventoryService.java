package org.example.domain.service;


import org.example.config.GameConstants;

import org.example.domain.entity.Item;

import java.util.ArrayList;
import java.util.List;

public class InventoryService {

    private final List<Item> items = new ArrayList<>();

    private static final int MAX_SIZE = GameConstants.Player.SIZE_BACKPACK; // a-z


    // Добавить предмет
    public boolean add(Item item) {
        if (items.size() >= MAX_SIZE) {
            return false; // инвентарь полон
        }
        items.add(item);
        return true;
    }

    // Удалить предмет по индексу
    public void remove(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    // Получить предмет по индексу
    public Item get(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    // Получить весь инвентарь
    public List<Item> getAll() {
        return new ArrayList<>(items); // копия для безопасности
    }

    // Размер инвентаря
    public int size() {
        return items.size();
    }

    // Инвентарь полон?
    public boolean isFull() {
        return items.size() >= MAX_SIZE;
    }

    // Очистить инвентарь
    public void clear() {
        items.clear();
    }
}