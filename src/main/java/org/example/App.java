package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.Item;
import org.example.domain.model.Room;
import org.example.domain.service.EnemyType;
import org.example.domain.service.LevelGenerator;
import sun.misc.Signal;

import java.util.*;

public class App {
    private static final List<Enemy> enemies = new ArrayList<>();
    private static List<Item> items = new ArrayList<>();

    public static void main(String[] args) {
        // Безопасное завершение при Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        System.out.print("\033[?25l");
        Toolkit.init();

        boolean running = true;

        // Цвета
        CharColor bg = new CharColor(CharColor.BLACK, CharColor.BLACK);
        CharColor playerColor = new CharColor(CharColor.BLACK, CharColor.YELLOW);
        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);

        // Базовый игрок
        org.example.domain.entity.Character player =
                new org.example.domain.entity.Character(GameConstants.CharacterBase.HEALTH,
                        GameConstants.CharacterBase.HEALTH, GameConstants.CharacterBase.AGILITY,
                        GameConstants.CharacterBase.STRENGTH, items);

        // Очистка экрана один раз
        Toolkit.clearScreen(bg);

        LevelGenerator levelGenerator = new LevelGenerator();
        char[][] asciiMap = levelGenerator.createAsciiMap(1);
        // Нарисуем карту — MokMap
        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
            String element = new String(asciiMap[i]);
            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
        }

        Room startRoom = levelGenerator.getRooms().getFirst();

        int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
        int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);

        createEnemies(levelGenerator, player);
        updateEnemyEffects(playerX, playerY);
        for (Enemy enemy : enemies) {
            Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                    enemy.getX() + 3, enemy.getY(),
                    new CharColor(CharColor.BLACK, CharColor.WHITE));
        }
        drawEnemies();

        char symbolUnderPlayer = asciiMap[playerY][playerX];

        Toolkit.printString("Use arrows to move, ESC to exit", 3, 29, hintColor);

        while (running) {
            // Рисуем игрока
            Toolkit.printString(String.valueOf(GameConstants.Icons.PLAYER), playerX + 3, playerY, playerColor);

            // Читаем клавишу
            InputChar ch = Toolkit.readCharacter();
            // Пробуем получить символ, игнорируя спец-клавиши
            char character;
            try {
                character = ch.getCharacter();
            } catch (RuntimeException e) {
                // Это стрелка или другая спец-клавиша — просто игнорируем
                continue;
            }

            // Затираем старое положение игрока (возвращаем '.')
            Toolkit.printString(String.valueOf(symbolUnderPlayer), playerX + 3, playerY, new CharColor(CharColor.BLACK, CharColor.WHITE));

            int newX = playerX, newY = playerY;
            // Обработка движения
            // 1. Проверяем ESC по коду (до всего остального)
            if (ch.getCode() == 27) {
                running = false;
                continue;
            }

            // 2. Игнорируем спец-клавиши (стрелки, F1-F12 и т.д.)
            if (character == 0) {
                continue; // Это не буква, игнорируем
            }
            switch (Character.toLowerCase(character)) {
                case GameConstants.control.KEY_W:
                    newY--;
                    break;
                case GameConstants.control.KEY_S:
                    newY++;
                    break;
                case GameConstants.control.KEY_A:
                    newX--;
                    break;
                case GameConstants.control.KEY_D:
                    newX++;
                    break;
                default:
                    continue;
            }

            if (!isValidMove(newX, newY, asciiMap)) continue;

            Enemy enemyAtPosition = getEnemyAt(newX, newY);
            if (enemyAtPosition != null) {
                // Атакуем врага
                attackEnemy(enemyAtPosition, player);
                if (enemyAtPosition.getHealth() <= 0) {
                    removeEnemy(enemyAtPosition, asciiMap);
                }
                continue; // Ход завершен
            } else {
                // Двигаем игрока
                playerX = newX;
                playerY = newY;
                symbolUnderPlayer = asciiMap[playerY][playerX];
            }

//                moveEnemies(playerX, playerY, asciiMap);
            moveEnemiesOriginal(playerX, playerY, asciiMap, levelGenerator.getRooms());
            updateEnemyEffects(playerX, playerY);
                clearEnemyPositions(asciiMap);
                drawEnemies();
        }

        Toolkit.shutdown();
        System.out.println("\nProgram finished normally.");
        System.out.print("\033[?25h");

    }

    private static void clearEnemyPositions(char[][] asciiMap) {
        for (Enemy enemy : enemies) {
            Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                    enemy.getX() + 3, enemy.getY(),
                    new CharColor(CharColor.BLACK, CharColor.WHITE));
        }
    }

    private static Room getRoomByPosition(int x, int y, List<Room> rooms) {
        for (Room room : rooms) {
            if (x >= room.getX1() && x <= room.getX2() &&
                    y >= room.getY1() && y <= room.getY2()) {
                return room;
            }
        }
        return null;
    }

    private static boolean canSeePlayer(int startX, int startY, int targetX, int targetY, char[][] map) {
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

    private static void clearEnemyPosition(Enemy enemy, char[][] map) {
        Toolkit.printString(String.valueOf(map[enemy.getY()][enemy.getX()]),
                enemy.getX() + 3, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
    }

    private static boolean isValidMove(int x, int y, char[][] map) {
        return x >= 0 && x < GameConstants.Map.WIDTH &&
                y >= 0 && y < GameConstants.Map.HEIGHT &&
                map[y][x] != '|' && map[y][x] != '~' && map[y][x] != ' ';
    }

    private static void drawEnemies() {
        for (Enemy enemy : enemies) {
            if (!enemy.isInvisible()) {
                CharColor color = new CharColor(CharColor.BLACK, (short) getEnemyColor(enemy));
                Toolkit.printString(enemy.getType(),
                        enemy.getX() + 3, enemy.getY(), color);
            }
        }
    }

    private static int getEnemyColor(Enemy enemy) {
        return switch (enemy.getType()) {
            case "z" -> CharColor.GREEN;
            case "v" -> CharColor.RED;
            //case "g" -> CharColor.WHITE;
            case "O" -> CharColor.YELLOW;
            case "s" -> CharColor.CYAN;
            default -> CharColor.WHITE;
        };
    }

    private static Enemy getEnemyAt(int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.getX() == x && enemy.getY() == y && enemy.getHealth() > 0) {
                return enemy;
            }
        }
        return null;
    }

    private static boolean isHit(int attackerAgility, int defenderAgility) {
        int baseChance = 50;
        int agilityDelta = attackerAgility - defenderAgility;
        int finalChance = Math.max(10, Math.min(90, baseChance + agilityDelta * 5));
        Random rand = new Random();
        return rand.nextInt(100) < finalChance;
    }

    private static void attackEnemy(Enemy enemy, org.example.domain.entity.Character player) {
        if (!isHit(player.getAgility(), enemy.getAgility())) {
            Toolkit.printString("You missed!                ", 3, 28,
                    new CharColor(CharColor.YELLOW, CharColor.BLACK));
            return;
        }

        // Способность: первый удар промах
        if (enemy.hasAbility(Enemy.ABILITY_FIRST_MISS)) {
            Toolkit.printString("First attack on vampire misses!                ", 3, 28,
                    new CharColor(CharColor.YELLOW, CharColor.BLACK));
            enemy.removeAbility(Enemy.ABILITY_FIRST_MISS);
            return;
        }

        int damage = Math.max(1, player.getStrength());
        enemy.setHealth(enemy.getHealth() - damage);

        String msg = "You dealt " + damage + " dmg to " + enemy.getType();
        if (enemy.getHealth() <= 0) msg += " - KILLED!";
        Toolkit.printString(msg + "                ", 3, 28,
                new CharColor(CharColor.YELLOW, CharColor.BLACK));
    }

    private static void removeEnemy(Enemy enemy, char[][] asciiMap) {
        // Затираем врага символом пола
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + 3, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        enemies.remove(enemy);
    }

    private static void updateEnemyEffects(int playerX, int playerY) {
        for (Enemy enemy : enemies) {
            // Призрак становится невидимым, если игрок далеко
            if (enemy.hasAbility(Enemy.ABILITY_INVISIBLE)) {
                int distance = Math.max(Math.abs(playerX - enemy.getX()),
                        Math.abs(playerY - enemy.getY()));
                enemy.setInvisible(distance > enemy.getHostility());
            }
        }
    }

    private static void createEnemies(LevelGenerator levelGenerator, org.example.domain.entity.Character player) {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();

        // Случайная плотность: 40-60% комнат с врагами
        int roomsWithEnemies = (int)(rooms.size() * (0.4 + rand.nextDouble() * 0.2));

        // Перемешиваем комнаты, чтобы выбрать случайные
        List<Room> shuffledRooms = new ArrayList<>(rooms);
        Collections.shuffle(shuffledRooms, rand);

        for (int i = 0; i < roomsWithEnemies; i++) {
            Room room = shuffledRooms.get(i);

            // Пропускаем стартовую
            if (room.isStartRoom()) {
                continue;
            }

            // Случайная позиция внутри комнаты
            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);

            // Случайный тип врага (а не по порядку)
            EnemyType randomType = EnemyType.values()[
                    rand.nextInt(EnemyType.values().length)
                    ];
// TODO: Продумать уровень врага в зависимости от глубины
//            // Уровень врага зависит от глубины (roguelike прогрессия)
//            int enemyLevel = 1 + rand.nextInt(levelDepth); // levelDepth — глобальная переменная

            Enemy enemy = randomType.create(1);
            enemy.setX(enemyX);
            enemy.setY(enemyY);
            enemies.add(enemy);
        }
    }

    private static void moveEnemiesOriginal(int playerX, int playerY, char[][] asciiMap, List<Room> rooms) {
        for (Enemy enemy : enemies) {
            if (enemy.getHealth() <= 0) continue;

            int dx = playerX - enemy.getX();
            int dy = playerY - enemy.getY();
            int dist = Math.max(Math.abs(dx), Math.abs(dy));

            // Враг преследует только если игрок видим и в зоне враждебности
            if (dist <= enemy.getHostility() && canSeePlayer(enemy.getX(), enemy.getY(), playerX, playerY, asciiMap)) {
                moveEnemyChase(enemy, playerX, playerY, asciiMap);
            } else {
                moveEnemyWander(enemy, asciiMap);
            }
        }
    }

    private static void moveEnemyWander(Enemy enemy, char[][] asciiMap) {
        String type = enemy.getType();

        switch (type) {
            case "z": moveZombie(enemy, asciiMap); break;
            case "v": moveVampire(enemy, asciiMap); break;
            case "g": moveGhost(enemy, asciiMap); break;
            case "O": moveOgre(enemy, asciiMap); break;
            case "s": moveSnakeMage(enemy, asciiMap); break;
        }
    }


    private static boolean isWalkable(int x, int y, char[][] map) {
        if (x < 0 || y < 0 || y >= map.length || x >= map[0].length)
            return false;

        char tile = map[y][x];
        return tile == '.'; // ходим только по полу
    }


    private static void moveZombie(Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        int dir = rand.nextInt(4);
        int nx = enemy.getX() + dx[dir];
        int ny = enemy.getY() + dy[dir];

        if (!isWalkable(nx, ny, asciiMap)) return;
        if (getEnemyAt(nx, ny) != null) return;

        clearEnemyPosition(enemy,asciiMap);

        enemy.setX(nx);
        enemy.setY(ny);
    }

    private static void moveVampire(Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        // 8 направлений
        int[] dx = {-1,-1,-1, 0,0, 1,1,1};
        int[] dy = {-1, 0, 1,-1,1,-1,0,1};

        int dir = rand.nextInt(8);
        int nx = enemy.getX() + dx[dir];
        int ny = enemy.getY() + dy[dir];

        if (!isWalkable(nx, ny, asciiMap)) return;
        if (getEnemyAt(nx, ny) != null) return;

        clearEnemyPosition(enemy,asciiMap);

        enemy.setX(nx);
        enemy.setY(ny);
    }

    private static void moveGhost(Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        for (int i = 0; i < 20; i++) { // 20 попыток найти свободную клетку
            int nx = enemy.getX() + rand.nextInt(7) - 3; // -3..3
            int ny = enemy.getY() + rand.nextInt(7) - 3;

            if (!isWalkable(nx, ny, asciiMap)) continue;
            if (getEnemyAt(nx, ny) != null) continue;

            clearEnemyPosition(enemy,asciiMap);

            enemy.setX(nx);
            enemy.setY(ny);
            return;
        }
    }

    private static void moveOgre(Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        int dir = rand.nextInt(4);

        // делает "длинный шаг"
        int nx = enemy.getX() + dx[dir] * 2;
        int ny = enemy.getY() + dy[dir] * 2;

        if (!isWalkable(nx, ny, asciiMap)) return;
        if (getEnemyAt(nx, ny) != null) return;

        clearEnemyPosition(enemy,asciiMap);

        enemy.setX(nx);
        enemy.setY(ny);
    }

    private static void moveSnakeMage(Enemy enemy, char[][] asciiMap) {
        Random rand = new Random();

        // Каждый ход выбираем случайную диагональ (-1 или 1 по X и Y)
        int[] choices = {-1, 1};
        enemy.setDiagX(choices[rand.nextInt(2)]);
        enemy.setDiagY(choices[rand.nextInt(2)]);

        int nx = enemy.getX() + enemy.getDiagX();
        int ny = enemy.getY() + enemy.getDiagY();

        // Проверка на проходимость
        if (!isWalkable(nx, ny, asciiMap) || getEnemyAt(nx, ny) != null) {
            // Если диагональ невозможна, пробуем противоположную
            nx = enemy.getX() - enemy.getDiagX();
            ny = enemy.getY() - enemy.getDiagY();
            if (!isWalkable(nx, ny, asciiMap) || getEnemyAt(nx, ny) != null) {
                // Если и так нельзя — стоим на месте
                return;
            }
        }

        clearEnemyPosition(enemy,asciiMap);

        enemy.setX(nx);
        enemy.setY(ny);
    }

    private static void moveEnemyChase(Enemy enemy, int playerX, int playerY, char[][] asciiMap) {
        // Ищем путь, но не дальше, чем радиус враждебности монстра
        List<int[]> path = findPath(enemy.getX(), enemy.getY(), playerX, playerY, asciiMap, enemy.getHostility());
        if (path != null && path.size() > 1) {
            // Первый шаг на пути к игроку
            int[] step = path.get(1);
            Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                    enemy.getX() + 3, enemy.getY(),
                    new CharColor(CharColor.BLACK, CharColor.WHITE));
            enemy.setX(step[0]);
            enemy.setY(step[1]);
        } else {
            moveEnemyWander(enemy, asciiMap);
            }
        }

    private static List<int[]> findPath(int sx, int sy, int ex, int ey, char[][] asciiMap, int maxSteps) {
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
                if (tile == '|' || tile == '~' || tile == ' ') continue;
                if (App.getEnemyAt(nx, ny) != null && !(nx == ex && ny == ey)) continue;

                visited[ny][nx] = true;
                prev[ny][nx][0] = x;
                prev[ny][nx][1] = y;
                queue.add(new int[]{nx, ny});
            }
        }

        return null;
    }
}
