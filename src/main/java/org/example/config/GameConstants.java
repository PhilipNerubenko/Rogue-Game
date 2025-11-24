package org.example.config;

public final class GameConstants {
    private GameConstants() {
    }

    public static final class Map {
        public static final int ROOMS  = 9;     //комнат на уровне
        public static final int WIDTH = 80;     //ширина игрового поля
        public static final int HEIGHT = 30;    //высота игрового поля
        public static final int MAP_OFFSET_X = 3; // смещение карты от левого края
        public static final int VISION_RADIUS = 8; // радиус видимости игрока
        private Map() {
        }
    }

    public static final class Colors {
        public static final String GREEN = "\033[92m";
        public static final String RED = "\033[91m";
        public static final String RESET = "\033[0m";
        private Colors() {
        }
    }

    public static final class Icons {
        public static final char WWALL = '|';
        public static final char HWALL = '~';
        public static final char CORRIDOR = '#';
        public static final char FLOOR = '.';  //Это точка!!!!!
        public static final char PLAYER = '@';
        public static final char ZOMBIE = 'Z';
        public static final char VAMPIRE = 'V';
        public static final char GHOST = 'G';
        public static final char SERPENT_MAGE = 'S';
        public static final char OGRE = 'O';
        public static final char EXIT = '⇧';
    }

    public static final class control {
        public static final int KEY_W = 'w';
        public static final int KEY_S = 's';
        public static final int KEY_A = 'a';
        public static final int KEY_D = 'd';
        private static final int KEY_H = 'h'; // Оружие
        private static final int KEY_J = 'j'; // Еда
        private static final int KEY_K = 'k'; // Эликсир
        private static final int KEY_E = 'e'; // Свиток
        private static final int KEY_Q = 'q'; // Выход
    }

    public static final class Player {
        public static final int START_HEALTH = 30;
        public static final int START_MAX_HEALTH = 30;  // <-- ДОБАВЬТЕ ЭТОТЬ
        public static final int START_AGILITY = 5;
        public static final int START_STRENGTH = 5;
        private Player() {}

    }

//    public static final class MokMap {
//        public static final String[] myArray = {
//                "      |~~~~~~~~~~~~|               |~~~~~~~~~|                                  ",
//                "      |............|              #+.........|        |~~~~~~~~~~~~~~~~~~~~~~~~|",
//                "      |............+###############|.........| #######+........................|",
//                "      |............|               |.........| #      |........................|",
//                "      |............|               |.........+##      |........................|",
//                "      |............|               |.........|        |~~~~~~+~~~~~~~~~~~~~~~~~|",
//                "      |~~~~~~~~~~~+|               |~~~+~~~~~|               #                  ",
//                "                  #                    ###                   ###############    ",
//                "     ##############                  |~~~+|                                #    ",
//                "  |~~+~~~~~~~~~~~~~~~~~|             |....+##########     |~~~~~~~~~~~~~~~~+|   ",
//                "  |....................|      #######+....|         #     |.................|   ",
//                "  |....................+#######      |....|         #     |.................|   ",
//                "  |....................|             |....|         ######+.................|   ",
//                "  |....................|             |....|               |.................|   ",
//                "  |~~~~~~~~~~~~~~~~+~~~|             |....|               |~~~~~~~~~~~~~~~~+|   ",
//                "                   #                 |~~~+|                                #    ",
//                "                   #                     #                    ##############    ",
//                "                   #                    ##                    #                 ",
//                "                   #                    #                     #                 ",
//                "                ####                 |~~+~|                   #                 ",
//                "                #                    |....|                   #                 ",
//                "        |~~~~~~~+~|                  |....|            |~~~~~~+~~~~~~~~~~~~~~~| ",
//                "        |.........|                  |....| ###########+......................| ",
//                "        |.........|                  |....| #          |......................| ",
//                "        |.........+#############     |....| #          |......................| ",
//                "        |.........|            #     |....| #          |......................| ",
//                "        |~~~~~~~~~|            ######+....+##          |~~~~~~~~~~~~~~~~~~~~~~| ",
//                "                                     |~~~~|                                     ",
//                "                                                                                "
//        };
//
//    }
}