package org.example.domain.entity;

import org.example.domain.enums.ItemType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Инвентарь с 5 отсеками по типам предметов.
 * Каждый отсек вмещает не более 9 предметов одного типа.
 */
public class Inventory {
    // Максимальное количество предметов одного типа
    private static final int MAX_PER_TYPE = 9;

    // Слоты для хранения предметов, сгруппированные по типу
    private final Map<ItemType, List<Item>> slots = new EnumMap<>(ItemType.class);

    // Общая стоимость сокровищ (вычисляется при добавлении для оптимизации)
    private int totalTreasureValue = 0;

    /**
     * Конструктор инициализирует пустые слоты для всех типов предметов.
     */
    public Inventory() {
        for (ItemType type : ItemType.values()) {
            slots.put(type, new ArrayList<>());
        }
    }

    /**
     * Добавляет предмет в соответствующий слот инвентаря.
     *
     * @param item предмет для добавления
     * @return true если предмет успешно добавлен, false если слот заполнен
     */
    public boolean add(Item item) {
        // Особый случай для сокровищ - учитываем только общую стоимость
        if (item.getType().equalsIgnoreCase("treasure")) {
            totalTreasureValue += item.getValue();
            slots.get(ItemType.TREASURE).add(item);
            return true;
        }

        // Определяем тип предмета
        ItemType type;
        try {
            type = ItemType.valueOf(item.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Unknown item type: " + item.getType());
            return false;
        }

        // Проверяем наличие места в слоте
        List<Item> slot = slots.get(type);
        if (slot.size() >= MAX_PER_TYPE) {
            return false;
        }

        slot.add(item);
        return true;
    }

    /**
     * Извлекает предмет из инвентаря по типу и индексу.
     *
     * @param type тип предмета
     * @param index индекс в списке предметов данного типа
     * @return извлеченный предмет или null если индекс некорректен
     */
    public Item take(ItemType type, int index) {
        List<Item> slot = slots.get(type);
        if (index < 0 || index >= slot.size()) return null;
        return slot.remove(index);
    }

    /**
     * Возвращает количество предметов указанного типа.
     */
    public int count(ItemType type) {
        return slots.get(type).size();
    }

    /**
     * Возвращает копию списка предметов указанного типа.
     * Изменения в возвращаемом списке не влияют на инвентарь.
     */
    public List<Item> getItems(ItemType type) {
        return new ArrayList<>(slots.get(type));
    }

    /**
     * Проверяет, заполнен ли слот для указанного типа.
     */
    public boolean isFull(ItemType type) {
        return count(type) >= MAX_PER_TYPE;
    }

    /**
     * Возвращает общую стоимость всех сокровищ.
     */
    public int getTreasureValue() {
        return totalTreasureValue;
    }

    /**
     * Форматирует содержимое инвентаря для отображения.
     * Группирует одинаковые предметы и показывает их количество.
     */
    public String getFormattedContents() {
        StringBuilder sb = new StringBuilder();

        // Обрабатываем все типы кроме сокровищ
        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            List<Item> slot = slots.get(type);
            if (!slot.isEmpty()) {
                sb.append(String.format("%s (%d): ", type.name(), slot.size()));

                // Группируем одинаковые предметы по подтипу
                Map<String, Integer> counts = new java.util.HashMap<>();
                for (Item item : slot) {
                    counts.put(item.getSubType(),
                            counts.getOrDefault(item.getSubType(), 0) + 1);
                }

                // Формируем строки для отображения
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

        // Отдельно выводим общую стоимость сокровищ
        if (totalTreasureValue > 0) {
            sb.append(String.format("TREASURE: %d gold\n", totalTreasureValue));
        }

        return sb.toString().trim();
    }

    /**
     * Возвращает подробный список всех предметов с их характеристиками.
     * Каждый предмет имеет уникальный порядковый номер.
     */
    public List<String> getDetailedItemList() {
        List<String> result = new ArrayList<>();
        int itemNumber = 1;

        for (ItemType type : ItemType.values()) {
            if (type == ItemType.TREASURE) continue;

            for (Item item : slots.get(type)) {
                // Формируем базовую информацию о предмете
                String itemInfo = String.format("%d. %s (%s) - ",
                        itemNumber++,
                        item.getSubType(),
                        type.name().toLowerCase()
                );

                // Добавляем информацию о характеристиках предмета
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