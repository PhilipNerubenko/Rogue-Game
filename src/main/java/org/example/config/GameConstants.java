package org.example.config;

import jcurses.system.CharColor;

public final class GameConstants {

    public static final class Map {

        public static final int TERMINAL_WIDTH = 140;     //ширина терминала поля
        public static final int TERMINAL_HEIGHT = 40;    //высота терминала поля
        public static final int ROOMS = 9;     //комнат на уровне
        public static final int WIDTH = 80;     //ширина игрового поля
        public static final int HEIGHT = 30;    //высота игрового поля

        public static final int MAP_OFFSET_X = 3; // смещение карты от левого края
        public static final int MAP_OFFSET_Y = 3; // смещение карты от левого края
        public static final int VISION_RADIUS = 8; // радиус видимости игрока
    }

    public static final class PathToFiles {

        public static final String DATA_DIR = "data";

        public static final String AUTOSAVE_DIR = DATA_DIR + "/autosaves";
        public static final String AUTOSAVE_PREFIX = "autosave_";
        public static final String AUTOSAVE_EXTENSION = ".json";
        public static final int AUTOSAVE_MAX = 10;

        public static final String STATISTICS_PATH = DATA_DIR + "/statistics.json";
        public static final String SCOREBOARD_PATH = DATA_DIR + "/scoreboard.json";

    }

    public static final class ScreenConfig {
        public static final int MESSAGE_LINE_1 = Map.HEIGHT + 1;
        public static final int MESSAGE_LINE_2 = Map.HEIGHT + 2;
        public static final int MESSAGE_LINE_3 = Map.HEIGHT + 3;
        public static final int DEATH_MESSAGE_Y = -Map.MAP_OFFSET_Y;
        public static final int DEATH_MESSAGE_X = -Map.MAP_OFFSET_X;

        public static final String HIDE_CURSOR = "\033[?25l";
        public static final String SHOW_CURSOR = "\033[?25h";
        public static final String SIGINT_STRING = "INT";
    }

    public static final class ProbabilitiesAndBalance {
        public static final int MIN_HIT_CHANCE = 10;
        public static final int MAX_HIT_CHANCE = 90;
        public static final int AGILITY_MULTIPLIER = 5;
        public static final int GHOST_INVISIBILITY_FAR_CHANCE = 80;
        public static final int GHOST_INVISIBILITY_NEAR_CHANCE = 20;
        public static final int SNAKE_SLEEP_CHANCE = 10;
        public static final double MIN_ENEMY_DENSITY = 0.4;
        public static final double DENSITY_RANGE = 0.2;
        public static final int MIN_ROOMS_WITH_ENEMIES = 1;
        public static final int FOUR_DIRECTIONS = 4;
        public static final int EIGHT_DIRECTIONS = 8;
        public static final int GHOST_TELEPORT_RANGE = 3;
        public static final int OGRE_DOUBLE_STEP = 2;
        public static final int OGRE_REST_DURATION = 1;
    }

    public static final class TextMessages {
        public static final String TERMINATE = "\nTerminated via Ctrl+C";
        public static final String DIED = "YOU DIED! Press any key to exit";
        public static final String VICTORY = "Congratulations! YOU WIN! Press any key to exit";
        public static final String MISSED = "You missed!";
        public static final String MISSED_VAMPIRE = "First attack on vampire misses!";
        public static final int MESSAGE_DURATION = 2;
    }

    public static final class Colors {
        // Цветовые константы JCurses (ограничены палитрой 0-7)
        public static final short COLOR_BLACK = CharColor.BLACK;
        public static final short COLOR_RED = CharColor.RED;
        public static final short COLOR_GREEN = CharColor.GREEN;
        public static final short COLOR_YELLOW = CharColor.YELLOW;
        public static final short COLOR_BLUE = CharColor.BLUE;
        public static final short COLOR_MAGENTA = CharColor.MAGENTA;
        public static final short COLOR_CYAN = CharColor.CYAN;
        public static final short COLOR_WHITE = CharColor.WHITE;
    }

    public static final class Icons {
        public static final char W_WALL = '|';
        public static final char H_WALL = '~';
        public static final char CORRIDOR = '#';
        public static final char EMPTINESS = ' ';
        public static final char FLOOR = '.';
        public static final char PLAYER = '@';
        public static final char ZOMBIE = 'z';
        public static final char VAMPIRE = 'v';
        public static final char GHOST = 'g';
        public static final char SNAKE_MAGE = 's';
        public static final char OGRE = 'o';
        public static final char EXIT = '⇧';
        public static final char DOOR = '+';

        public static final char TREASURES = '$';
        public static final char FOOD = ',';
        public static final char ELIXIR = '!';
        public static final char SCROLL = '?';
        public static final char WEAPON = ')';
    }

    public static final class Control {
        public static final int KEY_W = 'w';
        public static final int KEY_S = 's';
        public static final int ESC_KEY_CODE = 27; // Выход// Стрелка вниз
    }

    public static final class Player {
        public static final int START_MAX_HEALTH = 30;
        public static final int START_AGILITY = 5;
        public static final int START_STRENGTH = 5;
        public static final int MAX_PER_TYPE = 9; //  9 предметов

    }
}
