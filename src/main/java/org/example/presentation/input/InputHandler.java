package org.example.presentation.input;

import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.presentation.dto.InputCommand;

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