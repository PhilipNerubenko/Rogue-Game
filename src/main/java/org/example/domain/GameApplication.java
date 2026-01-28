package org.example.domain;

import org.example.config.GameConstants;
import org.example.domain.controller.LoadMenuController;
import org.example.domain.controller.MainMenuController;
import org.example.domain.entity.SessionStat;
import org.example.domain.enums.MenuAction;
import org.example.domain.interfaces.IAutosaveRepository;
import org.example.domain.interfaces.ISessionStatRepository;
import org.example.domain.interfaces.Renderer;
import sun.misc.Signal;

import java.io.IOException;
import java.util.List;

import static org.example.config.GameConstants.ScreenConfig.HIDE_CURSOR;
import static org.example.config.GameConstants.ScreenConfig.SIGINT_STRING;
import static org.example.config.GameConstants.TextMessages.TERMINATE;

public class GameApplication {

    private final Renderer renderer;
    private final SessionStat sessionStat;
    private final ISessionStatRepository sessionStatRepository;
    private final IAutosaveRepository autosaveRepository;

    public GameApplication(Renderer renderer, ISessionStatRepository sessionStatRepository, IAutosaveRepository autosaveRepository1) {
        this.renderer = renderer;
        this.sessionStatRepository = sessionStatRepository;
        this.autosaveRepository = autosaveRepository1;
        this.sessionStat = new SessionStat();
    }

    public void run() throws IOException {
        initTerminal();

        try {

            MainMenuController menu = new MainMenuController(renderer);
            boolean running = true;

            while (running) {
                MenuAction action = menu.show();

                switch (action) {
                    case NEW_GAME -> startNewGame();
                    case LOAD_GAME -> loadGame();
                    case SCOREBOARD -> {
                        List<SessionStat> stats = sessionStatRepository.getAllStats();
                        renderer.drawScoreboard(stats);
                    }
                    case EXIT -> running = false;
                }
            }
        } finally {
            renderer.shutdown();
        }
    }

    private void startNewGame() throws IOException {
        sessionStat.reset();

        GameInitializer initializer = new GameInitializer(sessionStat, renderer, sessionStatRepository, autosaveRepository);
        initializer.initializeNewGame();

        GameLoop currentGameLoop = new GameLoop(initializer);
        currentGameLoop.start();
    }

    private void loadGame() {
        // Создаем инициализатор (он же содержит все необходимые зависимости)
        GameInitializer initializer = new GameInitializer(
                sessionStat,
                renderer,
                sessionStatRepository,
                autosaveRepository
        );

        // Создаем контроллер меню загрузки
        LoadMenuController loadMenu = new LoadMenuController(
                renderer,
                initializer.getAutosaveService(),
                initializer.getSession(),
                initializer.getSessionStat(),
                initializer.getLevelGenerator()
        );

        // Показываем меню и получаем выбранное действие
        MenuAction action = loadMenu.show();

        // Если выбрана загрузка игры (а не выход из меню)
        if (action == MenuAction.LOAD_GAME) {
            // Обновляем туман войны для загруженной игры
            var fog = initializer.getFogOfWarService();
            var pos = initializer.getSession().getPlayer().getPosition();
            fog.updateForLoadedGame(pos, initializer.getSession().getCurrentMap());

            // Запускаем игровой цикл
            try {
                new GameLoop(initializer).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initTerminal() {
        handleShutdownSignal();
        System.out.print(HIDE_CURSOR);
        System.out.printf("\033[8;%d;%dt",
                GameConstants.Map.TERMINAL_HEIGHT,
                GameConstants.Map.TERMINAL_WIDTH
        );
        System.out.flush();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
    }

    private void handleShutdownSignal() {
        Signal.handle(new Signal(SIGINT_STRING), signal -> { renderer.shutdown(); System.out.println(TERMINATE); System.exit(0); });
    }

}
