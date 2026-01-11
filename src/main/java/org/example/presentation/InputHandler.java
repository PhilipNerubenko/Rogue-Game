package org.example.presentation;

import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.application.input.InputMapper;
import org.example.datalayer.AutosaveService;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.GameSession;
import org.example.domain.enums.ItemType;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.enums.Direction;
import org.example.domain.model.InputCommand;

import static org.example.config.GameConstants.control.*;

/**
 * Обработчик пользовательского ввода для игрового интерфейса.
 * Преобразует нажатия клавиш в игровые команды.
 */
public class InputHandler {

    private final InputMapper inputMapper;

    public InputHandler(InputMapper inputMapper) {
        this.inputMapper = inputMapper;
    }

    public InputCommand readCommand() {
        InputChar inputChar = Toolkit.readCharacter();
        return inputMapper.mapInput(inputChar);
    }
}