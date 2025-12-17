package org.example.presentation;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;

import java.util.List;

public class GameMenu {

    private static final String[] NEW_GAME_ART = {
            "                GGGGGGGG            AAAAAAAAAA        MMMMMM         MMMMMM    EEEEEEEEEEEEEEEE         ",
            "              GGGGGGGGGGGG        AAAAAAAAAAAAAAA     MMMMMMM       MMMMMMM    EEEEEEEEEEEEEEEE         ",
            "             GGGGG     GGGGG     AAAAA      AAAAAA    MMMM MMM     MMM MMMM    EEEE                     ",
            "            GGGG         GGGG                AAAAA    MMMM  MMM   MMM  MMMM    EEEE                     ",
            "            GGGG                    AAAAAAAAAAAAAA    MMMM  MMM   MMM  MMMM    EEEEEEEEEEEEEEEE         ",
            "            GGGG     GGGGGGGG     AAAAAAAAAAAAAAAA    MMMM   MMM MMM   MMMM    EEEEEEEEEEEEEEEE         ",
            "            GGGG         GGGG    AAAA         AAAA    MMMM   MMMMMM    MMMM    EEEE                     ",
            "             GGGGG     GGGGG     AAAA         AAAA    MMMM    MMMM     MMMM    EEEE                     ",
            "              GGGGGGGGGGGGG       AAAAAAAAAAAAAAAA    MMMM    MMMM     MMMM    EEEEEEEEEEEEEEEE         ",
            "                GGGGGGGG            AAAAAAAAA AAAA    MMMM             MMMM    EEEEEEEEEEEEEEEE         ",
            "                                                                                                        ",
            " RRRRRRRRRRRR            OOOOOOOOO             GGGGGGGG         UUUU         UUUU     EEEEEEEEEEEEEEEE  ",
            " RRRRRRRRRRRRRR        OOOOOOOOOOOOO         GGGGGGGGGGGG       UUUU         UUUU     EEEEEEEEEEEEEEEE  ",
            " RRRR       RRRR      OOOOO     OOOOO       GGGGG     GGGGG     UUUU         UUUU     EEEE              ",
            " RRRR       RRRR     OOOO         OOOO     GGGG         GGGG    UUUU         UUUU     EEEE              ",
            " RRRRRRRRRRRRRR      OOOO         OOOO     GGGG                 UUUU         UUUU     EEEEEEEEEEEEEEEE  ",
            " RRRRRRRRRRRRR       OOOO         OOOO     GGGG     GGGGGGGG    UUUU         UUUU     EEEEEEEEEEEEEEEE  ",
            " RRRR   RRRR         OOOO         OOOO     GGGG         GGGG    UUUU         UUUU     EEEE              ",
            " RRRR     RRRR        OOOOO     OOOOO       GGGGG     GGGGG      UUUUU     UUUUUU     EEEE              ",
            " RRRR       RRRR       OOOOOOOOOOOOO         GGGGGGGGGGGGG        UUUUUUUUUUUUUU      EEEEEEEEEEEEEEEE  ",
            " RRRR        RRRR        OOOOOOOOO             GGGGGGGG             UUUUUUUUUU        EEEEEEEEEEEEEEEE  ",
            "                                                                                                        ",
            "                                 trangbow,  oznakdat & celestma                                         ",
            "                                                                                                        ",
//            "                                            GAME  MENU                                                  ",
//            "                                +------------------------------+                                        ",
//            "                                |                              |                                        ",
//            "                                |          NEW   GAME          |                                        ",
//            "                                |          LOAD  GAME          |                                        ",
//            "                                |          SCOREBOARD          |                                        ",
//            "                                |          EXIT  GAME          |                                        ",
//            "                                |                              |                                        ",
//            "                                +------------------------------+                                        ",
    };

