package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.presentation.GameLoop;
import org.example.presentation.InputHandler;
import org.example.presentation.JCursesRenderer;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class App {

    public static void main(String[] args) {
        // Обработка Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            System.out.println("\nЗавершение через Ctrl+C");
            System.exit(0);
        });

        System.out.print("\033[?25l"); // Скрыть курсор

        try {
            GameInitializer initializer = new GameInitializer();
            initializer.initialize();

            GameLoop gameLoop = new GameLoop(initializer);
           gameLoop.start();

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.print("\033[?25h"); // Показать курсор
        }
    }
















//    private static GameSession session;
//    //    private static List<Enemy> enemies = new ArrayList<>();
//    private static EnemyAIService enemyAIService;





//    public static void main(String[] args) {
//        session = new GameSession();
//        session.setEnemies(new ArrayList<>());
//        CombatService combatService = new CombatService();
//
//
//        // 2. Создать сервисы
//       // CombatService combatService = new CombatService();
//        enemyAIService = new EnemyAIService();
//        MovementService movementService = new MovementService();
//        InputHandler inputHandler = new InputHandler();
//        JCursesRenderer renderer = new JCursesRenderer();
//
//
//        // Безопасное завершение при Ctrl+C
//        Signal.handle(new Signal("INT"), signal -> {
//            Toolkit.shutdown();
//            System.out.println("\nTerminated via Ctrl+C");
//            System.exit(0);
//        });
//
//        System.out.print("\033[?25l");
//        Toolkit.init();
//
//        int width = GameConstants.Map.WIDTH;
//        int height = GameConstants.Map.HEIGHT;
//
//        // Координаты игрока
////        int playerX = width / 2 + 2;
////        int playerY = height / 2;
//
//        boolean running = true;
//
//        // Цвета
//        CharColor bg = new CharColor(CharColor.BLACK, CharColor.BLACK);
//        CharColor playerColor = new CharColor(CharColor.BLACK, CharColor.YELLOW);
//        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);
//
//        // Очистка экрана один раз
//        Toolkit.clearScreen(bg);
//
//
//        LevelGenerator levelGenerator = new LevelGenerator();
//        char[][] asciiMap = levelGenerator.createAsciiMap(1) ;
//        // Нарисуем карту — MokMap
//        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
//            String element = new String(asciiMap[i]);
//            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
//        }
//
//        Room startRoom = levelGenerator.getRooms().getFirst();
//
//        int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
//        int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);
//
//        createEnemies(levelGenerator, playerX, playerY);
//
//        drawEnemies();
//
//        char symbolUnderPlayer = asciiMap[playerY][playerX];
//
//        Toolkit.printString("Use arrows to move, ESC to exit", 3, 29, hintColor);
//
//        while (running) {
//            // Рисуем игрока
//            Toolkit.printString(new String(new char[]{GameConstants.Icons.PLAYER}), playerX + 3, playerY, playerColor);
//
//            // Читаем клавишу
//            InputChar ch = Toolkit.readCharacter();
//            //int code = ch.getCode();
//            // Пробуем получить символ, игнорируя спец-клавиши
//            char character;
//            try {
//                character = ch.getCharacter();
//            } catch (RuntimeException e) {
//                // Это стрелка или другая спец-клавиша — просто игнорируем
//                continue;
//            }
//
//            // Затираем старое положение игрока (возвращаем '.')
//            Toolkit.printString(new String(String.valueOf(symbolUnderPlayer)), playerX + 3, playerY, new CharColor(CharColor.BLACK, CharColor.WHITE));
//
//            int newX = playerX, newY = playerY;
//            // Обработка движения
//            // 1. Проверяем ESC по коду (до всего остального)
//            if (ch.getCode() == 27) {
//                running = false;
//                continue;
//            }
//
//            // 2. Игнорируем спец-клавиши (стрелки, F1-F12 и т.д.)
//            if (character == 0) {
//                continue; // Это не буква, игнорируем
//            }
//            switch (Character.toLowerCase(character)) {
//                case GameConstants.control.KEY_W:
//                    newY--;
//                    break;
//                case GameConstants.control.KEY_S:
//                    newY++;
//                    break;
//                case GameConstants.control.KEY_A:
//                    newX--;
//                    break;
//                case GameConstants.control.KEY_D:
//                    newX++;
//                    break;
//                default: continue;
//            }
//
//            Enemy enemyAtPosition = enemyAIService.getEnemyAt(session, newX, newY);
//            if (enemyAtPosition != null) {
//                // Атакуем врага
//
//                combatService.attackEnemy(session, enemyAtPosition);
//                if (enemyAtPosition.getHealth() <= 0) {
//                    //removeEnemy(enemyAtPosition, asciiMap);
//                    combatService.removeEnemy(session, enemyAtPosition, asciiMap);
//                }
//                continue; // Ход завершен
//            }
//
//
//            if (newX >= 0 && newX < GameConstants.Map.WIDTH &&
//                    newY >= 0 && newY < GameConstants.Map.HEIGHT &&
//                    asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~'
//            && asciiMap[newY][newX] != ' ') {
//
//                playerX = newX;
//                playerY = newY;
//                symbolUnderPlayer = asciiMap[playerY][playerX];
//
//                enemyAIService.moveEnemies(session, playerX, playerY, asciiMap);
//                enemyAIService.updateEnemyEffects(session, playerX, playerY);
//                drawEnemies();
//            }
//        }
//
//        Toolkit.shutdown();
//        System.out.println("\nProgram finished normally.");
//        System.out.print("\033[?25h");
//
////        System.out.println("Таблица ASCII:");
////        for (int i = 0; i <= 127; i++) {
////            // Преобразуем число в символ
////            char asciiChar = (char) i;
////            // Выводим код и символ
////            System.out.printf("Код: %d, Символ: %s%n", i, asciiChar);
////        }
//
//
//    }
//
//    private static void drawEnemies() {
//        for (Enemy enemy : session.getEnemies()) {
//            if (!enemy.isInvisible()) {
//                CharColor color = new CharColor(CharColor.BLACK, (short) getEnemyColor(enemy));
//                Toolkit.printString(enemy.getType(),
//                        enemy.getX() + 3, enemy.getY(), color);
//            }
//        }
//    }
//
//    private static int getEnemyColor(Enemy enemy) {
//        return switch (enemy.getType()) {
//            case "z" -> CharColor.GREEN;
//            case "v" -> CharColor.RED;
//            case "g" -> CharColor.WHITE;
//            case "O" -> CharColor.YELLOW;
//            case "s" -> CharColor.CYAN;
//            default -> CharColor.WHITE;
//        };
//    }
//
//
//
//
//
////    private static void removeEnemy(Enemy enemy, char[][] asciiMap) {
////        // Затираем врага символом пола
////        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
////                enemy.getX() + 3, enemy.getY(),
////                new CharColor(CharColor.BLACK, CharColor.WHITE));
////        session.getEnemies().remove(enemy);
////    }
//
//
//
//
//
//
//
//
//
//    private static void createEnemies(LevelGenerator levelGenerator, int playerX, int playerY) {
//        List<Room> rooms = levelGenerator.getRooms();
//        Random rand = levelGenerator.getRand();
//        int playerAgility = 5; // Базовая ловкость игрока
//
//        for (int i = 0; i < EnemyType.values().length && i < rooms.size(); i++) {
//            Room room = rooms.get(i);
//
//            // Не размещаем врага в комнате игрока
//            if (room.isStartRoom()) {
//                continue;
//            }
//
//            // Случайная позиция внутри комнаты
//            int enemyX = room.getX1() + 1 + rand.nextInt(room.getWidth() - 2);
//            int enemyY = room.getY1() + 1 + rand.nextInt(room.getHeight() - 2);
//
//            // Создаем врага на уровне 1
//            Enemy enemy = EnemyType.values()[i].create(1, playerAgility);
//
//            // Устанавливаем позицию (нужно добавить в класс Enemy поля x, y)
//            enemy.setX(enemyX);
//            enemy.setY(enemyY);
//
//            session.getEnemies().add(enemy);
//        }
//    }

}

//
//char[][] asciiMap = new LevelGenerator().createAsciiMap(1) ;
//// Нарисуем карту — MokMap
//        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
//String element = new String(asciiMap[i]);
//            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
//        }

