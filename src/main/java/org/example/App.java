package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.model.Room;
import org.example.domain.service.EnemyType;
import org.example.domain.service.LevelGenerator;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class App {
    private static List<Enemy> enemies = new ArrayList<>();
    private static Enemy currentCombatEnemy = null;
    public static void main(String[] args) {
        // Безопасное завершение при Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        System.out.print("\033[?25l");
        Toolkit.init();

        int width = GameConstants.Map.WIDTH;
        int height = GameConstants.Map.HEIGHT;

        // Координаты игрока
//        int playerX = width / 2 + 2;
//        int playerY = height / 2;

        boolean running = true;

        // Цвета
        CharColor bg = new CharColor(CharColor.BLACK, CharColor.BLACK);
        CharColor playerColor = new CharColor(CharColor.BLACK, CharColor.YELLOW);
        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);

        // Очистка экрана один раз
        Toolkit.clearScreen(bg);

//        // Нарисуем карту — просто фон с точками
//        for (int y = 1; y < height+1; y++) {
//            for (int x = 3; x < width+3; x++) {
//                Toolkit.printString(new String( new char[]{GameConstants.Icons.FLOOR}), x, y, new CharColor(CharColor.BLACK, CharColor.WHITE));
//            }
//        }

//        // Нарисуем карту — MokMap
//        for (int i = 0; i < GameConstants.MokMap.myArray.length; i++) {
//            String element = GameConstants.MokMap.myArray[i];
//            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
//        }

        LevelGenerator levelGenerator = new LevelGenerator();
        char[][] asciiMap = levelGenerator.createAsciiMap(1) ;
        // Нарисуем карту — MokMap
        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
            String element = new String(asciiMap[i]);
            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
        }

        Room startRoom = levelGenerator.getRooms().getFirst();

        int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
        int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);

        createEnemies(levelGenerator, playerX, playerY);

        drawEnemies();

        char symbolUnderPlayer = asciiMap[playerY][playerX];

        Toolkit.printString("Use arrows to move, ESC to exit", 3, 29, hintColor);

        while (running) {
            // Рисуем игрока
            Toolkit.printString(new String(new char[]{GameConstants.Icons.PLAYER}), playerX + 3, playerY, playerColor);

            // Читаем клавишу
            InputChar ch = Toolkit.readCharacter();
            //int code = ch.getCode();
            // Пробуем получить символ, игнорируя спец-клавиши
            char character;
            try {
                character = ch.getCharacter();
            } catch (RuntimeException e) {
                // Это стрелка или другая спец-клавиша — просто игнорируем
                continue;
            }

            // Затираем старое положение игрока (возвращаем '.')
            Toolkit.printString(new String(String.valueOf(symbolUnderPlayer)), playerX + 3, playerY, new CharColor(CharColor.BLACK, CharColor.WHITE));

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
                default: continue;
            }

            Enemy enemyAtPosition = getEnemyAt(newX, newY);
            if (enemyAtPosition != null) {
                // Атакуем врага
                attackEnemy(enemyAtPosition);
                if (enemyAtPosition.getHealth() <= 0) {
                    removeEnemy(enemyAtPosition, asciiMap);
                }
                continue; // Ход завершен
            }


            if (newX >= 0 && newX < GameConstants.Map.WIDTH &&
                    newY >= 0 && newY < GameConstants.Map.HEIGHT &&
                    asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~'
            && asciiMap[newY][newX] != ' ') {

                playerX = newX;
                playerY = newY;
                symbolUnderPlayer = asciiMap[playerY][playerX];

                moveEnemies(playerX, playerY, asciiMap);
                updateEnemyEffects(playerX, playerY);
                drawEnemies();
            }
        }

        Toolkit.shutdown();
        System.out.println("\nProgram finished normally.");
        System.out.print("\033[?25h");

//        System.out.println("Таблица ASCII:");
//        for (int i = 0; i <= 127; i++) {
//            // Преобразуем число в символ
//            char asciiChar = (char) i;
//            // Выводим код и символ
//            System.out.printf("Код: %d, Символ: %s%n", i, asciiChar);
//        }


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
            case "g" -> CharColor.WHITE;
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

    private static void attackEnemy(Enemy enemy) {
        // Простой расчет урона (сила игрока - ловкость врага)
        int playerDamage = 10; // Базовая сила игрока
        int actualDamage = Math.max(1, playerDamage - enemy.getAgility());

        enemy.setHealth(enemy.getHealth() - actualDamage);

        // Сообщение об атаке
        Toolkit.printString("Вы нанесли " + actualDamage + " урона " + enemy.getType() + "!",
                3, 28, new CharColor(CharColor.YELLOW, CharColor.BLACK));
    }

    private static void removeEnemy(Enemy enemy, char[][] asciiMap) {
        // Затираем врага символом пола
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + 3, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        enemies.remove(enemy);
    }

    private static void moveEnemies(int playerX, int playerY, char[][] asciiMap) {
        for (Enemy enemy : enemies) {
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
                moveEnemyTowardsPlayer(enemy, dx, dy, asciiMap);
            } else {
                // Иначе случайное движение или спец. паттерн
                moveEnemyIdle(enemy, asciiMap);
            }

            // Проверяем, достиг ли враг игрока
            if (enemy.getX() == playerX && enemy.getY() == playerY) {
                attackPlayer(enemy);
                // Огр отдыхает после атаки
                if (enemy.hasAbility(Enemy.ABILITY_OGRE_REST)) {
                    enemy.setRestTurns(1);
                }
            }
        }
    }

    private static void moveEnemyTowardsPlayer(Enemy enemy, int dx, int dy, char[][] asciiMap) {
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
        if (getEnemyAt(newX, newY) == null) {
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

    private static void moveEnemyIdle(Enemy enemy, char[][] asciiMap) {
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

        if (getEnemyAt(newX, newY) == null && newX >= 0 && newX < asciiMap[0].length &&
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

    private static void attackPlayer(Enemy enemy) {
        int damage = enemy.getStrength();

        // У вампира есть особая механика
        if (enemy.hasAbility(Enemy.ABILITY_VAMPIRE_DRAIN)) {
            // Поглощение максимального HP
            System.out.println("Вампир высасывает ваше здоровье!");
        }

        Toolkit.printString("Враг нанес " + damage + " урона!",
                3, 27, new CharColor(CharColor.RED, CharColor.BLACK));
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

    private static void createEnemies(LevelGenerator levelGenerator, int playerX, int playerY) {
        List<Room> rooms = levelGenerator.getRooms();
        Random rand = levelGenerator.getRand();
        int playerAgility = 5; // Базовая ловкость игрока

        for (int i = 0; i < EnemyType.values().length && i < rooms.size(); i++) {
            Room room = rooms.get(i);

            // Не размещаем врага в комнате игрока
            if (room.isStartRoom()) {
                continue;
            }

            // Случайная позиция внутри комнаты
            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);

            // Создаем врага на уровне 1
            Enemy enemy = EnemyType.values()[i].create(1, playerAgility);

            // Устанавливаем позицию (нужно добавить в класс Enemy поля x, y)
            enemy.setX(enemyX);
            enemy.setY(enemyY);

            enemies.add(enemy);
        }
    }
        
}

//
//char[][] asciiMap = new LevelGenerator().createAsciiMap(1) ;
//// Нарисуем карту — MokMap
//        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
//String element = new String(asciiMap[i]);
//            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
//        }

