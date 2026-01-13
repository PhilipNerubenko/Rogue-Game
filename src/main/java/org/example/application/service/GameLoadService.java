package org.example.application.service;

import org.example.application.GameInitializer;

public class GameLoadService {
    public boolean load(GameInitializer initializer) {
        var input = initializer.getSaveGameUseCase();

        if (!input.hasSavedGames()) {
            return false;
        }

        boolean loaded = input.loadSavedGame(
                initializer.getSession(),
                initializer.getSessionStat()
        );

        if (!loaded) return false;

        var fog = initializer.getFogOfWarService();
        if (fog != null) {
            var pos = initializer.getSession().getPlayer().getPosition();
            fog.updateForLoadedGame(pos, initializer.getSession().getCurrentMap());
        }

        return true;
    }
}