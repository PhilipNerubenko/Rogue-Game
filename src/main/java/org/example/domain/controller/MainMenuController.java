package org.example.domain.controller;

import org.example.domain.enums.MenuAction;
import org.example.domain.interfaces.Renderer;

import static org.example.config.GameConstants.control.*;

public class MainMenuController {

    private final Renderer renderer;
    private int selected = 0;

    public MainMenuController(Renderer renderer) {
        this.renderer = renderer;
    }

    public MenuAction show() {
        while (true) {
            renderer.drawMenuScreen(selected);

            int code = renderer.readCharacter();

            if (code == KEY_W || code == 'W') {
                selected = Math.max(0, selected - 1);
            } else if (code == KEY_S || code == 'S') {
                selected = Math.min(MenuAction.values().length - 1, selected + 1);
            } else if (code == '\n' || code == '\r') {
                return MenuAction.values()[selected];
            } else if (code == ESC_KEY_CODE) {
                return MenuAction.EXIT;
            }
        }
    }
}