    private static final String[] GAME_OVER_ART = {
            "                GGGGGGGG            AAAAAAAAAA        MMMMMM         MMMMMM    EEEEEEEEEEEEEEEE         ",
            "              GGGGGGGGGGGG        AAAAAAAAAAAAAAA     MMMMMMM       MMMMMMM    EEEEEEEEEEEEEEEE         ",
            "             GGGGG     GGGGG     AAAAA      AAAAAA    MMMM MMM     MMM MMMM    EEEE                     ",
            "            GGGG         GGGG                AAAAA    MMMM  MMM   MMM  MMMM    EEEE                     ",
            "            GGGG                    AAAAAAAAAAAAAA    MMMM  MMM   MMM  MMMM    EEEEEEEEEEEEEEEE         ",
            "            GGGG     GGGGGGGG     AAAAAAAAAAAAAAAA    MMMM   MMM MMM   MMMM    EEEEEEEEEEEEEEEE         ",
            "            GGGG         GGGG    AAAA         AAAA    MMMM   MMMMMM    MMMM    EEEE                     ",
            "             GGGGG     GGGGG     AAAA         AAAA    MMMM    MMMM     MMMM    EEEE                     ",
            "              GGGGGGGGGGGGG       AAAAAAAAAAAAAAAA    MMMM    MMMM     MMMM    EEEEEEEEEEEEEEEE         ",
            "                GGGGGGGG            AAAAAAAAA AAAA    MMMM             MMMM    EEEEEEEEEEEEEEEE         ",
            "                                                                                                        ",
            "                OOOOOOOOO        VVVV              VVVV    EEEEEEEEEEEEEEEE    RRRRRRRRRRRR             ",
            "              OOOOOOOOOOOOO       VVVV            VVVV     EEEEEEEEEEEEEEEE    RRRRRRRRRRRRRR           ",
            "             OOOOO     OOOOO       VVVV          VVVV      EEEE                RRRR       RRRR          ",
            "            OOOO         OOOO       VVVV        VVVV       EEEE                RRRR       RRRR          ",
            "            OOOO         OOOO        VVVV      VVVV        EEEEEEEEEEEEEEEE    RRRRRRRRRRRRRR           ",
            "            OOOO         OOOO         VVVV    VVVV         EEEEEEEEEEEEEEEE    RRRRRRRRRRRRR            ",
            "            OOOO         OOOO          VVVV  VVVV          EEEE                RRRR   RRRR              ",
            "             OOOOO     OOOOO            VVVVVVVV           EEEE                RRRR    RRRR             ",
            "              OOOOOOOOOOOOO              VVVVVV            EEEEEEEEEEEEEEEE    RRRR     RRRR            ",
            "                OOOOOOOOO                 VVVV             EEEEEEEEEEEEEEEE    RRRR      RRRR           ",
            "                                                                                                        ",
            "                                                                                                        ",
            "                                                                                                        ",
//            "                                            GAME  OVER                                                  ",
//            "                                +------------------------------+                                        ",
//            "                                |                              |                                        ",
//            "                                |          NEW   GAME          |                                        ",
//            "                                |          LOAD  GAME          |                                        ",
//            "                                |          SCOREBOARD          |                                        ",
//            "                                |          EXIT  GAME          |                                        ",
//            "                                |                              |                                        ",
//            "                                +------------------------------+                                        ",
    };

    private static final List<String> MENU_ITEMS = List.of(
            "NEW GAME",
            "LOAD GAME",
            "SCOREBOARD",
            "EXIT GAME"
    );

    private int selectedIndex = 0;
    private boolean menuActive = true;
    private MenuAction selectedAction = null;
    private boolean isGameOver = false;

    public enum MenuAction {
        NEW_GAME,
        LOAD_GAME,
        SCOREBOARD,
        EXIT_GAME
    }

    public GameMenu() {
        this(false);
    }

    public GameMenu(boolean isGameOver) {
        this.isGameOver = isGameOver;
        this.selectedIndex = 0;
    }

    public MenuAction showAndGetAction() {
        menuActive = true;
        selectedAction = null;

        while (menuActive) {
            render();
            handleInput();
        }

        return selectedAction;
    }

