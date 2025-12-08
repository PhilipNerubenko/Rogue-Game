package org.example.domain.service;


import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;

import java.util.Random;

import static org.example.App.printLine;
import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.MISSED;
import static org.example.config.GameConstants.TextMessages.MISSED_VAMPIRE;

/**
 * Сервис обслуживающий сражение
 */

public class CombatService {

    public void attackEnemy(GameSession session, Enemy enemy) {
        CharColor attackColor = new CharColor(CharColor.YELLOW, CharColor.BLACK); // Цвет можно вынести глобально, если уже есть

        // Проверка промаха
        if (!isHit(session.getPlayer().getAgility(), enemy.getAgility())) {
            printLine(MESSAGE_LINE_1, MISSED, attackColor, MAP_WIDTH);
            return;
        }

        // Способность: первый удар промах
        if (enemy.hasAbility(Enemy.ABILITY_FIRST_MISS)) {
            printLine(MESSAGE_LINE_1, MISSED_VAMPIRE, attackColor, MAP_WIDTH);
            enemy.removeAbility(Enemy.ABILITY_FIRST_MISS);
            return;
        }

        // Наносим урон
        int damage = Math.max(1, session.getPlayer().getStrength());
        enemy.setHealth(enemy.getHealth() - damage);

        String msg = "You dealt " + damage + " dmg to " + enemy.getType();
        if (enemy.getHealth() <= 0) msg += " - KILLED!";

        printLine(MESSAGE_LINE_1, msg, attackColor, MAP_WIDTH);
    }

    public void removeEnemy(GameSession session, Enemy enemy, char[][] asciiMap) {
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + MAP_OFFSET_X, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        session.getEnemies().remove(enemy);
    }

    public void attackPlayer(GameSession session, Enemy enemy, boolean guaranteed) {
        CharColor attackColor = new CharColor(CharColor.YELLOW, CharColor.BLACK);

        int damage = Math.max(1, enemy.getStrength());

        if (guaranteed) {
            session.getPlayer().setHealth(session.getPlayer().getHealth() - damage);
            printLine(MESSAGE_LINE_2, enemy.getType() + " COUNTERATTACKS for " + damage + " dmg!", attackColor, MAP_WIDTH);
            return;
        }

        // Обычная атака с шансом промаха
        if (!isHit(enemy.getAgility(), session.getPlayer().getAgility())) {
            printLine(MESSAGE_LINE_2, enemy.getType() + " missed!", attackColor, MAP_WIDTH);
            return;
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
        printLine(MESSAGE_LINE_2, message.toString(), attackColor, MAP_WIDTH);

        // После удара огра — выставляем отдых
        if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
            enemy.setRestTurns(OGRE_REST_DURATION); // отдых 1 ход
        }
    }

    private static boolean isHit(int attackerAgility, int defenderAgility) {
        int baseChance = 50;
        int agilityDelta = attackerAgility - defenderAgility;
        int finalChance = Math.max(MIN_HIT_CHANCE, Math.min(MAX_HIT_CHANCE, baseChance + agilityDelta * AGILITY_MULTIPLIER));
        Random rand = new Random();
        return rand.nextInt(100) < finalChance;
    }
}
