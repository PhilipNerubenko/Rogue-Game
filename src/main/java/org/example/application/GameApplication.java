package org.example.application;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.application.service.GameLoadService;
import org.example.config.GameConstants;
import org.example.datalayer.SessionStat;
import org.example.presentation.GameLoop;
import org.example.presentation.JCursesRenderer;
import org.example.presentation.MainMenuController;
import org.example.presentation.MenuAction;
import sun.misc.Signal;

import java.io.IOException;

import static org.example.config.GameConstants.ScreenConfig.HIDE_CURSOR;
import static org.example.config.GameConstants.ScreenConfig.SHOW_CURSOR;
import static org.example.config.GameConstants.TextMessages.TERMINATE;

public class GameApplication {

    private JCursesRenderer renderer;
    private final SessionStat sessionStat = new SessionStat();

    public void run() {
        initPresentation();

        try {
            renderer = new JCursesRenderer();

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
            shutdown();
        }
    }

    private void startNewGame() throws IOException {
        sessionStat.reset();

        GameInitializer initializer = new GameInitializer(sessionStat);
        initializer.initializeNewGame();

        GameLoop currentGameLoop = new GameLoop(initializer);
        currentGameLoop.start();
    }

    private void loadGame() {
        GameInitializer initializer = new GameInitializer(sessionStat);
        GameLoadService loader = new GameLoadService();

        if (!loader.load(initializer)) {
            renderer.drawString(10, 10, "No saved games found!", CharColor.RED);
            Toolkit.readCharacter();
            return;
        }

        try {
            new GameLoop(initializer).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPresentation() {
        Signal.handle(new Signal("INT"), signal -> { Toolkit.shutdown(); System.out.println(TERMINATE); System.exit(0); });

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

    private void shutdown() {
        if (renderer != null) {
            renderer.shutdown();
        } else {
            Toolkit.shutdown();
        }
        System.out.print(SHOW_CURSOR);
    }


}
