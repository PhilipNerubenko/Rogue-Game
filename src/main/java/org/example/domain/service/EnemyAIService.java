package org.example.domain.service;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;

import java.util.Random;



/**
 * подстраивание игры под уровень игрока
 */
public class EnemyAIService {
    public void moveEnemies(GameSession session,int playerX, int playerY, char[][] asciiMap) {
        for (Enemy enemy : session.getEnemies()) {
            if (enemy.getHealth() <= 0) continue;

            // Огр отдыхает
            if (enemy.getRestTurns() > 0) {
                enemy.setRestTurns(enemy.getRestTurns() - 1);
                continue;
            }

            // Вычисляем расстояние до игрока
            int dx = playerX - enemy.getX();
            int dy = playerY - enemy.getY();
            int distance = Math.max(Math.abs(dx), Math.abs(dy)); // Чебышевское расстояние

            // Если игрок в зоне враждебности - преследуем
            if (distance <= enemy.getHostility()) {
                moveEnemyTowardsPlayer(session, enemy, dx, dy, asciiMap);
            } else {
                // Иначе случайное движение или спец. паттерн
                moveEnemyIdle(session, enemy, asciiMap);
            }

            // Проверяем, достиг ли враг игрока
            if (enemy.getX() == playerX && enemy.getY() == playerY) {
                //attackPlayer(enemy);


                // Огр отдыхает после атаки
                if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
                    enemy.setRestTurns(1);
                }
            }
        }
    }

    public void moveEnemyTowardsPlayer(GameSession session, Enemy enemy, int dx, int dy, char[][] asciiMap) {
        int newX = enemy.getX();
        int newY = enemy.getY();

        // Перемещение по диагонали для змеиного мага
        if (enemy.hasAbility(Enemy.ABILITY_DIAGONAL_MOVE)) {
            newX += Integer.signum(dx);
            newY += Integer.signum(dy);
        } else {
            // Обычное движение (по горизонтали или вертикали)
            if (Math.abs(dx) > Math.abs(dy)) {
                newX += Integer.signum(dx);
            } else if (dy != 0) {
                newY += Integer.signum(dy);
            }
        }

        // Огр перемещается на 2 клетки
        if (enemy.hasAbility(Enemy.ABILITY_OGRE_CHARGE)) {
            newX += Integer.signum(dx);
            newY += Integer.signum(dy);
        }

        // Проверка столкновения с другими врагами
        if (getEnemyAt(session, newX, newY) == null) {
            // Проверка препятствий
            if (newX >= 0 && newX < asciiMap[0].length &&
                    newY >= 0 && newY < asciiMap.length &&
                    asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~' &&
                    asciiMap[newY][newX] != ' ') {

                // Затираем старую позицию
                Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                        enemy.getX() + 3, enemy.getY(),
                        new CharColor(CharColor.BLACK, CharColor.WHITE));

                enemy.setX(newX);
                enemy.setY(newY);
            }
        }
    }

    private void moveEnemyIdle(GameSession session,Enemy enemy, char[][] asciiMap) {
        // Призрак телепортируется
        if (enemy.hasAbility(Enemy.ABILITY_TELEPORT)) {
            Random rand = new Random();
            // Случайная позиция в пределах комнаты
            enemy.setX(enemy.getX() + rand.nextInt(5) - 2);
            enemy.setY(enemy.getY() + rand.nextInt(5) - 2);
            return;
        }

        // Обычное случайное движение
        Random rand = new Random();
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        int dir = rand.nextInt(4);
        int newX = enemy.getX() + dx[dir];
        int newY = enemy.getY() + dy[dir];

        if (getEnemyAt(session, newX, newY) == null && newX >= 0 && newX < asciiMap[0].length &&
                newY >= 0 && newY < asciiMap.length &&
                asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~' &&
                asciiMap[newY][newX] != ' ') {

            Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                    enemy.getX() + 3, enemy.getY(),
                    new CharColor(CharColor.BLACK, CharColor.WHITE));

            enemy.setX(newX);
            enemy.setY(newY);
        }
    }

    public void updateEnemyEffects(GameSession session,int playerX, int playerY) {
        for (Enemy enemy : session.getEnemies()) {
            // Призрак становится невидимым, если игрок далеко
            if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
                int distance = Math.max(Math.abs(playerX - enemy.getX()),
                        Math.abs(playerY - enemy.getY()));
                enemy.setInvisible(distance > enemy.getHostility());
            }
        }
    }


    public Enemy getEnemyAt(GameSession session,int x, int y) {
        for (Enemy enemy : session.getEnemies()) {
            if (enemy.getX() == x && enemy.getY() == y && enemy.getHealth() > 0) {
                return enemy;
            }
        }
        return null;
    }

}
