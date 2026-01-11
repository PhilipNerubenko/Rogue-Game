package org.example.domain.factory;

import org.example.domain.entity.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Генератор предметов для подземелья
 * Создает случайные предметы в зависимости от уровня сложности
 */
public class ItemGenerator {

    // Общий генератор случайных чисел для всего класса
    private static final Random rand = new Random();

    /**
     * Генерирует список предметов для указанного уровня подземелья
     *
     * @param level Уровень подземелья (от 1 и выше)
     * @return Список сгенерированных предметов
     */
    public static List<Item> generateForLevel(int level) {
        List<Item> items = new ArrayList<>();

        // Базовое количество предметов увеличивается с уровнем
        int itemCount = 12 + level * 5;

        // Генерация каждого предмета
        for (int i = 0; i < itemCount; i++) {
            Item item = generateSingleItem(level);
            items.add(item);
        }

        return items;
    }

    /**
     * Генерирует один случайный предмет
     *
     * @param level Уровень подземелья для расчета характеристик
     * @return Сгенерированный предмет
     */
    private static Item generateSingleItem(int level) {
        int roll = rand.nextInt(100);

        // Распределение вероятностей:
        // 0-7: Сокровища (8%)
        // 8-27: Еда (20%)
        // 28-49: Эликсиры (22%)
        // 50-67: Свитки (18%)
        // 68-99: Оружие (32%)

        if (roll < 8) {
            return generateTreasure(level);
        } else if (roll < 28) {
            return generateFood(level);
        } else if (roll < 50) {
            return generateElixir(level);
        } else if (roll < 68) {
            return generateScroll();
        } else {
            return generateWeapon(level);
        }
    }

    /**
     * Генерирует сокровище (золото)
     */
    private static Item generateTreasure(int level) {
        // Количество золота увеличивается с уровнем
        int goldAmount = 50 + rand.nextInt(100) + level * 40;
        return new Item("treasure", "Gold", 0, 0, 0, 0, goldAmount);
    }

    /**
     * Генерирует еду (восстанавливает здоровье)
     */
    private static Item generateFood(int level) {
        // Эффективность еды увеличивается с уровнем
        int healthRestore = 8 + level + rand.nextInt(6);
        return new Item("food", "Food", healthRestore, 0, 0, 0, 0);
    }

    /**
     * Генерирует эликсир (увеличивает характеристики)
     */
    private static Item generateElixir(int level) {
        boolean isStrength = rand.nextBoolean();
        String name;
        int agilityBonus;
        int strengthBonus;

        if (isStrength) {
            strengthBonus = 4 + level;
            name = "Strength Potion (+" + strengthBonus + ")";
            agilityBonus = 0;
        } else {
            agilityBonus = 4 + level;
            name = "Agility Potion (+" + agilityBonus + ")";
            strengthBonus = 0;
        }

        return new Item("elixir", name, 0, 0, agilityBonus, strengthBonus, 0);
    }

    /**
     * Генерирует магический свиток
     */
    private static Item generateScroll() {
        int type = rand.nextInt(3);

        return switch (type) {
            case 0 -> new Item("scroll", "Strength Scroll (+2)", 0, 0, 0, 2, 0);
            case 1 -> new Item("scroll", "Agility Scroll (+2)", 0, 0, 2, 0, 0);
            default -> new Item("scroll", "Health Scroll (+4 HP)", 0, 4, 0, 0, 0);
        };
    }

    /**
     * Генерирует оружие
     */
    private static Item generateWeapon(int level) {
        String name;
        int damageBonus;

        if (rand.nextBoolean()) {
            // Клинок: средний урон с небольшим разбросом
            damageBonus = 4 + level + rand.nextInt(3);
            name = "Blade (damage +" + damageBonus + ")";
        } else {
            // Меч: вариативный урон
            damageBonus = 3 + level + rand.nextInt(4);
            name = "Sword (damage +" + damageBonus + ")";
        }

        return new Item("weapon", name, 0, 0, 0, damageBonus, 0);
    }
}