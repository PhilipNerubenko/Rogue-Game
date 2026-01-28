package org.example.domain.controller;

import org.example.domain.enums.MenuAction;
import org.example.domain.interfaces.Renderer;
import org.example.domain.model.SaveSlotUiModel;
import org.example.domain.service.AutosaveService;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.SessionStat;
import org.example.domain.factory.LevelGenerator;

import java.util.List;

import static org.example.config.GameConstants.Colors.COLOR_RED;
import static org.example.config.GameConstants.Colors.COLOR_YELLOW;
import static org.example.config.GameConstants.Control.*;

public class LoadMenuController {

    private final Renderer renderer;
    private final AutosaveService autosaveService;
    private final GameSession session;
    private final SessionStat sessionStat;
    private final LevelGenerator levelGenerator;
    private int selected = 0;

    public LoadMenuController(Renderer renderer, AutosaveService autosaveService,
                              GameSession session, SessionStat sessionStat,
                              LevelGenerator levelGenerator) {
        this.renderer = renderer;
        this.autosaveService = autosaveService;
        this.session = session;
        this.sessionStat = sessionStat;
        this.levelGenerator = levelGenerator;
    }

    public MenuAction show() {
        List<SaveSlotUiModel> allSlots = autosaveService.getSaveSlots();
        List<SaveSlotUiModel> realSlots = allSlots.stream()
                .filter(slot -> !slot.isEmpty())
                .toList();

        // Проверка наличия сохранений
        if (realSlots.stream().allMatch(SaveSlotUiModel::isEmpty)) {
            renderer.clearScreen();
            renderer.drawMessage(10, "No saved games available!", COLOR_YELLOW);
            renderer.drawMessage(12, "Press any key to return...", COLOR_YELLOW);
            renderer.readCharacter();
            return MenuAction.EXIT;
        }

        while (true) {
            renderer.drawLoadGameScreen(selected, realSlots);

            int code = renderer.readCharacter();

            if (code == KEY_W || code == 'W') {
                selected = Math.max(0, selected - 1);
            } else if (code == KEY_S || code == 'S') {
                selected = Math.min(realSlots.size() - 1, selected + 1);
            } else if (code == '\n' || code == '\r') {
                SaveSlotUiModel selectedSlot = realSlots.get(selected);
                if (!selectedSlot.isEmpty()) {
                    boolean success = autosaveService.loadSpecificSave(
                            selected, session, sessionStat, levelGenerator);
                    if (success) {
                        return MenuAction.LOAD_GAME;
                    } else {
                        renderer.drawMessage(15, "Failed to load save!", COLOR_RED);
                        renderer.readCharacter();
                    }
                }
            } else if (code == ESC_KEY_CODE) {
                return MenuAction.EXIT;
            }
        }
    }
}