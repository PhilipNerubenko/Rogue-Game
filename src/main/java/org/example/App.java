package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
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
        int playerX = width / 2 + 2;
        int playerY = height / 2;

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

        char[][] asciiMap = new  LevelGenerator().createAsciiMap(1) ;
        // Нарисуем карту — MokMap
        for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
            String element = new String(asciiMap[i]);
            Toolkit.printString(element, 3, i, new CharColor(CharColor.BLACK, CharColor.WHITE));
        }



        Toolkit.printString("Use arrows to move, ESC to exit", 3, 29, hintColor);

        while (running) {
            // Рисуем игрока
            Toolkit.printString(new String(new char[]{GameConstants.Icons.PLAYER}), playerX, playerY, playerColor);

            // Читаем клавишу
            InputChar ch = Toolkit.readCharacter();
            int code = ch.getCode();

            // Затираем старое положение игрока (возвращаем '.')
            Toolkit.printString(new String(new char[]{GameConstants.Icons.FLOOR}), playerX, playerY, new CharColor(CharColor.BLACK, CharColor.WHITE));

            // Обработка движения
            // TODO WASD
            if (code == InputChar.KEY_UP && playerY > 0) playerY--;
            else if (code == InputChar.KEY_DOWN && playerY < height - 2) playerY++;
            else if (code == InputChar.KEY_LEFT && playerX > 0) playerX--;
            else if (code == InputChar.KEY_RIGHT && playerX < width - 1) playerX++;
            else if (code == 27) running = false; // ESC
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