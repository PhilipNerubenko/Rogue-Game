package org.example;

import org.example.domain.GameApplication;
import org.example.datalayer.AutosaveRepository;
import org.example.datalayer.SessionStatRepository;
import org.example.domain.interfaces.IAutosaveRepository;
import org.example.domain.interfaces.ISessionStatRepository;
import org.example.domain.interfaces.Renderer;
import org.example.presentation.views.JCursesRenderer;

public class App {

    public static void main(String[] args) {
        Renderer renderer = new JCursesRenderer();
        ISessionStatRepository sessionStatRepository = new SessionStatRepository();
        IAutosaveRepository autosaveRepository = new AutosaveRepository();
        new GameApplication(renderer, sessionStatRepository, autosaveRepository).run();
    }
}