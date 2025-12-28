package org.example;

import jcurses.system.Toolkit;
import org.example.presentation.GameLoop;
import org.example.presentation.JCursesRenderer;
import sun.misc.Signal;

import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class App {

    public static void main(String[] args) {
        init_presentation();

        JCursesRenderer renderer = null;
        try {
            renderer = new JCursesRenderer();

            boolean runningMenu = true;
            int currentOption = 0;

            while (runningMenu) {
                renderer.drawMenuScreen(currentOption);

                char key = Toolkit.readCharacter().getCharacter();

                switch (key) {
                    case '\n':  // Enter
                        switch (currentOption) {
                            case 0: // NEW GAME
                                GameInitializer initializer = new GameInitializer();
                                initializer.initializeNewGame();
                                GameLoop gameLoop = new GameLoop(initializer);
                                gameLoop.start();  // Блокирует до выхода из игры
                                break;
                            case 1: // LOAD GAME
                                // TODO: реализовать загрузку игры
                                break;
                            case 2: // SCOREBOARD
                                // TODO: реализовать показ таблицы лидеров

                                renderer.drawScoreboard();

                                while (Toolkit.readCharacter().getCharacter() != 27);
                                break;
                            case 3: // EXIT GAME
                                runningMenu = false;
                                break;
                        }
                        break;

                    case 'W':
                    case 'w':
                        currentOption = Math.max(0, currentOption - 1);
                        break;

                    case 'S':
                    case 's':
                        currentOption = Math.min(3, currentOption + 1);
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (renderer != null) {
                renderer.shutdown();
            } else {
                Toolkit.shutdown();
            }
            System.out.print(SHOW_CURSOR);
        }
    }

    private static void init_presentation() {
        // Обработка Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println(TERMINATE);
            System.exit(0);
        });

        System.out.print(HIDE_CURSOR);
        System.out.print("\033[8;40;120t");
        System.out.flush();
    }
}