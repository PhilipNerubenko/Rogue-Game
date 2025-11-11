package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import jcurses.widgets.*;

public class Main {
    public static void main(String[] args) {
//        Toolkit.init();
//        Window window = new Window(5, 5, 30, 10, true, "Привет");
//        Toolkit.printString("Hello, JCurses!", 2, 2, new CharColor(CharColor.WHITE, CharColor.BLUE));
//        window.show();
//        Toolkit.readCharacter();
//        window.close();
//        Toolkit.shutdown();


        Toolkit.init();
        Window window = new Window(10, 10, 80, 80, false, "Example");
        Toolkit.printString("Hello, JCurses1!", 5, 5, new CharColor(CharColor.BLACK,
                CharColor.RED));

        window.show();
        Toolkit.printString("Hello, JCurses2!", 5, 7, new CharColor(CharColor.BLUE, CharColor.WHITE));
        Toolkit.shutdown();
    }


}