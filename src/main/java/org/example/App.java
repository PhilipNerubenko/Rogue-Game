package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.domain.model.Level;
import org.example.presentation.GameLoop;
import org.example.presentation.GameMenu;
import org.example.presentation.GameMenu.GameStats;
import sun.misc.Signal;

public class App {

    private static boolean appRunning = true;
    private static GameStats lastGameStats = null;

    public static void main(String[] args) {
        // Обработка Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            appRunning = false;
            Toolkit.shutdown();
            System.out.print("\033[8;45;130t");
            System.out.flush();
            System.out.println("\nUse WASD to move, ESC to exit");
            System.exit(0);
        });

        System.out.print("\033[?25l"); // Скрыть курсор

        try {
            Toolkit.init();

            while (appRunning) {
                // Устанавливаем последнюю статистику для Game Over экрана
                if (lastGameStats != null) {
                    GameMenu.setLastGameStats(lastGameStats);
                }

                // Показываем меню (Game Over если была смерть/победа)
                boolean showGameOver = (lastGameStats != null);
                GameMenu menu = new GameMenu(showGameOver);
                GameMenu.MenuAction action = menu.showAndGetAction();

                // Сбрасываем статистику после показа Game Over
                if (showGameOver) {
                    lastGameStats = null;
                    GameMenu.setLastGameStats(null);
                }

                switch (action) {
                    case NEW_GAME:
                        startNewGame();
                        break;
                    case LOAD_GAME:
                        loadGame();
                        break;
                    case SCOREBOARD:
                        showScoreboard();
                        break;
                    case EXIT_GAME:
                        appRunning = false;
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("Critical error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Toolkit.shutdown();
            System.out.print("\033[?25h"); // Показать курсор
            System.out.println("\nGame over.");
        }
    }

    private static void startNewGame() {
        try {
            int levelNumber = 1;
            GameInitializer initializer = new GameInitializer(levelNumber);
            initializer.initialize();

            GameLoop gameLoop = new GameLoop(initializer);

            // Запускаем игру и получаем результат
            GameResult result = gameLoop.start();

            // Сохраняем статистику для Game Over экрана
            if (result != null) {
                lastGameStats = new GameStats(
                        result.getLevel(),
                        result.getTreasures(),
                        result.isVictory()
                );

                // Сохраняем в таблицу рекордов (по заданию 5)
                saveToScoreboard(result);
            }

        } catch (Exception e) {
            System.err.println("Error starting the game: " + e.getMessage());
            showErrorMessage("Error starting the game: " + e.getMessage());
        }
    }

    private static void saveToScoreboard(GameResult result) {
        // TODO: Реализовать сохранение в таблицу рекордов (Задание 5)
        System.out.println("Saving the result: Level " + result.getLevel() +
                ", Treasures: " + result.getTreasures() +
                ", Victory: " + result.isVictory());
    }

    private static void loadGame() {
        showMessage("Функция загрузки игры в разработке");
    }

    private static void showScoreboard() {
        showMessage("Таблица рекордов в разработке");
    }

    private static void showMessage(String message) {
        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));
        Toolkit.printString(message, 40, 10, new CharColor(CharColor.YELLOW, CharColor.BLACK));
        Toolkit.printString("Нажмите любую клавишу...", 35, 12,
                new CharColor(CharColor.GREEN, CharColor.BLACK));
        Toolkit.readCharacter();
    }

    private static void showErrorMessage(String message) {
        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));
        Toolkit.printString("ERROR:", 40, 10, new CharColor(CharColor.RED, CharColor.BLACK));
        Toolkit.printString(message, 40, 12, new CharColor(CharColor.RED, CharColor.BLACK));
        Toolkit.printString("Press any key...", 35, 14,
                new CharColor(CharColor.GREEN, CharColor.BLACK));
        Toolkit.readCharacter();
    }

    // Класс для хранения результата игры
    public static class GameResult {
        private final int level;
        private final int treasures;
        private final boolean victory;
        private final boolean escaped; // ESC для выхода

        public GameResult(int level, int treasures, boolean victory, boolean escaped) {
            this.level = level;
            this.treasures = treasures;
            this.victory = victory;
            this.escaped = escaped;
        }

        public int getLevel() { return level; }
        public int getTreasures() { return treasures; }
        public boolean isVictory() { return victory; }
        public boolean isEscaped() { return escaped; }
    }
}