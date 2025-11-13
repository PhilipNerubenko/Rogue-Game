package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;

public class Main1 {
    public static void main(String[] args) {
        System.out.println("=== DEBUG MODE ===");
        System.out.println("Terminal: " + System.getenv("TERM"));
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println("Press keys to see codes (ESC to exit)...");

        try { Thread.sleep(1000); } catch (Exception e) {}

        // ВАЖНО: Инициализируем
        Toolkit.init();

        // Устанавливаем raw режим ВНУТРИ Java
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty raw -echo < /dev/tty"}).waitFor();
            System.out.println("Raw mode: ENABLED");
        } catch (Exception e) {
            System.err.println("Raw mode FAILED: " + e.getMessage());
        }

        Toolkit.clearScreen(new CharColor(CharColor.BLACK, CharColor.BLACK));
        Toolkit.printString("Press keys to test:", 0, 0, new CharColor(CharColor.WHITE, CharColor.BLACK));

        boolean running = true;
        int y = 2;

        while (running) {
            InputChar ch = Toolkit.readCharacter();
            int code = ch.getCode();

            // Показываем код на экране
            String debug = "Code: " + code + " | Char: '" + (char)code + "'    ";
            Toolkit.printString(debug, 0, y, new CharColor(CharColor.CYAN, CharColor.BLACK));
            y = (y + 1) % 20;

            if (code == 27) running = false; // ESC
        }

        // Восстанавливаем терминал
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty sane < /dev/tty"}).waitFor();
        } catch (Exception e) {}

        Toolkit.shutdown();
        System.out.println("\nProgram finished.");
    }
}
