package org.example.domain.service;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.App;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.Player;
import org.example.domain.model.Room;
import org.example.presentation.Renderer;

import java.util.*;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.MAP_WIDTH;
import static org.example.config.GameConstants.ScreenConfig.MESSAGE_LINE_1;


/**
 * подстраивание игры под уровень игрока
 */
public class EnemyAIService {
    
    public List<String> witchMoveEnemiesPattern(GameSession session, CombatService combatService, int playerX, int playerY,
                                        char[][] asciiMap) {
        List<String> messages = new ArrayList<>();
        for (Enemy enemy : session.getEnemies()) {
            if (enemy.getHealth() <= 0) continue;

            // Обработка отдыха огра
            String restMessage = handleOgreRestTurn(session, combatService, playerX, playerY, enemy);
            if (restMessage != null) {
                messages.add(restMessage);
                continue; // Огр не двигается в этот ход
            }

            String attackMessage = tryAttackAdjacentPlayer(session, combatService, playerX, playerY, enemy);
            if (attackMessage != null) {
                messages.add(attackMessage);
                continue; // Атака завершила ход
            }

            int dx = playerX - enemy.getX();
            int dy = playerY - enemy.getY();
            int dist = Math.max(Math.abs(dx), Math.abs(dy));

            // Враг преследует только если игрок видим и в зоне враждебности
            if (dist <= enemy.getHostility() && canSeePlayer(enemy.getX(), enemy.getY(), playerX, playerY, asciiMap)) {
                if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
                    enemy.setInvisible(false);
                }
                moveEnemyChase(session, enemy, playerX, playerY, asciiMap);
            } else {
                if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
                    updateGhostEffect(enemy, playerX, playerY);
                }
                moveEnemyWander(session, enemy, asciiMap);
            }
        }
        return messages;
    }

    public String tryAttackAdjacentPlayer(GameSession session, CombatService combatService,  int playerX, int playerY, Enemy enemy) {
        boolean canAttack =
                (enemy.getX() == playerX && enemy.getY() == playerY - 1) ||
                        (enemy.getX() == playerX && enemy.getY() == playerY + 1) ||
                        (enemy.getX() == playerX - 1 && enemy.getY() == playerY) ||
                        (enemy.getX() == playerX + 1 && enemy.getY() == playerY);

        if (canAttack) {
            return combatService.attackPlayer(session, enemy, false);
        }
        return null;
    }

    public String handleOgreRestTurn(GameSession session, CombatService combatService, int playerX, int playerY, Enemy enemy) {
        if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST) && enemy.getRestTurns() > 0) {
            enemy.setRestTurns(enemy.getRestTurns() - 1);

            // Проверяем: игрок рядом — контратака
            boolean isAdjacent = (playerX == enemy.getX() && Math.abs(playerY - enemy.getY()) == 1) ||
                    (playerY == enemy.getY() && Math.abs(playerX - enemy.getX()) == 1);

            if (isAdjacent) {
                return combatService.attackPlayer(session, enemy, true); // true = гарантированная контратака
            } else {
                // Можно вывести сообщение, что огр отдыхает
                return enemy.getType() + " is resting...";
            }
        }
        return null;
    }

    public void moveEnemyWander(GameSession session, Enemy enemy, char[][] asciiMap) {
        char type = enemy.getType();

        switch (type) {
            case ZOMBIE: moveZombie(session, enemy, asciiMap); break;
            case VAMPIRE: moveVampire(session, enemy, asciiMap); break;
            case GHOST: moveGhost(session, enemy, asciiMap); break;
            case OGRE: moveOgre(session, enemy, asciiMap); break;
            case SNAKE_MAGE: moveSnakeMage(session, enemy, asciiMap); break;
        }
    }

    public void moveZombie(GameSession session, Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        int dir = rand.nextInt(FOUR_DIRECTIONS);
        int nx = enemy.getX() + dx[dir];
        int ny = enemy.getY() + dy[dir];

        if (!isWalkable(nx, ny, asciiMap)) return;
        if (getEnemyAt(session, nx, ny) != null) return;

        enemy.setX(nx);
        enemy.setY(ny);
    }

    public void moveVampire(GameSession session, Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        // 8 направлений
        int[] dx = {-1,-1,-1, 0,0, 1,1,1};
        int[] dy = {-1, 0, 1,-1,1,-1,0,1};

        int dir = rand.nextInt(EIGHT_DIRECTIONS);
        int nx = enemy.getX() + dx[dir];
        int ny = enemy.getY() + dy[dir];

        if (!isWalkable(nx, ny, asciiMap)) return;
        if (getEnemyAt(session, nx, ny) != null) return;

        enemy.setX(nx);
        enemy.setY(ny);
    }

    public void moveGhost(GameSession session, Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        for (int i = 0; i < 10; i++) { // 10 попыток найти свободную клетку
            int nx = enemy.getX() + rand.nextInt(7) - GHOST_TELEPORT_RANGE; // -3..3
            int ny = enemy.getY() + rand.nextInt(7) - GHOST_TELEPORT_RANGE;

            if (!isWalkable(nx, ny, asciiMap)) continue;
            if (getEnemyAt(session, nx, ny) != null) continue;

            enemy.setX(nx);
            enemy.setY(ny);
            return;
        }
    }

    public void moveOgre(GameSession session, Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        int dir = rand.nextInt(FOUR_DIRECTIONS);

        // Огр ходит на 2 клетки за раз
        int step1X = enemy.getX() + dx[dir];
        int step1Y = enemy.getY() + dy[dir];
        int step2X = enemy.getX() + dx[dir] * OGRE_DOUBLE_STEP;
        int step2Y = enemy.getY() + dy[dir] * OGRE_DOUBLE_STEP;

        // Проверяем обе клетки на проходимость
        if (isWalkable(step1X, step1Y, asciiMap) &&
                isWalkable(step2X, step2Y, asciiMap) &&
                getEnemyAt(session, step1X, step1Y) == null &&
                getEnemyAt(session, step2X, step2Y) == null) {

            enemy.setX(step2X);
            enemy.setY(step2Y);
        }
    }

    public void moveSnakeMage(GameSession session, Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        // Каждый ход выбираем случайную диагональ (-1 или 1 по X и Y)
        int[] choices = {-1, 1};
        enemy.setDiagX(choices[rand.nextInt(2)]);
        enemy.setDiagY(choices[rand.nextInt(2)]);

        int nx = enemy.getX() + enemy.getDiagX();
        int ny = enemy.getY() + enemy.getDiagY();

        // Проверка на проходимость
        if (!isWalkable(nx, ny, asciiMap) || getEnemyAt(session, nx, ny) != null) {
            // Если диагональ невозможна, пробуем противоположную
            nx = enemy.getX() - enemy.getDiagX();
            ny = enemy.getY() - enemy.getDiagY();
            if (!isWalkable(nx, ny, asciiMap) || getEnemyAt(session, nx, ny) != null) {
                // Если и так нельзя — стоим на месте
                return;
            }
        }

        enemy.setX(nx);
        enemy.setY(ny);
    }

    public void moveEnemyChase(GameSession session, Enemy enemy, int playerX, int playerY, char[][] asciiMap) {
        // Ищем путь, но не дальше, чем радиус враждебности монстра
        List<int[]> path = findPath(session, enemy.getX(), enemy.getY(), playerX, playerY, asciiMap);
        if (path != null && path.size() > 1) {
            // Первый шаг на пути к игроку
            int[] step = path.get(1);
            enemy.setX(step[0]);
            enemy.setY(step[1]);
        } else {
            moveEnemyWander(session, enemy, asciiMap);
        }
    }

    public List<int[]> findPath(GameSession session, int sx, int sy, int ex, int ey, char[][] asciiMap) {
        int height = asciiMap.length;
        int width = asciiMap[0].length;
        boolean[][] visited = new boolean[height][width];
        int[][][] prev = new int[height][width][2]; // для восстановления пути

        // Инициализация вручную
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                prev[i][j][0] = -1;
                prev[i][j][1] = -1;
            }
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{sx, sy});
        visited[sy][sx] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1];

            if (x == ex && y == ey) {
                // Восстанавливаем путь
                List<int[]> path = new ArrayList<>();
                while (x != -1 && y != -1) {
                    path.add(0, new int[]{x, y});
                    int px = prev[y][x][0];
                    int py = prev[y][x][1];
                    x = px;
                    y = py;
                }
                return path;
            }

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                if (visited[ny][nx]) continue;
                char tile = asciiMap[ny][nx];
                if (tile == W_WALL || tile == H_WALL || tile == EMPTINESS) continue;
                if (getEnemyAt(session, nx, ny) != null && !(nx == ex && ny == ey)) continue;

                visited[ny][nx] = true;
                prev[ny][nx][0] = x;
                prev[ny][nx][1] = y;
                queue.add(new int[]{nx, ny});
            }
        }

        return null;
    }

    private static void updateGhostEffect(Enemy enemy, int playerX, int playerY) {
        Random rand = new Random();

        // Призрак: периодически становится невидимым
        if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
            int distance = Math.max(Math.abs(playerX - enemy.getX()),
                    Math.abs(playerY - enemy.getY()));

            // Если далеко от игрока, высокая вероятность невидимости
            if (distance > enemy.getHostility()) {
                enemy.setInvisible(rand.nextInt(100) < GHOST_INVISIBILITY_FAR_CHANCE); // 80% шанс
            } else {
                // Близко к игроку - реже невидимость
                enemy.setInvisible(rand.nextInt(100) < GHOST_INVISIBILITY_NEAR_CHANCE); // 20% шанс
            }
        }
    }

    public void updateAllGhostEffects(GameSession session, int playerX, int playerY) {
        Random rand = new Random();

        for (Enemy enemy : session.getEnemies()) {
            // Призрак: периодически становится невидимым
            if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
                int distance = Math.max(Math.abs(playerX - enemy.getX()),
                        Math.abs(playerY - enemy.getY()));

                // Если далеко от игрока, высокая вероятность невидимости
                if (distance > enemy.getHostility()) {
                    enemy.setInvisible(rand.nextInt(100) < GHOST_INVISIBILITY_FAR_CHANCE); // 80% шанс
                } else {
                    // Близко к игроку - реже невидимость
                    enemy.setInvisible(rand.nextInt(100) < GHOST_INVISIBILITY_NEAR_CHANCE); // 20% шанс
                }
            }
        }
    }

    public Enemy getEnemyAt(GameSession session, int x, int y) {
        for (Enemy enemy : session.getEnemies()) {
            if (enemy.getX() == x && enemy.getY() == y && enemy.getHealth() > 0) {
                return enemy;
            }
        }
        return null;
    }

    public boolean isWalkable(int x, int y, char[][] map) {
        if (x < 0 || y < 0 || y >= map.length || x >= map[0].length)
            return false;

        char tile = map[y][x];
        return tile == '.'; // ходим только по полу
    }

    public boolean canSeePlayer(int startX, int startY, int targetX, int targetY, char[][] map) {
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);
        int sx = startX < targetX ? 1 : -1;
        int sy = startY < targetY ? 1 : -1;
        int err = dx - dy;

        int x = startX;
        int y = startY;

        while (true) {
            if (x == targetX && y == targetY) return true; // Достигли цели
            if (!isWalkable(x, y, map)) return false;      // Стена на пути

            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
    }
}
