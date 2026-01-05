package org.example;

import org.example.datalayer.AutosaveService;
import org.example.datalayer.SessionStat;
import org.example.datalayer.Statistics;
import org.example.domain.entity.*;
import org.example.domain.service.*;
import org.example.presentation.InputHandler;
import org.example.presentation.JCursesRenderer;
import org.example.presentation.Renderer;

import java.io.IOException;
import java.util.ArrayList;

import static org.example.config.GameConstants.Map.MAP_LEVEL;


public class GameInitializer {
    private GameSession session;
    private Renderer renderer;
    private InputHandler inputHandler;
    private CombatService combatService;
    private EnemyAIService enemyAIService;
    private MovementService movementService;
    private FogOfWarService fogOfWarService;
    private LevelGenerator levelGenerator;

    // Упрощенный конструктор для основного использования
    public GameInitializer() {
//        System.out.println("[GameInitializer] Initializing...");

        this.session = new GameSession();
        this.renderer = new JCursesRenderer();
        this.inputHandler = new InputHandler();
        this.combatService = new CombatService();
        this.enemyAIService = new EnemyAIService();
        this.movementService = new MovementService();
        this.levelGenerator = new LevelGenerator();
        this.fogOfWarService = new FogOfWarService(levelGenerator);

//        System.out.println("[GameInitializer] All components created");
//        System.out.println("[GameInitializer] FogOfWarService: " + (fogOfWarService != null));

        // КРИТИЧЕСКИ ВАЖНО: Устанавливаем fogOfWarService в AutosaveService
        if (this.inputHandler != null) {
            try {
                // Получаем доступ к autosaveService через reflection или сеттер
                java.lang.reflect.Field autosaveField = InputHandler.class.getDeclaredField("autosaveService");
                autosaveField.setAccessible(true);
                AutosaveService autosaveService = (AutosaveService) autosaveField.get(this.inputHandler);

                if (autosaveService != null) {
                    autosaveService.setFogOfWarService(this.fogOfWarService);
//                    System.out.println("[GameInitializer] FogOfWarService set in AutosaveService");
                } else {
//                    System.err.println("[GameInitializer] ERROR: AutosaveService is null!");
                }
            } catch (Exception e) {
                System.err.println("[GameInitializer] ERROR: Failed to set FogOfWarService in AutosaveService: " + e.getMessage());
            }
        }

//        System.out.println("[GameInitializer] Initialization complete\n");
    }

    public void initializeNewGame(SessionStat sessionStat) throws IOException{
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

        // 5. Сбрасываем статистику
        Statistics.resetStatistics(sessionStat);

    }

    // Геттеры для всех компонентов
    public GameSession getSession() { return session; }
    public Renderer getRenderer() { return renderer; }
    public InputHandler getInputHandler() { return inputHandler; }
    public CombatService getCombatService() { return combatService; }
    public EnemyAIService getEnemyAIService() { return enemyAIService; }
    public MovementService getMovementService() { return movementService; }
    public FogOfWarService getFogOfWarService() { return fogOfWarService; }
    public LevelGenerator getLevelGenerator() { return levelGenerator; }


}