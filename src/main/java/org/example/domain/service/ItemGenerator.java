package org.example.domain.service;

import org.example.domain.entity.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemGenerator {

    private static final Random rand = new Random();

    /**
     * Генерирует список предметов для указанного уровня подземелья
     * Чем выше уровень - тем больше предметов и они лучше
     */
    public static List<Item> generateForLevel(int level) {
        List<Item> items = new ArrayList<>();

        // Количество предметов растёт с уровнем
        int itemCount = 12 + level * 5; // на 1-м уровне ~17, на 5-м ~37

        for (int i = 0; i < itemCount; i++) {
            int roll = rand.nextInt(100);

            Item item;
            if (roll < 8) {
                // 8% шанс — сокровища
                item = new Item("treasure", "Золото", 0, 0, 0, 0,
                        50 + rand.nextInt(100) + level * 40);
            }
            else if (roll < 28) {
                // 20% — еда
                item = new Item("food", "Еда", 8 + level + rand.nextInt(6), 0, 0, 0, 0);
            }
            else if (roll < 50) {
                // 22% - эликсиры
                boolean isStrength = rand.nextBoolean();
                String name = isStrength
                        ? "Зелье силы (+" + (4 + level) + ")"
                        : "Зелье ловкости (+" + (4 + level) + ")";
                item = new Item("elixir", name, 0, 0,
                        isStrength ? 0 : 4 + level,
                        isStrength ? 4 + level : 0,
                        0);
            }
            else if (roll < 68) {
                // 18% - свитки
                int type = rand.nextInt(3);
                item = switch (type) {
                    case 0 -> new Item("scroll", "Свиток силы (+2)", 0, 0, 0, 2, 0);
                    case 1 -> new Item("scroll", "Свиток ловкости (+2)", 0, 0, 2, 0, 0);
                    default -> new Item("scroll", "Свиток жизни (+4 HP)", 0, 4, 0, 0, 0);
                };
            }
            else {
                // 32% - оружие
                String name = rand.nextBoolean()
                        ? "Клинок (урон +" + (4 + level + rand.nextInt(3)) + ")"
                        : "Меч (урон +" + (3 + level + rand.nextInt(4)) + ")";
                item = new Item("weapon", name, 0, 0, 0,
                        4 + level + rand.nextInt(4), 0);
            }

            items.add(item);
        }

        return items;
    }
}