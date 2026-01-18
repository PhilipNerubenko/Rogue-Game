package org.example.domain.service;

import org.example.datalayer.GameState;
import org.example.domain.entity.*;
import org.example.domain.enums.ItemType;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.interfaces.IAutosaveRepository;
import org.example.domain.model.Position;
import org.example.domain.model.Room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис для управления автосохранениями игры
 */
public class AutosaveService {
    private final IAutosaveRepository repository;
    private final FogOfWarService fogOfWarService;

    public AutosaveService(IAutosaveRepository repository, FogOfWarService fogOfWarService) {
        this.repository = repository;
        this.fogOfWarService = fogOfWarService;
    }

    /**
     * Сохраняет текущее состояние игры
     */
    public void saveGame(GameSession session, SessionStat sessionStat) {
        if (session.getPlayer() == null || session.getPlayer().isDead()) {
            System.err.println("Cannot save: player is dead or not initialized");
            return;
        }

        try {
            GameState gameState = createGameState(session, sessionStat);
            repository.save(gameState);
        } catch (Exception e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загружает и восстанавливает игру из последнего сохранения
     */
    public boolean loadAndRestoreGame(GameSession session, SessionStat sessionStat,
                                      LevelGenerator levelGenerator) {
        GameState gameState = repository.loadLatest();
        if (gameState == null) {
            return false;
        }

        try {
            restoreGameState(gameState, session, sessionStat, levelGenerator);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to restore game state: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private GameState createGameState(GameSession session, SessionStat sessionStat) {
        GameState gameState = new GameState();
        gameState.setPlayerState(createPlayerState(session.getPlayer()));
        gameState.setLevelState(createLevelState(session));
        gameState.setFogOfWarState(createFogOfWarState());
        gameState.setSessionStat(sessionStat);

        GameState.GameSessionState gameSessionState = new GameState.GameSessionState();
        gameSessionState.setCurrentMap(session.getCurrentMap());
        gameState.setGameSessionState(gameSessionState);

        return gameState;
    }

    private GameState.PlayerState createPlayerState(Player player) {
        GameState.PlayerState playerState = new GameState.PlayerState();
        playerState.setMaxHealth(player.getMaxHealth());
        playerState.setHealth(player.getHealth());
        playerState.setAgility(player.getAgility());
        playerState.setStrength(player.getStrength());
        playerState.setPositionX(player.getPosition().getX());
        playerState.setPositionY(player.getPosition().getY());
        playerState.setSleepTurns(player.getSleepTurns());

        // Сохраняем инвентарь
        Inventory inventory = player.getInventory();
        List<Item> allItems = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            allItems.addAll(inventory.getItems(type));
        }
        playerState.setInventoryItems(allItems);
        playerState.setEquippedWeapon(player.getEquippedWeapon());

        return playerState;
    }

    private GameState.LevelState createLevelState(GameSession session) {
        GameState.LevelState levelState = new GameState.LevelState();
        levelState.setLevelNumber(session.getLevelNum());
        levelState.setAsciiMap(session.getCurrentMap());
        levelState.setItems(new ArrayList<>(session.getCurrentLevelItems()));
        levelState.setEnemies(new ArrayList<>(session.getEnemies()));

        if (session.getRooms() != null) {
            levelState.setRooms(new ArrayList<>(session.getRooms()));
        }

        return levelState;
    }

    private GameState.FogOfWarState createFogOfWarState() {
        GameState.FogOfWarState fogOfWarState = new GameState.FogOfWarState();

        if (fogOfWarService != null) {
            List<Position> exploredCells = new ArrayList<>(fogOfWarService.getAllExploredCells());
            List<Room> exploredRooms = new ArrayList<>(fogOfWarService.getAllExploredRooms());
            fogOfWarState.setExploredCells(exploredCells);
            fogOfWarState.setExploredRooms(exploredRooms);
        } else {
            fogOfWarState.setExploredCells(new ArrayList<>());
            fogOfWarState.setExploredRooms(new ArrayList<>());
        }

        return fogOfWarState;
    }

    private void restoreGameState(GameState gameState, GameSession session,
                                  SessionStat sessionStat, LevelGenerator levelGenerator) {
        if (gameState == null) {
            System.err.println("[Autosave] ERROR: GameState is null!");
            return;
        }

        restoreSessionStatistics(gameState, sessionStat);
        restoreLevelGenerator(gameState, levelGenerator);
        restoreLevelState(gameState, session);
        restoreFogOfWar(gameState);
        restorePlayer(gameState, session);
    }

    private void restoreSessionStatistics(GameState gameState, SessionStat sessionStat) {
        SessionStat saved = gameState.getSessionStat();
        if (saved != null) {
            sessionStat.setTreasures(saved.getTreasures());
            sessionStat.setLevelNum(saved.getLevelNum());
            sessionStat.setEnemies(saved.getEnemies());
            sessionStat.setFood(saved.getFood());
            sessionStat.setElixirs(saved.getElixirs());
            sessionStat.setScrolls(saved.getScrolls());
            sessionStat.setAttacks(saved.getAttacks());
            sessionStat.setMissed(saved.getMissed());
            sessionStat.setMoves(saved.getMoves());
        }
    }

    private void restoreLevelGenerator(GameState gameState, LevelGenerator levelGenerator) {
        if (levelGenerator != null && gameState.getLevelState() != null) {
            levelGenerator.restoreFromGameState(
                    gameState.getLevelState().getAsciiMap(),
                    gameState.getLevelState().getRooms(),
                    gameState.getLevelState().getItems()
            );
        }
    }

    private void restoreLevelState(GameState gameState, GameSession session) {
        GameState.LevelState levelState = gameState.getLevelState();
        session.setLevelNum(levelState.getLevelNumber());
        session.setCurrentMap(levelState.getAsciiMap());

        session.getCurrentLevelItems().clear();
        if (levelState.getItems() != null) {
            session.getCurrentLevelItems().addAll(levelState.getItems());
        }

        session.getEnemies().clear();
        if (levelState.getEnemies() != null) {
            session.getEnemies().addAll(levelState.getEnemies());
        }

        if (levelState.getRooms() != null) {
            session.setRooms(new ArrayList<>(levelState.getRooms()));
        }
    }

    private void restoreFogOfWar(GameState gameState) {
        if (gameState.getFogOfWarState() != null && fogOfWarService != null) {
            Set<Position> cells = new HashSet<>(gameState.getFogOfWarState().getExploredCells());
            Set<Room> rooms = new HashSet<>(gameState.getFogOfWarState().getExploredRooms());
            fogOfWarService.restoreExploredCells(cells);
            fogOfWarService.restoreExploredRooms(rooms);
        }
    }

    private void restorePlayer(GameState gameState, GameSession session) {
        GameState.PlayerState playerState = gameState.getPlayerState();

        Player player = new Player(
                new Position(playerState.getPositionX(), playerState.getPositionY()),
                playerState.getEquippedWeapon(),
                playerState.getMaxHealth(),
                playerState.getHealth(),
                playerState.getAgility(),
                playerState.getStrength()
        );

        player.setSleepTurns(playerState.getSleepTurns());

        Inventory inventory = new Inventory();
        if (playerState.getInventoryItems() != null) {
            for (Item item : playerState.getInventoryItems()) {
                inventory.add(item);
            }
        }
        player.setInventory(inventory);

        session.setPlayer(player);
    }

    public boolean hasSaves() {
        return repository.hasSaves();
    }
}