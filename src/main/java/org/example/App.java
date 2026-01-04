package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.datalayer.SessionStat;
import org.example.presentation.GameLoop;
import org.example.presentation.JCursesRenderer;
import sun.misc.Signal;

import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class App {

    public static void main(String[] args) {
        init_presentation();

        SessionStat sessionStat = new SessionStat(
                0,  // treasures
                1,  // level
                0,  // enemies
                0,  // food
                0,  // elixirs
                0,  // scrolls
                0,  // attacks
                0,  // missed
                0   // moves
        );

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
                                initializer.initializeNewGame(sessionStat);
                                GameLoop gameLoop = new GameLoop(initializer);
                                gameLoop.start(sessionStat);
                                break;
                            case 1: // LOAD GAME
                                handleLoadGame(renderer, sessionStat);
                                break;
                            case 2: // SCOREBOARD
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

    private static void handleLoadGame(JCursesRenderer renderer, SessionStat sessionStat) {
        try {
            GameInitializer initializer = new GameInitializer();
            GameLoop gameLoop = new GameLoop(initializer);

            // Получаем InputHandler и проверяем наличие сохранений
            org.example.presentation.InputHandler inputHandler = initializer.getInputHandler();

            if (!inputHandler.hasSavedGames()) {
                renderer.clearScreen();
                renderer.drawString(10, 10, "No saved games found!", CharColor.RED);
                renderer.drawString(10, 12, "Press any key to continue...", CharColor.YELLOW);
                renderer.refresh();
                Toolkit.readCharacter();
                return;
            }

            // Создаем новую статистику (она будет перезаписана при загрузке)
            SessionStat loadedSessionStat = new SessionStat();

            // Загружаем игру
            boolean loaded = inputHandler.loadSavedGame(
                    initializer.getSession(),
                    loadedSessionStat,
                    initializer.getLevelGenerator()
            );

            if (loaded) {
                // Устанавливаем сессию и статистику в InputHandler
                inputHandler.setGameSession(initializer.getSession());
                inputHandler.setSessionStat(loadedSessionStat);

                // Запускаем игру с загруженной статистикой
                gameLoop.start(loadedSessionStat);
            } else {
                renderer.clearScreen();
                renderer.drawString(10, 10, "Failed to load game!", CharColor.RED);
                renderer.drawString(10, 12, "Press any key to continue...", CharColor.YELLOW);
                renderer.refresh();
                Toolkit.readCharacter();
            }

        } catch (Exception e) {
            System.err.println("Error loading game: " + e.getMessage());
            e.printStackTrace();
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