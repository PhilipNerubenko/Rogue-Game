package org.example.application.usecase;

import org.example.datalayer.entity.SessionStat;
import org.example.domain.entity.GameSession;

public interface SaveGameUseCase {
    void saveGame(GameSession session, SessionStat stats);
    boolean loadSavedGame(GameSession session, SessionStat stats);
    boolean hasSavedGames();
}
