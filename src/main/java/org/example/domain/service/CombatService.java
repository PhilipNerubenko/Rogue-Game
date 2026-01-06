package org.example.domain.service;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Item;

import java.io.IOException;
import java.util.Random;

import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.TextMessages.*;

/**
 * Сервис для управления боями между игроком и врагами
 */
public class CombatService {

    /**
     * Атаковать врага
     * @param session текущая игровая сессия
     * @param enemy враг для атаки
     * @param sessionStat статистика сессии
     * @return сообщение о результате атаки
     */
    public String attackEnemy(GameSession session, Enemy enemy, SessionStat sessionStat) throws IOException {
        // Проверка промаха игрока
        if (!isHit(session.getPlayer().getAgility(), enemy.getAgility())) {
            sessionStat.incrementMissed();
            return MISSED;
        }

        // Проверка специальной способности врага "промах первого удара"
        if (enemy.hasAbility(Enemy.ABILITY_FIRST_MISS)) {
            enemy.removeAbility(Enemy.ABILITY_FIRST_MISS);
            sessionStat.incrementMissed();
            return MISSED_VAMPIRE;
        }

        // Расчет и нанесение урона
        int damage = Math.max(1, session.getPlayer().getStrength());
        enemy.setHealth(enemy.getHealth() - damage);

        String resultMessage = "You dealt " + damage + " dmg to " + enemy.getType();

        // Проверка смерти врага
        if (enemy.getHealth() <= 0) {
            int gold = calculateGoldDrop(enemy);
            Item goldItem = Item.createTreasure(gold);
            session.getPlayer().getInventory().add(goldItem);
            resultMessage += " - KILLED!, added " + gold;
        }

        sessionStat.incrementAttacks();
        return resultMessage;
    }

    /**
     * Удалить врага с карты и из списка врагов
     * @param session игровая сессия
     * @param enemy враг для удаления
     * @param asciiMap ASCII карта игры
     */
    public void removeEnemy(GameSession session, Enemy enemy, char[][] asciiMap) {
        // Очистка позиции врага на карте
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + MAP_OFFSET_X, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        // Удаление врага из списка активных врагов
        session.getEnemies().remove(enemy);
    }

    /**
     * Атаковать игрока
     * @param session текущая игровая сессия
     * @param enemy враг, атакующий игрока
     * @param guaranteed гарантированный ли удар (контратака)
     * @return сообщение о результате атаки
     */
    public String attackPlayer(GameSession session, Enemy enemy, boolean guaranteed) {
        int damage = Math.max(1, enemy.getStrength());

        // Гарантированная атака (например, контратака)
        if (guaranteed) {
            session.getPlayer().setHealth(session.getPlayer().getHealth() - damage);
            return enemy.getType() + " COUNTERATTACKS for " + damage + " dmg!";
        }

        // Проверка промаха врага
        if (!isHit(enemy.getAgility(), session.getPlayer().getAgility())) {
            return enemy.getType() + " missed!";
        }

        // Нанесение урона игроку
        session.getPlayer().setHealth(session.getPlayer().getHealth() - damage);
        StringBuilder message = new StringBuilder(enemy.getType() + " dealt " + damage + " dmg");

        // Обработка специальных способностей врага
        processEnemyAbilities(session, enemy, message);

        // Проверка смерти игрока
        if (session.getPlayer().getHealth() <= 0) {
            message.append(" - YOU DIED!");
        }

        return message.toString();
    }

    /**
     * Проверка попадания атаки
     * @param attackerAgility ловкость атакующего
     * @param defenderAgility ловкость защищающегося
     * @return true если атака попала
     */
    private static boolean isHit(int attackerAgility, int defenderAgility) {
        int baseChance = 50;
        int agilityDelta = attackerAgility - defenderAgility;
        int finalChance = Math.max(MIN_HIT_CHANCE,
                Math.min(MAX_HIT_CHANCE,
                        baseChance + agilityDelta * AGILITY_MULTIPLIER));
        Random rand = new Random();
        return rand.nextInt(100) < finalChance;
    }

    /**
     * Расчет количества золота за убитого врага
     * @param enemy убитый враг
     * @return количество золота
     */
    private int calculateGoldDrop(Enemy enemy) {
        int baseGold = 10; // Базовое значение

        // Модификаторы в зависимости от характеристик врага
        int hostilityBonus = enemy.getHostility() * 2;
        int strengthBonus = enemy.getStrength();
        int agilityBonus = enemy.getAgility();
        int healthBonus = enemy.getHealth() / 10; // 10% от максимального здоровья

        int totalGold = baseGold + hostilityBonus + strengthBonus +
                agilityBonus + healthBonus;

        // Добавление случайности ±20%
        Random rand = new Random();
        int variation = (int)(totalGold * 0.2);
        totalGold += rand.nextInt(variation * 2) - variation;

        return Math.max(1, totalGold);
    }

    /**
     * Обработка специальных способностей врага при атаке
     * @param session игровая сессия
     * @param enemy враг
     * @param message сообщение для дополнения
     */
    private void processEnemyAbilities(GameSession session, Enemy enemy, StringBuilder message) {
        // Способность вампира: уменьшение максимального здоровья
        if (enemy.hasAbility(Enemy.ABILITY_VAMPIRE_DRAIN)) {
            int currentMaxHealth = session.getPlayer().getMaximumHealth();
            session.getPlayer().setMaximumHealth(currentMaxHealth - 1);
            message.append(", reduced your max HP to ")
                    .append(session.getPlayer().getMaximumHealth());
        }

        // Способность змеи: усыпление игрока
        if (enemy.hasAbility(Enemy.ABILITY_SNAKE_SLEEP) &&
                new Random().nextInt(100) < SNAKE_SLEEP_CHANCE) {
            session.getPlayer().setSleepTurns(true);
            message.append(", put you to sleep!");
        } else {
            message.append("!");
        }

        // Способность невидимого врага: снятие невидимости после атаки
        if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
            enemy.setInvisible(false);
        }

        // Способность огра: отдых после атаки
        if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
            enemy.setRestTurns(OGRE_REST_DURATION);
        }
    }
}