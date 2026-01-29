package org.example.domain.interfaces;

import org.example.datalayer.GameState;

import java.util.List;

public interface IAutosaveRepository {
    boolean save(GameState gameState);
    GameState loadLatest();
    GameState load(int slotIndex);
    List<String> getSaveInfo();
    void cleanupOldSaves();
    boolean hasSaves();
}