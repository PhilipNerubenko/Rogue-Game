package org.example;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.datalayer.Statistics;
import org.example.domain.entity.*;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.presentation.InputHandler;
import org.example.presentation.JCursesRenderer;
import org.example.presentation.Renderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.example.config.GameConstants.Icons.*;
import static org.example.config.GameConstants.Map.MAP_LEVEL;
import static org.example.config.GameConstants.Map.MAP_OFFSET_X;
import static org.example.config.GameConstants.ProbabilitiesAndBalance.*;
import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;

public class GameInitializer {
    private GameSession session;
    private Renderer renderer;
    private InputHandler inputHandler;
    private CombatService combatService;
    private EnemyAIService enemyAIService;
    private MovementService movementService;
    private InventoryService inventoryService;
    private FogOfWarService fogOfWarService;
    private LevelGenerator levelGenerator;

    // Упрощенный конструктор для основного использования
    public GameInitializer() {
        this.session = new GameSession();
        // 2. Создаем сервисы (для GameLoop)
        this.renderer = new JCursesRenderer();
        this.inputHandler = new InputHandler();
        this.combatService = new CombatService();
        this.enemyAIService = new EnemyAIService();
        this.movementService = new MovementService();
        this.inventoryService = new InventoryService();
        // 3. LevelGenerator создаем без сессии
        this.levelGenerator = new LevelGenerator();
        // 4. FogOfWarService
        this.fogOfWarService = new FogOfWarService(levelGenerator);


    }

    public void initializeNewGame() throws IOException{
        // 1. Создаем игрока (позиция игрока будет записана в в GameLoop)
        Player player =  new Player();
        session.setPlayer(player);

        // 2. Устанавливаем начальный уровень = 1
        session.setLevelNum(1);

        // 3. Инициализируем пустые списки
        session.setEnemies(new ArrayList<>());
        session.setCurrentLevelItems(new ArrayList<>());

        // 4. Карта будет создана позже в GameLoop
        session.setCurrentMap(null);

    }

    // Геттеры для всех компонентов
    public GameSession getSession() { return session; }
    public Renderer getRenderer() { return renderer; }
    public InputHandler getInputHandler() { return inputHandler; }
    public CombatService getCombatService() { return combatService; }
    public EnemyAIService getEnemyAIService() { return enemyAIService; }
    public MovementService getMovementService() { return movementService; }
    public InventoryService getInventoryService() { return inventoryService; }
    public FogOfWarService getFogOfWarService() { return fogOfWarService; }
    public LevelGenerator getLevelGenerator() { return levelGenerator; }

    public char[][] getAsciiMap() {
        // Возвращаем карту из сессии, а не создаем новую
        if (session.getCurrentMap() != null) {
            return session.getCurrentMap();
        }
        // Fallback на случай, если карта еще не создана
        return levelGenerator.createAsciiMap(MAP_LEVEL);
    }
}