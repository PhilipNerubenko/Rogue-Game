package org.example.config;

public final class GameConstants {

    public static final class Map {
        public static final int ROOMS = 9;     //комнат на уровне
        public static final int WIDTH = 80;     //ширина игрового поля
        public static final int HEIGHT = 30;    //высота игрового поля

        public static final int MAP_LEVEL = 1;
        public static final int MAP_OFFSET_X = 8; // смещение карты от левого края
        public static final int MAP_OFFSET_Y = 3; // смещение карты от левого края
        public static final int VISION_RADIUS = 8; // радиус видимости игрока
    }

    public static final class PathToFiles {

        public static final String DATA_DIR = "data";
        public static final String SAVES_DIR = DATA_DIR + "/saves";
        public static final String SAVE_PATH = DATA_DIR + "/save.json";
        public static final String STATISTICS_PATH = DATA_DIR + "/statistics.json";
        public static final String SCOREBOARD_PATH = DATA_DIR + "/scoreboard.json";

    }

    public static final class ScreenConfig {
        public static final int MAP_WIDTH = 80;
        public static final int MAP_HEIGHT = 25;
        public static final int UI_START_Y = 26;
        public static final int STATUS_LINE_Y = 30;
        public static final int MESSAGE_LINE_1 = 27;
        public static final int MESSAGE_LINE_2 = 28;
        public static final int MESSAGE_LINE_3 = 29;
        public static final int DEATH_MESSAGE_Y = 31;
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
        public static final String CONTROL = "WASD | . rest | ESC | h/j/k/e use | 1-9 choose (wep 0-9, 0 off)";
        public static final String DIED = "YOU DIED! Press ESC to exit";
        public static final String VICTORY = "Congratulations! YOU VICTORY!   Press ESC to exit";
        public static final String MISSED = "You missed!";
        public static final String MISSED_VAMPIRE = "First attack on vampire misses!";
        public static final String CONTROL_HINT = "Use WASD to move, ESC to exit";
    }

    public static final class Colors {
        // Используем стандартные цвета JCurses (0-7)
        public static final int BLACK = 0;
        public static final int RED = 1;
        public static final int GREEN = 2;
        public static final int YELLOW = 3;
        public static final int BLUE = 4;
        public static final int MAGENTA = 5;
        public static final int CYAN = 6;
        public static final int WHITE = 7;

        // ANSI escape codes для консоли (если нужно)
        public static final String ANSI_GREEN = "\033[92m";
        public static final String ANSI_RED = "\033[91m";
        public static final String ANSI_RESET = "\033[0m";
    }

    public static final class Icons {
        public static final char W_WALL = '|';
        public static final char H_WALL = '~';
        public static final char CORRIDOR = '#';
        public static final char EMPTINESS = ' ';
        public static final char FLOOR = '.';  //Это точка!!!!!
        public static final char PLAYER = '@';
        public static final char ZOMBIE = 'z';
        public static final char VAMPIRE = 'v';
        public static final char GHOST = 'g';
        public static final char SNAKE_MAGE = 's';
        public static final char OGRE = 'o';
        public static final char EXIT = '⇧';
    }

    public static final class control {
        public static final int KEY_W = 'w';
        public static final int KEY_S = 's';
        public static final int KEY_A = 'a';
        public static final int KEY_D = 'd';
        public static final int REST1 = '.';
        public static final int REST2 = ' ';
        private static final int KEY_H = 'h'; // Оружие
        private static final int KEY_J = 'j'; // Еда
        private static final int KEY_K = 'k'; // Эликсир
        private static final int KEY_E = 'e'; // Свиток
        private static final int KEY_Q = 'q'; // Выход
        public static final int ESC_KEY_CODE = 27; // Выход
    }

    public static final class Player {
        public static final int START_HEALTH = 30;
        public static final int START_MAX_HEALTH = 30;  // <-- ДОБАВЬТЕ ЭТОТЬ
        public static final int START_AGILITY = 5;
        public static final int START_STRENGTH = 5;
        public static final int SIZE_BACKPACK = 36; //  9 предметов  х  4 типа(еда, свитки, эликсир, оружие)
        public static final int MAX_PER_TYPE = 9; //  9 предметов

    }
}
