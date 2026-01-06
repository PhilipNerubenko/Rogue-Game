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

/**
 * Класс-инициализатор игры. Отвечает за создание и настройку всех компонентов игровой сессии.
 * Следует паттерну "Фабрика" или "Строитель" для инициализации сложного объекта GameSession.
 */
public class GameInitializer {
    // Основные компоненты игровой сессии
    private final GameSession session;
    private final Renderer renderer;
    private final InputHandler inputHandler;

    // Сервисы игровых механик
    private final CombatService combatService;
    private final EnemyAIService enemyAIService;
    private final FogOfWarService fogOfWarService;
    private final LevelGenerator levelGenerator;

    /**
     * Конструктор по умолчанию. Инициализирует все компоненты игры.
     * Выполняет настройку зависимостей между сервисами.
     */
    public GameInitializer() {
        // Создание основных объектов игры
        this.session = new GameSession();
        this.renderer = new JCursesRenderer(); // Реализация рендерера на основе JCurses
        this.inputHandler = new InputHandler();

        // Инициализация игровых сервисов
        this.combatService = new CombatService();
        this.enemyAIService = new EnemyAIService();
        this.levelGenerator = new LevelGenerator();

        // FogOfWarService зависит от LevelGenerator для работы с картой
        this.fogOfWarService = new FogOfWarService(levelGenerator);

        // Настройка дополнительных сервисов (автосохранение)
        configureAutosaveService();
    }

    /**
     * Настраивает AutosaveService с помощью рефлексии.
     * Приватный метод, который инъектирует зависимость FogOfWarService в AutosaveService.
     * Использует рефлексию для доступа к приватному полю, что может быть необходимо
     * для интеграции с существующей архитектурой.
     */
    private void configureAutosaveService() {
        try {
            // Получаем доступ к приватному полю autosaveService в классе InputHandler
            var autosaveField = InputHandler.class.getDeclaredField("autosaveService");
            autosaveField.setAccessible(true); // Разрешаем доступ к приватному полю

            // Получаем экземпляр AutosaveService из InputHandler
            var autosaveService = (AutosaveService) autosaveField.get(inputHandler);

            // Если сервис существует, настраиваем его
            if (autosaveService != null) {
                autosaveService.setFogOfWarService(fogOfWarService);
            }
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение, так как автосохранение - вторичная функция
            System.err.println("Failed to configure AutosaveService: " + e.getMessage());
        }
    }

    /**
     * Инициализирует новую игровую сессию с нуля.
     * Сбрасывает все состояния и создает нового игрока.
     *
     * @param sessionStat объект для хранения статистики сессии
     * @throws IOException если возникли ошибки ввода-вывода при инициализации
     */
    public void initializeNewGame(SessionStat sessionStat) throws IOException {
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
}