package org.rogue.presentation;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import sun.misc.Signal;

public class Main {
    public static void main(String[] args) {
        // Безопасное завершение при Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        Toolkit.init();

        int width = 40;
        int height = 20;

        // Координаты игрока
        int playerX = width / 2;
        int playerY = height / 2;

        boolean running = true;

        // Цвета
        CharColor bg = new CharColor(CharColor.BLACK, CharColor.BLACK);
        CharColor playerColor = new CharColor(CharColor.YELLOW, CharColor.BLACK);
        CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);

        // Очистка экрана один раз
        Toolkit.clearScreen(bg);

        // Нарисуем карту — просто фон с точками
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Toolkit.printString(".", x, y, new CharColor(CharColor.WHITE, CharColor.BLACK));
            }
        }

        Toolkit.printString("Use arrows to move, ESC to exit", 2, height - 1, hintColor);

        while (running) {
            // Рисуем игрока
            Toolkit.printString("@", playerX, playerY, playerColor);

            // Читаем клавишу
            InputChar ch = Toolkit.readCharacter();
            int code = ch.getCode();

            // Затираем старое положение игрока (возвращаем '.')
            Toolkit.printString(".", playerX, playerY, new CharColor(CharColor.WHITE, CharColor.BLACK));

            // Обработка движения
            if (code == InputChar.KEY_UP && playerY > 0) playerY--;
            else if (code == InputChar.KEY_DOWN && playerY < height - 2) playerY++;
            else if (code == InputChar.KEY_LEFT && playerX > 0) playerX--;
            else if (code == InputChar.KEY_RIGHT && playerX < width - 1) playerX++;
            else if (code == 27) running = false; // ESC
        }

        Toolkit.shutdown();
        System.out.println("\nProgram finished normally.");
    }
}
