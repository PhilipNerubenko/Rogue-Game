package org.example.application.service;

import org.example.application.GameInitializer;
import org.example.datalayer.SessionStat;
import org.example.application.records.LoadedGame;

public class GameLoadService {

    public LoadedGame loadGame(GameInitializer initializer) {
        var input = initializer.getSaveGameUseCase();

        if (!input.hasSavedGames()) {
            return null;
        }

        boolean loaded = input.loadSavedGame(
                initializer.getSession(),
                initializer.getSessionStat()
        );

        if (!loaded) return null;

        var fog = initializer.getFogOfWarService();
        if (fog != null) {
            var pos = initializer.getSession().getPlayer().getPosition();
            fog.updateForLoadedGame(pos, initializer.getSession().getCurrentMap());
        }

        return new LoadedGame(initializer, initializer.getSessionStat());
    }
}

