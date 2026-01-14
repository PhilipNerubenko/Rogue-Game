package org.example.presentation.controllers;

import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.presentation.views.JCursesRenderer;

public class MainMenuController {

    private final JCursesRenderer renderer;
    private int selected = 0;

    public MainMenuController(JCursesRenderer renderer) {
        this.renderer = renderer;
    }

    public MenuAction show() {
        while (true) {
            renderer.drawMenuScreen(selected);

            InputChar input = Toolkit.readCharacter();
            int code = input.getCode();

            if (code == InputChar.KEY_UP || code == 'w' || code == 'W') {
                selected = Math.max(0, selected - 1);
            } else if (code == InputChar.KEY_DOWN || code == 's' || code == 'S') {
                selected = Math.min(MenuAction.values().length - 1, selected + 1);
            } else if (code == '\n' || code == '\r') {
                return MenuAction.values()[selected];
            } else if (code == 27) {
                return MenuAction.EXIT;
            }
        }
    }
}
