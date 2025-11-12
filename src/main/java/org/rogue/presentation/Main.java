package org.rogue.presentation;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import sun.misc.Signal;

public class Main {
    public static void main(String[] args) {
        // Handle Ctrl+C so the terminal isn't left in a broken state
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\nTerminated via Ctrl+C");
            System.exit(0);
        });

        // Initialize jcurses
        Toolkit.init();

        // Print "Hello, world!" in the center of the screen
        Toolkit.printString("Hello, world!", 10, 5, new CharColor(CharColor.BLACK, CharColor.GREEN));
        Toolkit.printString("Press any key to exit...", 5, 7, new CharColor(CharColor.BLACK, CharColor.CYAN));

        // Wait for a key press
        Toolkit.readCharacter();

        // Properly shut down curses mode
        Toolkit.shutdown();
        System.out.println("\nProgram finished normally.");
    }
}