    private void render() {
        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));

        // Выбираем заголовок
        String[] titleArt = isGameOver ? GAME_OVER_ART : NEW_GAME_ART;
        String screenTitle = isGameOver ? "GAME OVER" : "GAME MENU";

        // Тест отдельных символов

        // Рисуем заголовок
        CharColor titleColor = new CharColor(CharColor.BLACK, CharColor.WHITE);

        int startY = 1;
        for (int i = 0; i < titleArt.length; i++) {
            Toolkit.printString(titleArt[i], 2, startY + i, titleColor);
        }


        // Показываем статистику после Game Over (если есть)
        if (isGameOver && lastGameStats != null) {
            CharColor statsColor = new CharColor(CharColor.YELLOW, CharColor.BLACK);
            Toolkit.printString("Достигнут уровень: " + lastGameStats.getLevel(),
                    50, startY + titleArt.length - 5, statsColor);
            Toolkit.printString("Собрано сокровищ: " + lastGameStats.getTreasures(),
                    50, startY + titleArt.length - 3, statsColor);
        }

        // Рисуем меню
        int menuStartY = startY + titleArt.length + 2;
        int menuStartX = 50;

        for (int i = 0; i < MENU_ITEMS.size(); i++) {
            CharColor color = (i == selectedIndex) ?
                    new CharColor(CharColor.BLACK, CharColor.WHITE) :
                    new CharColor(CharColor.WHITE, CharColor.BLACK);

            String itemText = MENU_ITEMS.get(i);
            itemText = (i == selectedIndex) ? "> " + itemText + " <" : "  " + itemText + "  ";

            Toolkit.printString(itemText, menuStartX, menuStartY + i * 2, color);
        }

        // Инструкция
        CharColor hintColor = new CharColor(CharColor.GREEN, CharColor.BLACK);
        String hintText;
        if (isGameOver) {
            hintText = "W/S: Навигация, ENTER: Выбор, ESC: Главное меню";
        } else {
            hintText = "W/S: Навигация, ENTER: Выбор, ESC: Выход из игры";
        }

        Toolkit.printString(hintText, 30, menuStartY + MENU_ITEMS.size() * 2 + 2, hintColor);
    }

    private void handleInput() {
        InputChar ch = Toolkit.readCharacter();

        // ESC: в Game Over = NEW GAME меню, в основном меню = EXIT
        if (ch.getCode() == 27) {
            if (isGameOver) {
                selectedAction = MenuAction.NEW_GAME; // Возврат к выбору новой игры
            } else {
                selectedAction = MenuAction.EXIT_GAME;
            }
            menuActive = false;
            return;
        }

        char character;
        try {
            character = ch.getCharacter();
        } catch (RuntimeException e) {
            return;
        }

        switch (Character.toLowerCase(character)) {
            case 'w':
                selectedIndex = Math.max(0, selectedIndex - 1);
                break;
            case 's':
                selectedIndex = Math.min(MENU_ITEMS.size() - 1, selectedIndex + 1);
                break;
            case '\n':
            case '\r':
                selectMenuItem();
                break;
        }
    }

    private void selectMenuItem() {
        String selectedItem = MENU_ITEMS.get(selectedIndex);

        switch (selectedItem) {
            case "NEW GAME":
                selectedAction = MenuAction.NEW_GAME;
                break;
            case "LOAD GAME":
                selectedAction = MenuAction.LOAD_GAME;
                break;
            case "SCOREBOARD":
                selectedAction = MenuAction.SCOREBOARD;
                break;
            case "EXIT GAME":
                selectedAction = MenuAction.EXIT_GAME;
                break;
        }
        menuActive = false;
    }

    // Для отображения статистики после Game Over
    private static GameStats lastGameStats = null;

    public static void setLastGameStats(GameStats stats) {
        lastGameStats = stats;
    }

    public static class GameStats {
        private final int level;
        private final int treasures;
        private final boolean victory;

        public GameStats(int level, int treasures, boolean victory) {
            this.level = level;
            this.treasures = treasures;
            this.victory = victory;
        }

        public int getLevel() { return level; }
        public int getTreasures() { return treasures; }
        public boolean isVictory() { return victory; }
    }
}