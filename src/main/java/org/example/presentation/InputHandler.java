package org.example.presentation;

import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.entity.ItemType;
/**
 * Обработчик нажатия клавиш
 * - WASD: движение
 * - hjke: использование предметов (показать список)
 * - 0-9: выбор предмета
 * - ESC/q: выход
 */
public class InputHandler {


        // ========== Состояние ожидания выбора предмета ==========
        private boolean awaitingSelection = false;
        private ItemType pendingItemType = null;

        /**
         * Читает и разбирает следующую команду от игрока.
         * Вызов блокирует, пока не будет нажата клавиша.
         */
        public InputCommand readCommand() {
            InputChar ch = Toolkit.readCharacter();

            // Если ожидаем выбор предмета
            if (awaitingSelection) {
                return handleItemSelection(ch);
            }

            return handleNormalInput(ch);
        }

    private InputCommand handleNormalInput(InputChar ch) {
        char character;
        try {
            character = ch.getCharacter();
        } catch (RuntimeException e) {
            // Спец-клавиша (стрелка и т.д.)
            return InputCommand.none();
        }

        // Код клавиши для ESC
        if (ch.getCode() == 27) {
            return InputCommand.quit();
        }

        if (character == 0) {
            return InputCommand.none();
        }

        // Движение
        switch (Character.toLowerCase(character)) {
            case 'w': {
                System.out.println("Motion detected NORTH"); // Отладка
                return InputCommand.move(Direction.NORTH);
            }
            case 's': {
                System.out.println("Motion detected SOUTH"); // Отладка
                return InputCommand.move(Direction.SOUTH);
            }
            case 'a': {
                System.out.println("Motion detected WEST"); // Отладка
                return InputCommand.move(Direction.WEST);
            }
            case 'd': {
                System.out.println("Motion detected EAST"); // Отладка
                return InputCommand.move(Direction.EAST);
            }
        }

        return InputCommand.none();
    }

        /**
         * Безопасное чтение кода клавиши.
         */
        private int readKeyCode() {
            try {
                return Toolkit.readCharacter().getCode();
            } catch (Exception e) {
                return -1; // Ошибка ввода
            }
        }

    private InputCommand handleItemSelection(InputChar ch) {
        char character;
        try {
            character = ch.getCharacter();
        } catch (RuntimeException e) {
            return InputCommand.none();
        }

        if (character >= '1' && character <= '9') {
            int index = character - '1';
            resetAwaitingState();
            return InputCommand.selectIndex(index);
        }

        if (ch.getCode() == 27) { // ESC отмена
            System.out.println("Discovered ESC (kod 27)");
            resetAwaitingState();
            return InputCommand.none();
        }

        return InputCommand.none();
    }

    private void resetAwaitingState() {
        awaitingSelection = false;
        pendingItemType = null;
    }

    public boolean isAwaitingSelection() { return awaitingSelection; }
    public ItemType getPendingItemType() { return pendingItemType; }
}
