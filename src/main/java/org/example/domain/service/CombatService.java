package org.example.domain.service;


import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;

/**
 * Сервич обслуживающий  сражение
 */

public class CombatService {

    public void attackEnemy(GameSession session, Enemy enemy) {
        // СКОПИРУЙТЕ СЮДА весь код из App.attackEnemy():
        int playerDamage = 10;
        int actualDamage = Math.max(1, playerDamage - enemy.getAgility());
        enemy.setHealth(enemy.getHealth() - actualDamage);

        Toolkit.printString("You have applied" + actualDamage + " damage " + enemy.getType() + "!",
                3, 28, new CharColor(CharColor.YELLOW, CharColor.BLACK));
    }

    public void removeEnemy(GameSession session, Enemy enemy, char[][] asciiMap) {
        // СКОПИРУЙТЕ СЮДА весь код из App.removeEnemy():
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + 3, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        session.getEnemies().remove(enemy);
    }

    public void attackPlayer(Enemy enemy) {
        // СКОПИРУЙТЕ СЮДА весь код из App.attackPlayer():
        int damage = enemy.getStrength();
        Toolkit.printString("The enemy inflicted " + damage + " damage!",
                3, 27, new CharColor(CharColor.RED, CharColor.BLACK));
    }
}
