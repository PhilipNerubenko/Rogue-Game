package org.example.application;

import org.example.application.usecase.AutosaveGameUseCase;
import org.example.application.input.GameInputMapper;
import org.example.application.input.InputMapper;
import org.example.application.input.InputStateManager;
import org.example.datalayer.AutosaveService;
import org.example.datalayer.SessionStat;
import org.example.datalayer.Statistics;
import org.example.domain.entity.*;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.service.*;
import org.example.presentation.InputHandler;
import org.example.presentation.JCursesRenderer;
import org.example.presentation.Renderer;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Класс-инициализатор игры. Отвечает за создание и настройку всех компонентов игровой сессии.
 * Следует паттерну "Фабрика" или "Строитель" для инициализации сложного объекта GameSession.
 */
public class GameInitializer {
    // Основные компоненты игровой сессии
    private final GameSession session;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final InputStateManager inputStateManager;
    private final SessionStat sessionStat;

    // Сервисы игровых механик
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private final SaveGameUseCase saveGameUseCase;

    /**
     * Конструктор по умолчанию. Инициализирует все компоненты игры.
     * Выполняет настройку зависимостей между сервисами.
     */
    public GameInitializer(SessionStat sessionStat) {
        this.sessionStat = sessionStat;

        // domain
        this.session = new GameSession();
        this.combatService = new CombatService();
        this.enemyAIService = new EnemyAIService();
        this.levelGenerator = new LevelGenerator();
        this.fogOfWarService = new FogOfWarService(levelGenerator);

        // datalayer
        AutosaveService autosaveService = new AutosaveService();
        autosaveService.setFogOfWarService(fogOfWarService); // Установка зависимости FogOfWarService

        // application
        this.inputStateManager = new InputStateManager();
        this.saveGameUseCase = new AutosaveGameUseCase(autosaveService, levelGenerator);
        InputMapper inputMapper = new GameInputMapper(inputStateManager);

        // presentation
        this.inputHandler = new InputHandler(inputMapper);
        this.renderer = new JCursesRenderer(); // Реализация рендерера на основе JCurses
    }

    /**
     * Инициализирует новую игровую сессию с нуля.
     * Сбрасывает все состояния и создает нового игрока.
     *
     * @throws IOException если возникли ошибки ввода-вывода при инициализации
     */
    public void initializeNewGame() throws IOException {
        // Создание и настройка игрока
        session.setPlayer(new Player());

        // Установка начального уровня
        session.setLevelNum(1);

        // Инициализация пустых списков врагов и предметов
        session.setEnemies(new ArrayList<>());
        session.setCurrentLevelItems(new ArrayList<>());

        // Сброс текущей карты (будет сгенерирована позже)
        session.setCurrentMap(null);

        // Сброс статистики до начальных значений
        Statistics.resetStatistics(sessionStat);
    }

    // ==================== ГЕТТЕРЫ ====================
    // Предоставляют доступ к компонентам игры извне

    /**
     * @return текущая игровая сессия
     */
    public GameSession getSession() { return session; }

    /**
     * @return рендерер для отображения игры
     */
    public Renderer getRenderer() { return renderer; }

    /**
     * @return обработчик пользовательского ввода
     */
    public InputHandler getInputHandler() { return inputHandler; }

    /**
     * @return сервис боевой системы
     */
    public CombatService getCombatService() { return combatService; }

    /**
     * @return ИИ для управления врагами
     */
    public EnemyAIService getEnemyAIService() { return enemyAIService; }

    /**
     * @return сервис "тумана войны" (ограниченной видимости)
     */
    public FogOfWarService getFogOfWarService() { return fogOfWarService; }

    /**
     * @return генератор уровней/карт
     */
    public LevelGenerator getLevelGenerator() { return levelGenerator; }
    public SaveGameUseCase getSaveGameUseCase() { return saveGameUseCase; }
    public SessionStat getSessionStat() { return sessionStat; }

    public InputStateManager getInputStateManager() {
        return inputStateManager;
    }
}