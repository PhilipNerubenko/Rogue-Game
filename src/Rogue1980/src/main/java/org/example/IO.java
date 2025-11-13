package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IO {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[92m";
    private static final String ANSI_RED = "\u001B[91m";
    private static final String ANSI_YELLOW = "\u001B[93m";
    private static final String ANSI_BLUE = "\u001B[94m";
    private static final String ANSI_BROWN = "\u001B[33m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String error = "An error occurred";
    private static String lastColor = ANSI_RESET;

    private static final Map<Character, String> colorMap = Map.of(
            'Z', ANSI_GREEN,
            'V', ANSI_RED,
            'G', ANSI_BLUE,
            'S', ANSI_BLUE,
            'O', ANSI_YELLOW,
            '·', ANSI_BROWN,
            '@', ANSI_PURPLE,
            '⇧', ANSI_PURPLE,
            '#', ANSI_CYAN,
            't', ANSI_CYAN);

    private IO() {
    }

    public static char getCh() {
        char input = 'q';
        try {
            cmdRawMode();
            input = (char) System.in.read();
            resetCmd();

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            Logger.getLogger(IO.class.getName()).log(Level.SEVERE, error, e);
        }

        return input;
    }

    public static void print(char[][] printChar) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < printChar.length; i++) {
            for (int j = 0; j < printChar[i].length; j++) {
                chooseColor(printChar[i][j], sb);
            }
            sb.append((i == printChar.length - 1) ? "" : "\n");
        }

        clearScreen();
        System.out.print(sb);
    }

    private static void chooseColor(char currentChar, StringBuilder sb) {
        String color = colorMap.getOrDefault(currentChar, ANSI_RESET);

        if (!lastColor.equals(color)) {
            sb.append(color);
        }
        sb.append(currentChar);
        lastColor = color;
    }

    private static void clearScreen() {
        System.out.print("\n\033[2J");
    }

    public static void hideCursor() {
        System.out.print("\033[?25l");
    }

    public static void showCursor() {
        System.out.print("\033[?25h");
    }

    private static void cmdRawMode() throws Exception {
        String[] cmd = { "/bin/sh", "-c", "stty raw -echo </dev/tty" };
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    public static void resetCmd() {
        try {
            String[] resetCmd = { "/bin/sh", "-c", "stty sane </dev/tty" };
            Runtime.getRuntime().exec(resetCmd).waitFor();

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            Logger.getLogger(IO.class.getName()).log(Level.SEVERE, error, e);
        }
    }

    public static String getString() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
            input = reader.readLine();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            Logger.getLogger(IO.class.getName()).log(Level.SEVERE, error, e);
        }

        return input;
    }
}
