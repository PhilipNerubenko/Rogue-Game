package org.example.presentation;

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
//        public InputCommand readCommand() {
//            int keyCode = readKeyCode();

//            // Если ожидаем выбор предмета (после h/j/k/e)
//            if (awaitingSelection) {
//                return handleItemSelection(keyCode);
//            }

//            // Обычный ввод
//            return handleNormalInput(keyCode);
//        }

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

//        /**
//         * Обработка обычных команд (движение, вызов списка предметов, выход).
//         */
//        private InputCommand handleNormalInput(int keyCode) {
//            // --- ДВИЖЕНИЕ WASD ---
//            switch (keyCode) {
//                case 'w': case 'W':
//                    return InputCommand.move(Direction.NORTH);
//                case 's': case 'S':
//                    return InputCommand.move(Direction.SOUTH);
//                case 'a': case 'A':
//                    return InputCommand.move(Direction.WEST);
//                case 'd': case 'D':
//                    return InputCommand.move(Direction.EAST);
//            }

//            // --- ИСПОЛЬЗОВАНИЕ ПРЕДМЕТОВ hjke ---
//            switch (keyCode) {
//                case 'h': case 'H':
//                    awaitingSelection = true;
//                    pendingItemType = ItemType.WEAPON;
//                    return InputCommand.useItem(ItemType.WEAPON);
//
//                case 'j': case 'J':
//                    awaitingSelection = true;
//                    pendingItemType = ItemType.FOOD;
//                    return InputCommand.useItem(ItemType.FOOD);
//
//                case 'k': case 'K':
//                    awaitingSelection = true;
//                    pendingItemType = ItemType.ELIXIR;
//                    return InputCommand.useItem(ItemType.ELIXIR);
//
//                case 'e': case 'E':
//                    awaitingSelection = true;
//                    pendingItemType = ItemType.SCROLL;
//                    return InputCommand.useItem(ItemType.SCROLL);
//            }

//            // --- ВЫХОД ---
//            switch (keyCode) {
//                case 27: // ESC
//                case 'q': case 'Q':
//                    return InputCommand.quit();
//            }
//
//            // Неизвестная клавиша
//            return InputCommand.none();
//        }

//        /**
//         * Обработка выбора предмета (цифры 0-9).
//         */
//        private InputCommand handleItemSelection(int keyCode) {
//            // Цифры 1-9 (индекс 0-8)
//            if (keyCode >= '1' && keyCode <= '9') {
//                int index = keyCode - '1';
//                resetAwaitingState();
//                return InputCommand.selectIndex(index);
//            }
//
//            // Цифра 0 (только для оружия - убрать из рук)
//            if (keyCode == '0' && pendingItemType == ItemType.WEAPON) {
//                resetAwaitingState();
//                return InputCommand.selectIndex(0); // 0 = убрать оружие
//            }
//
//            // ESC отменяет выбор
//            if (keyCode == 27) {
//                resetAwaitingState();
//                return InputCommand.none();
//            }

//            // Любая другая клавиша - игнорируем
//            return InputCommand.none();
//        }

        /**
         * Сбросить состояние ожидания выбора.
         */
        private void resetAwaitingState() {
            awaitingSelection = false;
            pendingItemType = null;
        }

        // ========== Геттеры состояния (для UI) ==========

        public boolean isAwaitingSelection() {
            return awaitingSelection;
        }

        public ItemType getPendingItemType() {
            return pendingItemType;
        }
}
