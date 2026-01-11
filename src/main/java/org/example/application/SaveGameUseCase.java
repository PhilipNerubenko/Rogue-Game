package org.example.application;

import org.example.datalayer.SessionStat;
import org.example.domain.entity.GameSession;

public interface SaveGameUseCase {
    void saveGame(GameSession session, SessionStat stats);
    boolean loadSavedGame(GameSession session, SessionStat stats);
    boolean hasSavedGames();
}
