package org.example;

import jcurses.system.Toolkit;
import jcurses.widgets.*;

public class Main {
    public static void main(String[] args) {
        Toolkit.init();

        Window window = new Window(5, 5, 30, 10, true, "Привет");


        Label label = new Label("Hello, JCurses!");
        window.add(label);

        window.show();
        Toolkit.readCharacter();
        window.close();
        Toolkit.shutdown();
    }
}