package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.model.Room;
import org.example.domain.service.LevelGenerator;
import sun.misc.Signal;

public class App {
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
            // TODO WASD
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

            if (newX >= 0 && newX < GameConstants.Map.WIDTH &&
                    newY >= 0 && newY < GameConstants.Map.HEIGHT &&
                    asciiMap[newY][newX] != '|' && asciiMap[newY][newX] != '~'
            && asciiMap[newY][newX] != ' ') {

                playerX = newX;
                playerY = newY;
                symbolUnderPlayer = asciiMap[playerY][playerX];
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
}

//
//char[][] asciiMap = new LevelGenerator().createAsciiMap(1) ;
//// Нарисуем карту — MokMap
//        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
//String element = new String(asciiMap[i]);
//            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
//        }