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

        character = Character.toLowerCase(character);

        // Движение и выбор предмета
        switch (character) {
            case 'w': {
                return InputCommand.move(Direction.NORTH);
            }
            case 's': {
                return InputCommand.move(Direction.SOUTH);
            }
            case 'a': {
                return InputCommand.move(Direction.WEST);
            }
            case 'd': {
                return InputCommand.move(Direction.EAST);
            }
            case 'h':
                return InputCommand.useItem(ItemType.WEAPON);
            case 'j':
                return InputCommand.useItem(ItemType.FOOD);
            case 'k':
                return InputCommand.useItem(ItemType.ELIXIR);
            case 'e':
                return InputCommand.useItem(ItemType.SCROLL);
            case 'q': // Убрать оружие (unequip)
                return InputCommand.unequipWeapon();
        }

        return InputCommand.none();
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
            return InputCommand.selectIndex(index);
        }

        if (ch.getCode() == 27) { // ESC отмена
            resetAwaitingState();
            return InputCommand.none();
        }

        return InputCommand.none();
    }

    /**
     * Установить состояние ожидания выбора предмета
     */
    public void setAwaitingSelection(boolean awaiting, ItemType itemType) {
        this.awaitingSelection = awaiting;
        this.pendingItemType = awaiting ? itemType : null;
    }

    public void resetAwaitingState() {
        awaitingSelection = false;
        pendingItemType = null;
    }

    public boolean isAwaitingSelection() { return awaitingSelection; }
    public ItemType getPendingItemType() { return pendingItemType; }
}
