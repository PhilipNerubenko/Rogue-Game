package org.example.domain;

import org.example.domain.entity.*;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.input.GameCommandHandler;
import org.example.domain.input.InputCommand;
import org.example.domain.input.ItemSelectionState;
import org.example.domain.interfaces.Renderer;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;

import java.io.IOException;
import java.util.List;

import static org.example.config.GameConstants.TextMessages.MESSAGE_DURATION;

public class GameLoop {

    private final GameSession session;
    private final AutosaveService autosaveService;
    private final StatisticsService statisticsService;
    private final ItemSelectionState itemSelectionState;
    private final GameCommandHandler gameCommandHandler;
    private final Renderer renderer;
    private final MapVisibilityService mapVisibilityService;
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final Message message;
    private final LevelGenerator levelGenerator;
    private final SessionStat currentSessionStat;

    private char[][] asciiMap;
    private boolean running = false;

    public GameLoop(GameInitializer initializer) {
        this.session = initializer.getSession();
        this.currentSessionStat = initializer.getSessionStat();
        this.renderer = initializer.getRenderer();
        this.fogOfWarService = initializer.getFogOfWarService();
        this.autosaveService = initializer.getAutosaveService();
        this.statisticsService = initializer.getStatisticsService();
        this.itemSelectionState = initializer.getInputStateManager();
        this.gameCommandHandler = initializer.getGameInputManager();
        this.message = initializer.getMessage();
        this.mapVisibilityService = new MapVisibilityService(fogOfWarService);
        this.combatService = initializer.getCombatService();
        this.enemyAIService = initializer.getEnemyAIService();
        this.levelGenerator = initializer.getLevelGenerator();
        this.asciiMap = new char[org.example.config.GameConstants.Map.HEIGHT][org.example.config.GameConstants.Map.WIDTH];
    }

    public void start() throws IOException {
        if (session.getCurrentMap() == null || session.getPlayer() == null) {
            generateNewLevel();
        } else {
            initializeLoadedGame();
            fogOfWarService.updateVisibility(getPlayerPosition(), asciiMap);
        }

        gameCommandHandler.bindWorld(session, asciiMap, currentSessionStat);
        running = true;

        while (running) {
            updateTimers();

            if (session.getPlayer().isSleepTurns()) {
                running = gameCommandHandler.handleSleepTurn();
                continue;
            }

            renderer.renderWorld(session, asciiMap, mapVisibilityService, fogOfWarService, itemSelectionState, message);

            InputCommand command = gameCommandHandler.processInput();

            if (command.getType() == InputCommand.Type.QUIT) {
                autosaveService.saveGame(session, currentSessionStat);
                running = false;
                continue;
            }

            processCommand(command);
            updateWorldState();

            if (session.getPlayer().getHealth() <= 0) {
                gameCommandHandler.handleDeath();
                running = false;
            }
        }
        renderer.shutdown();
    }

    private void updateTimers() {
        if (message.getMessageTimer() > 0) {
            message.setMessageTimer(message.getMessageTimer() - 1);
        } else {
            message.resetMessage();
        }
    }

    private void processCommand(InputCommand command) throws IOException {
        switch (command.getType()) {
            case MOVE -> {
                if (gameCommandHandler.handleMovement(command.getDirection())) {
                    generateNewLevel();
                }
            }
            case USE_ITEM -> gameCommandHandler.handleUseItem(command.getItemType());
            case SELECT_INDEX -> gameCommandHandler.handleItemSelection(command.getSelectedIndex());
            case UNEQUIP_WEAPON -> gameCommandHandler.handleUnequipWeapon();
        }
    }

    private void updateWorldState() {
        fogOfWarService.updateVisibility(getPlayerPosition(), asciiMap);

        Position pos = getPlayerPosition();
        List<String> enemyMessages = enemyAIService.witchMoveEnemiesPattern(
                session, combatService, pos.getX(), pos.getY(), asciiMap);

        if (!enemyMessages.isEmpty()) {
            message.setActiveMessageLine2(String.join(", ", enemyMessages));
            message.setMessageTimer(MESSAGE_DURATION);
        }
    }

    private void initializeLoadedGame() {
        this.asciiMap = session.getCurrentMap();
        message.setActiveMessageLine1("Loaded game - Level " + session.getLevelNum());
        message.setMessageTimer(MESSAGE_DURATION);
    }

    private void generateNewLevel() throws IOException {
        int levelToGenerate = (session.getCurrentMap() == null) ? session.getLevelNum() : session.getLevelNum() + 1;

        if (levelToGenerate > 21) {
            gameCommandHandler.handleVictory();
            running = false;
            return;
        }

        if (session.getCurrentMap() != null) session.setLevelNum(levelToGenerate);

        asciiMap = levelGenerator.createAsciiMap(levelToGenerate);
        session.setCurrentMap(asciiMap);
        session.setRooms(levelGenerator.getRooms());
        session.getCurrentLevelItems().clear();
        session.getCurrentLevelItems().addAll(levelGenerator.getItems());

        gameCommandHandler.bindWorld(session, asciiMap, currentSessionStat);

        for (Room room : levelGenerator.getRooms()) {
            if (room.isStartRoom()) {
                getPlayerPosition().setX(room.getX1() + 2);
                getPlayerPosition().setY(room.getY1() + 2);
                break;
            }
        }

        session.getEnemies().clear();
        session.getEnemies().addAll(levelGenerator.generateEnemiesForSession(session, asciiMap));

        fogOfWarService.reset();
        fogOfWarService.updateVisibility(getPlayerPosition(), asciiMap);

        if (levelToGenerate > 1) {
            message.setActiveMessageLine1("You have gone deeper...");
            statisticsService.incrementLevel(currentSessionStat);
        }
        message.setMessageTimer(MESSAGE_DURATION);
    }

    private Position getPlayerPosition() {
        return session.getPlayer().getPosition();
    }
}