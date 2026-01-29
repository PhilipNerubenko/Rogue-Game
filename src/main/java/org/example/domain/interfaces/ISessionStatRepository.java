package org.example.domain.interfaces;

import org.example.domain.entity.GameSession;
import org.example.domain.entity.SessionStat;
import java.io.IOException;
import java.util.List;

public interface ISessionStatRepository {
    void save(SessionStat sessionStat) throws IOException;
    void addToScoreboard(SessionStat sessionStat, GameSession gameSession) throws IOException;
    void reset(SessionStat sessionStat) throws IOException;
    List<SessionStat> getAllStats();
}
