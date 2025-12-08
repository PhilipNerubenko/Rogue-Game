package org.example.domain.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Рюкзак (5 отсеков по типу , в отсек вмещаеться не более 9 предметов )
public class Inventory {
    private final Map<ItemType, List<Item>> slots = new EnumMap<>(ItemType.class);
    private static final int MAX_PER_TYPE = 9;

    public Inventory() {
        // Инициализируем все слоты
        for (ItemType type : ItemType.values()) {
            slots.put(type, new ArrayList<>());
        }
    }

    /**
     * Добавить предмет. Возвращает false, если слот заполнен.
     */
    public boolean add(Item item) {
        List<Item> slot = slots.get(item.getType());
        if (slot.size() >= MAX_PER_TYPE) return false;
        slot.add(item);
        return true;
    }

    /**
     * Взять предмет из слота (удаляет из инвентаря).
     */
    public Item take(ItemType type, int index) {
        List<Item> slot = slots.get(type);
        if (index < 0 || index >= slot.size()) return null;
        return slot.remove(index);
    }

    /**
     * Посмотреть, сколько предметов в слоте.
     */
    public int count(ItemType type) {
        return slots.get(type).size();
    }

    /**
     * Получить список всех предметов типа (например, для отрисовки).
     */
    public List<Item> getItems(ItemType type) {
        return new ArrayList<>(slots.get(type)); // Возвращаем копию
    }

    public boolean isFull(ItemType type) {
        return count(type) >= MAX_PER_TYPE;
    }
}
