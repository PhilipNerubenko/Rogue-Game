package org.example.domain.service;

import org.example.domain.entity.GameSession;
import org.example.domain.entity.SessionStat;
import org.example.domain.interfaces.ISessionStatRepository;
import java.io.IOException;
import java.util.List;

public class StatisticsService {
    private final ISessionStatRepository repository;

    public StatisticsService(ISessionStatRepository repository) {
        this.repository = repository;
    }

    public void incrementLevel(SessionStat stats) throws IOException {
        stats.setLevelNum(stats.getLevelNum() + 1);
        repository.save(stats);
    }

    public void incrementEnemies(SessionStat stats) throws IOException {
        stats.setEnemies(stats.getEnemies() + 1);
        repository.save(stats);
    }

    public void incrementFood(SessionStat stats) throws IOException {
        stats.setFood(stats.getFood() + 1);
        repository.save(stats);
    }

    public void incrementElixirs(SessionStat stats) throws IOException {
        stats.setElixirs(stats.getElixirs() + 1);
        repository.save(stats);
    }

    public void incrementScrolls(SessionStat stats) throws IOException {
        stats.setScrolls(stats.getScrolls() + 1);
        repository.save(stats);
    }

    public void incrementAttacks(SessionStat stats) throws IOException {
        stats.setAttacks(stats.getAttacks() + 1);
        repository.save(stats);
    }

    public void incrementMissed(SessionStat stats) throws IOException {
        stats.setMissed(stats.getMissed() + 1);
        repository.save(stats);
    }

    public void incrementMoves(SessionStat stats) throws IOException {
        stats.setMoves(stats.getMoves() + 1);
        repository.save(stats);
    }

    public void addTreasures(int count, SessionStat stats) throws IOException {
        stats.setTreasures(stats.getTreasures() + count);
        repository.save(stats);
    }

    public void reset(SessionStat stats) throws IOException {
        repository.reset(stats);
    }

    public void addToScoreboard(SessionStat sessionStat, GameSession gameSession) throws IOException {
        repository.addToScoreboard(sessionStat, gameSession);
    }

    public List<SessionStat> getScoreboardStats() {
        return repository.getAllStats();
    }
}