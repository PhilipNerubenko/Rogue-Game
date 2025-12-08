package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.presentation.GameLoop;
import sun.misc.Signal;

import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class App {

    public static void main(String[] args) {
        // Обработка Ctrl+C
        Signal.handle(new Signal(SIGINT_STRING), signal -> {
            Toolkit.shutdown();
            System.out.println(TERMINATE);
            System.exit(0);
        });

        System.out.print(HIDE_CURSOR);

        try {
            Toolkit.init();

            // Создание и инициализация игры
            GameInitializer initializer = new GameInitializer();
            initializer.initialize();

            // Запуск игрового цикла
            GameLoop gameLoop = new GameLoop(initializer);
            gameLoop.start();

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Toolkit.shutdown();
            System.out.print(SHOW_CURSOR);
        }
    }

    public static void printLine(int y, String text, CharColor color, int maxLength) {
        // Очистка строки пробелами
        Toolkit.printString(" ".repeat(maxLength), MAP_OFFSET_X, y, color);
        // Печать нового текста
        Toolkit.printString(text, MAP_OFFSET_X, y, color);
    }
}