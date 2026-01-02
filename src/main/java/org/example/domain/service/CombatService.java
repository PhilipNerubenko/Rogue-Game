package org.example.domain.service;


import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;

import org.example.domain.entity.Item;
import org.example.presentation.Renderer;


import java.io.IOException;
import java.util.Random;

import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.TextMessages.*;

/**
 * Сервис обслуживающий сражение
 */

public class CombatService {

    public String attackEnemy(GameSession session, Enemy enemy, SessionStat sessionStat) throws IOException {

        // Проверка промаха
        if (!isHit(session.getPlayer().getAgility(), enemy.getAgility())) {
            sessionStat.incrementMissed();
            return MISSED;
        }

        // Способность: первый удар промах
        if (enemy.hasAbility(Enemy.ABILITY_FIRST_MISS)) {
            enemy.removeAbility(Enemy.ABILITY_FIRST_MISS);
            sessionStat.incrementMissed();
            return MISSED_VAMPIRE;
        }

        // Наносим урон
        int damage = Math.max(1, session.getPlayer().getStrength());
        enemy.setHealth(enemy.getHealth() - damage);

        String msg = "You dealt " + damage + " dmg to " + enemy.getType();
        if (enemy.getHealth() <= 0) {
            // ДОБАВИТЬ СЮДА ЛОГИКУ ДОБАВЛЕНИЯ ЗОЛОТА!
            int gold = calculateGoldDrop(enemy);


            // Создаем предмет "золото"
            Item goldItem = Item.createTreasure(gold);

            // Добавляем в инвентарь игрока
            session.getPlayer().getInventory().add(goldItem);

            msg += " - KILLED!, added " + gold;
        }

        sessionStat.incrementAttacks(); // Увеличиваем счётчик атак


        return msg;
    }

    public void removeEnemy(GameSession session, Enemy enemy, char[][] asciiMap) {
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + MAP_OFFSET_X, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        session.getEnemies().remove(enemy);
    }

    public String attackPlayer(GameSession session, Enemy enemy, boolean guaranteed) {

        int damage = Math.max(1, enemy.getStrength());

        if (guaranteed) {
            session.getPlayer().setHealth(session.getPlayer().getHealth() - damage);
            return enemy.getType() + " COUNTERATTACKS for " + damage + " dmg!";
        }

        // Обычная атака с шансом промаха
        if (!isHit(enemy.getAgility(), session.getPlayer().getAgility())) {
            return enemy.getType() + " missed!";
        }

        session.getPlayer().setHealth(session.getPlayer().getHealth() - damage);
        StringBuilder message = new StringBuilder(enemy.getType() + " dealt " + damage + " dmg");

        // Ваши спец. способности...
        if (enemy.hasAbility(Enemy.ABILITY_VAMPIRE_DRAIN)) {
            int currentMaxHealth = session.getPlayer().getMaximumHealth();
            session.getPlayer().setMaximumHealth(currentMaxHealth - 1);
            message.append(", reduced your max HP to ").append(session.getPlayer().getMaximumHealth());
        }

        if (enemy.hasAbility(Enemy.ABILITY_SNAKE_SLEEP) && new Random().nextInt(100) < SNAKE_SLEEP_CHANCE) {
            session.getPlayer().setSleepTurns(true);
            message.append(", put you to sleep!");
        } else {
            message.append("!");
        }

        if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
            enemy.setInvisible(false);
        }

        if (session.getPlayer().getHealth() <= 0) message.append(" - YOU DIED!");

        // После удара огра — выставляем отдых
        if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
            enemy.setRestTurns(OGRE_REST_DURATION); // отдых 1 ход
        }

        return message.toString();
    }

    private static boolean isHit(int attackerAgility, int defenderAgility) {
        int baseChance = 50;
        int agilityDelta = attackerAgility - defenderAgility;
        int finalChance = Math.max(MIN_HIT_CHANCE, Math.min(MAX_HIT_CHANCE, baseChance + agilityDelta * AGILITY_MULTIPLIER));
        Random rand = new Random();
        return rand.nextInt(100) < finalChance;
    }

    private int calculateGoldDrop(Enemy enemy) {
        // Формула для расчета золота в зависимости от характеристик врага
        int baseGold = 10; // Базовое значение

        // Модификаторы в зависимости от характеристик врага
        int hostilityBonus = enemy.getHostility() * 2;
        int strengthBonus = enemy.getStrength() * 1;
        int agilityBonus = enemy.getAgility() * 1;
        int healthBonus = enemy.getHealth() / 10; // 10% от максимального здоровья

        int totalGold = baseGold + hostilityBonus + strengthBonus +
                agilityBonus + healthBonus;

        // Добавляем случайность ±20%
        Random rand = new Random();
        int variation = (int)(totalGold * 0.2);
        totalGold += rand.nextInt(variation * 2) - variation;

        // Минимум 1 золото
        return Math.max(1, totalGold);
    }
}
