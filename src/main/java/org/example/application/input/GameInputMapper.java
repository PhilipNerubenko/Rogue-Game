package org.example.application.input;

import jcurses.system.InputChar;
import org.example.domain.enums.Direction;
import org.example.domain.enums.ItemType;
import org.example.domain.model.InputCommand;

import static org.example.config.GameConstants.control.*;

public class GameInputMapper implements InputMapper {
    private final InputStateManager stateManager;

    public GameInputMapper(InputStateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public InputCommand mapInput(InputChar inputChar) {
        int keyCode = inputChar.getCode();
        char keyChar = inputChar.getCharacter();

        // ESC - специальная обработка
        if (keyCode == ESC_KEY_CODE) {
            return handleEscape();
        }

        // Ожидание выбора предмета
        if (stateManager.isAwaitingSelection()) {
            return handleSelection(keyChar);
        }

        // Обычный ввод
        return handleMovementOrAction(keyChar);
    }

    private InputCommand handleEscape() {
        stateManager.resetAwaitingState();
        return InputCommand.quit();
    }

    private InputCommand handleSelection(char keyChar) {
        if (keyChar >= '0' && keyChar <= '9') {
            int index = Character.getNumericValue(keyChar);
            return InputCommand.selectIndex(index);
        }
        return InputCommand.none();
    }

    private InputCommand handleMovementOrAction(char keyChar) {
        // Движение (WASD или стрелки)
        if (keyChar == 'W' || keyChar == 'w') {
            return InputCommand.move(Direction.NORTH);
        }
        if (keyChar == 'S' || keyChar == 's') {
            return InputCommand.move(Direction.SOUTH);
        }
        if (keyChar == 'A' || keyChar == 'a') {
            return InputCommand.move(Direction.WEST);
        }
        if (keyChar == 'D' || keyChar == 'd') {
            return InputCommand.move(Direction.EAST);
        }

        // Использование предметов (горячие клавиши)
        if (keyChar == 'h' || keyChar == 'H') {
            return InputCommand.useItem(ItemType.WEAPON);
        }
        if (keyChar == 'j' || keyChar == 'J') {
            return InputCommand.useItem(ItemType.FOOD);
        }
        if (keyChar == 'k' || keyChar == 'K') {
            return InputCommand.useItem(ItemType.ELIXIR);
        }
        if (keyChar == 'e' || keyChar == 'E') {
            return InputCommand.useItem(ItemType.SCROLL);
        }

        // Снятие оружия
        if (keyChar == 'q' || keyChar == 'Q') {
            return InputCommand.unequipWeapon();
        }

        // Пропуск хода (пробел или точка)
        if (keyChar == '.' || keyChar == ' ') {
            return InputCommand.none();
        }

        // Неизвестная команда
        return InputCommand.none();
    }
}
