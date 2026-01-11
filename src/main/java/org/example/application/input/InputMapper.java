package org.example.application.input;

import jcurses.system.InputChar;
import org.example.domain.model.InputCommand;

public interface InputMapper {
    InputCommand mapInput(InputChar inputChar);
}
