package org.example.domain;

import org.example.config.GameConstants;
import org.example.domain.controller.MainMenuController;
import org.example.domain.entity.SessionStat;
import org.example.domain.enums.MenuAction;
import org.example.domain.interfaces.IAutosaveRepository;
import org.example.domain.interfaces.ISessionStatRepository;
import org.example.domain.interfaces.Renderer;
import sun.misc.Signal;

import java.io.IOException;

import static org.example.config.GameConstants.Colors.COLOR_RED;
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

    public void run() {
        initTerminal();

        try {

            MainMenuController menu = new MainMenuController(renderer);
            boolean running = true;

            while (running) {
                MenuAction action = menu.show();

                switch (action) {
                    case NEW_GAME -> startNewGame();
                    case LOAD_GAME -> loadGame();
                    case SCOREBOARD -> renderer.drawScoreboard();
                    case EXIT -> running = false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        GameInitializer initializer = new GameInitializer(sessionStat, renderer, sessionStatRepository, autosaveRepository);

        // Проверяем наличие сохранений
        if (!initializer.getAutosaveService().hasSaves()) {
            renderer.drawString(10, 10, "No saved games found!", COLOR_RED);
            renderer.readCharacter();
            return;
        }

        // Загружаем игру
        boolean loaded = initializer.getAutosaveService().loadAndRestoreGame(
                initializer.getSession(),
                initializer.getSessionStat(),
                initializer.getLevelGenerator()
        );

        if (!loaded) {
            renderer.drawString(10, 10, "Failed to load game!", COLOR_RED);
            renderer.readCharacter();
            return;
        }

        // Обновляем туман войны
        var fog = initializer.getFogOfWarService();
        var pos = initializer.getSession().getPlayer().getPosition();
        fog.updateForLoadedGame(pos, initializer.getSession().getCurrentMap());

        try {
            new GameLoop(initializer).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
