package org.example.domain;

import org.example.domain.input.GameCommandHandler;
import org.example.domain.interfaces.IAutosaveRepository;
import org.example.domain.interfaces.ISessionStatRepository;
import org.example.domain.service.AutosaveService;
import org.example.domain.entity.SessionStat;
import org.example.domain.service.StatisticsService;
import org.example.domain.entity.*;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.service.*;
import org.example.domain.interfaces.Renderer;
import org.example.domain.input.ItemSelectionState;

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
    private final ItemSelectionState itemSelectionState;
    private final GameCommandHandler gameCommandHandler;
    private final SessionStat sessionStat;

    // Сервисы игровых механик
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;
    private final AutosaveService autosaveService;
    private final StatisticsService statisticsService;
    private final Message message;

    /**
     * Конструктор по умолчанию. Инициализирует все компоненты игры.
     * Выполняет настройку зависимостей между сервисами.
     */
    public GameInitializer(SessionStat sessionStat, Renderer renderer, ISessionStatRepository sessionStatRepository, IAutosaveRepository autosaveRepository) {
        this.sessionStat = sessionStat;

        // domain
        this.session = new GameSession();
        this.combatService = new CombatService();
        this.enemyAIService = new EnemyAIService();
        this.levelGenerator = new LevelGenerator();
        this.fogOfWarService = new FogOfWarService(levelGenerator);
        this.statisticsService = new StatisticsService(sessionStatRepository);
        this.itemSelectionState = new ItemSelectionState();
        this.message = new Message();
        this.gameCommandHandler = new GameCommandHandler(renderer, itemSelectionState, combatService,
                enemyAIService, statisticsService, fogOfWarService, message);

        // application
        this.autosaveService = new AutosaveService(autosaveRepository, fogOfWarService);

        // presentation
        this.renderer = renderer; // Реализация рендерера на основе JCurses
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
        statisticsService.reset(sessionStat);
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
    public AutosaveService getAutosaveService() {
        return autosaveService;
    }

    public ItemSelectionState getInputStateManager() {
        return itemSelectionState;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public SessionStat getSessionStat() { return sessionStat; }

    public GameCommandHandler getGameInputManager() {
        return gameCommandHandler;
    }

    public Message getMessage() {
        return message;
    }
}