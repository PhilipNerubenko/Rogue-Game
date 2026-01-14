package org.example.application.usecase.impl;

import org.example.application.usecase.SaveGameUseCase;
import org.example.datalayer.service.AutosaveService;
import org.example.domain.entity.GameSession;
import org.example.datalayer.entity.SessionStat;
import org.example.domain.factory.LevelGenerator;

public class AutosaveGameUseCase implements SaveGameUseCase {
    private final AutosaveService autosaveService;
    private final LevelGenerator levelGenerator;

    public AutosaveGameUseCase(AutosaveService autosaveService,
                               LevelGenerator levelGenerator) {
        this.autosaveService = autosaveService;
        this.levelGenerator = levelGenerator;
    }

    @Override
    public void saveGame(GameSession session, SessionStat stats) {
        if (session != null && session.getPlayer() != null && !session.getPlayer().isDead()) {
            autosaveService.saveGame(session, stats);
        }
    }

    @Override
    public boolean loadSavedGame(GameSession session, SessionStat stats) {
        return autosaveService.loadAndRestoreGame(session, stats, levelGenerator);
    }

    @Override
    public boolean hasSavedGames() {
        return autosaveService.hasSaves();
    }
}