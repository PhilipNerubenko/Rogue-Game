package org.example.domain.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Рюкзак (5 отсеков по типу , в отсек вмещаеться не более 9 предметов )
public class Inventory {
    private final Map<ItemType, List<Item>> slots = new EnumMap<>(ItemType.class);
    private static final int MAX_PER_TYPE = 9;
    private int totalTreasureValue = 0; // Для сокровищ

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
        // Сокровища - особый случай
        if (item.getType().equalsIgnoreCase("treasure")) {
            totalTreasureValue += item.getValue();
            return true;
        }

        // Пробуем определить тип предмета
        ItemType type;
        try {
            type = ItemType.valueOf(item.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Если тип не распознан, логируем ошибку и возвращаем false
            System.err.println("ERROR: Unknown item type: " + item.getType());
            return false;
        }

        // Получаем слот для этого типа
        List<Item> slot = slots.get(type);
        if (slot == null) {
            // На всякий случай инициализируем слот, если его нет
            System.err.println("WARNING: Slot for type " + type + " was null, initializing");
            slot = new ArrayList<>();
            slots.put(type, slot);
        }

        // Проверяем, полон ли слот
        if (slot.size() >= MAX_PER_TYPE) {
            return false;
        }
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

    public int getTreasureValue() {
        // Логика подсчета сокровищ
        int total = 0;
        for (Item item : slots.get(ItemType.TREASURE)) {
            total += item.getValue();
        }
        return total;
    }

    /**
     * Получить форматированное представление инвентаря для отображения
     */
    public String getFormattedContents() {
        StringBuilder sb = new StringBuilder();

        // Общие предметы
        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue; // сокровища отдельно

            List<Item> slot = slots.get(type);
            if (!slot.isEmpty()) {
                sb.append(String.format("%s (%d): ", type.name(), slot.size()));

                // Группируем одинаковые предметы
                Map<String, Integer> counts = new java.util.HashMap<>();
                for (Item item : slot) {
                    counts.put(item.getSubType(),
                            counts.getOrDefault(item.getSubType(), 0) + 1);
                }

                List<String> itemStrings = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > 1) {
                        itemStrings.add(entry.getKey() + " x" + entry.getValue());
                    } else {
                        itemStrings.add(entry.getKey());
                    }
                }

                sb.append(String.join(", ", itemStrings));
                sb.append("\n");
            }
        }

        // Сокровища отдельно
        if (totalTreasureValue > 0) {
            sb.append(String.format("TREASURE: %d gold\n", totalTreasureValue));
        }

        return sb.toString().trim();
    }

    /**
     * Получить подробный список предметов (как в getItemAt)
     */
    public List<String> getDetailedItemList() {
        List<String> result = new ArrayList<>();
        int index = 1;

        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            for (Item item : slots.get(type)) {
                String itemInfo = String.format("%d. %s (%s) - ",
                        index++,
                        item.getSubType(),
                        type.name().toLowerCase()
                );

                // Добавляем эффекты
                List<String> effects = new ArrayList<>();
                if (item.getHealth() > 0) effects.add("HP+" + item.getHealth());
                if (item.getMaxHealth() > 0) effects.add("MaxHP+" + item.getMaxHealth());
                if (item.getAgility() > 0) effects.add("AGI+" + item.getAgility());
                if (item.getStrength() > 0) effects.add("STR+" + item.getStrength());
                if (item.getValue() > 0) effects.add("Value:" + item.getValue());

                if (!effects.isEmpty()) {
                    itemInfo += String.join(", ", effects);
                } else {
                    itemInfo += "no effects";
                }

                result.add(itemInfo);
            }
        }

        return result;
    }
}
