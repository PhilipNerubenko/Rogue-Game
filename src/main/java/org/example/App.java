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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

//    private static void moveEnemies(int playerX, int playerY, char[][] asciiMap) {
//        for (Enemy enemy : enemies) {
//            if (enemy.getHealth() <= 0) continue;
//
//            // Огр отдыхает
//            if (enemy.getRestTurns() > 0) {
//                enemy.setRestTurns(enemy.getRestTurns() - 1);
//                continue;
//            }
//
//            // Вычисляем расстояние до игрока
//            int dx = playerX - enemy.getX();
//            int dy = playerY - enemy.getY();
//            int distance = Math.max(Math.abs(dx), Math.abs(dy)); // Чебышевское расстояние
//
//            // Если игрок в зоне враждебности - преследуем
//            if (distance <= enemy.getHostility()) {
//                moveEnemyTowardsPlayer(enemy, dx, dy, asciiMap);
//            } else {
//                // Иначе случайное движение или спец. паттерн
//                moveEnemyIdle(enemy, asciiMap);
//            }
//
//            // Проверяем, достиг ли враг игрока
//            if (enemy.getX() == playerX && enemy.getY() == playerY) {
//                attackPlayer(enemy);
//                // Огр отдыхает после атаки
//                if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
//                    enemy.setRestTurns(1);
//                }
//            }
//        }
//    }
//
//    private static void moveEnemyTowardsPlayer(Enemy enemy, int dx, int dy, char[][] asciiMap) {
//        int newX = enemy.getX();
//        int newY = enemy.getY();
//
//        // Перемещение по диагонали для змеиного мага
//        if (enemy.hasAbility(Enemy.ABILITY_DIAGONAL_MOVE)) {
//            newX += Integer.signum(dx);
//            newY += Integer.signum(dy);
//        } else {
//            // Обычное движение (по горизонтали или вертикали)
//            if (Math.abs(dx) > Math.abs(dy)) {
//                newX += Integer.signum(dx);
//            } else if (dy != 0) {
//                newY += Integer.signum(dy);
//            }
//        }
//
//        // Огр перемещается на 2 клетки
//        if (enemy.hasAbility(Enemy.ABILITY_OGRE_CHARGE)) {
//            newX += Integer.signum(dx);
//            newY += Integer.signum(dy);
//        }
//
//        // Проверка столкновения с другими врагами
//        if (getEnemyAt(newX, newY) == null) {
//            // Проверка препятствий
//            if (newX >= 0 && newX < asciiMap[0].length &&
//                    newY >= 0 && newY < asciiMap.length &&
//                    asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~' &&
//                    asciiMap[newY][newX] != ' ') {
//
//                // Затираем старую позицию
//                Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
//                        enemy.getX() + 3, enemy.getY(),
//                        new CharColor(CharColor.BLACK, CharColor.WHITE));
//
//                enemy.setX(newX);
//                enemy.setY(newY);
//            }
//        }
//    }
//
//    private static void moveEnemyIdle(Enemy enemy, char[][] asciiMap) {
//        // Призрак телепортируется
//        if (enemy.hasAbility(Enemy.ABILITY_TELEPORT)) {
//            Random rand = new Random();
//            // Случайная позиция в пределах комнаты
//            enemy.setX(enemy.getX() + rand.nextInt(5) - 2);
//            enemy.setY(enemy.getY() + rand.nextInt(5) - 2);
//            return;
//        }
//
//        // Обычное случайное движение
//        Random rand = new Random();
//        int[] dx = {0, 0, -1, 1};
//        int[] dy = {-1, 1, 0, 0};
//        int dir = rand.nextInt(4);
//        int newX = enemy.getX() + dx[dir];
//        int newY = enemy.getY() + dy[dir];
//
//        if (getEnemyAt(newX, newY) == null && newX >= 0 && newX < asciiMap[0].length &&
//                newY >= 0 && newY < asciiMap.length &&
//                asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~' &&
//                asciiMap[newY][newX] != ' ') {
//
//            Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
//                    enemy.getX() + 3, enemy.getY(),
//                    new CharColor(CharColor.BLACK, CharColor.WHITE));
//
//            enemy.setX(newX);
//            enemy.setY(newY);
//        }
//    }

//    private static void attackPlayer(Enemy enemy) {
//        int damage = enemy.getStrength();
//
//        // У вампира есть особая механика
//        if (enemy.hasAbility(Enemy.ABILITY_VAMPIRE_DRAIN)) {
//            // Поглощение максимального HP
//            System.out.println("Вампир высасывает ваше здоровье!");
//        }
//
//        Toolkit.printString("Enemy dealt " + damage + " damage!",
//                3, 27, new CharColor(CharColor.RED, CharColor.BLACK));
//    }

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

}
