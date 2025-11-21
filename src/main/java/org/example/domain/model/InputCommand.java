package org.example.domain.model;

import org.example.domain.entity.ItemType;

public class InputCommand {
//    public final class InputCommand {
//
//        /**
//         * Тип команды по ТЗ:
//         * - MOVE: WASD — перемещение персонажа
//         * - USE_ITEM: hjke — показать список предметов
//         * - SELECT_INDEX: 0-9 — выбрать конкретный предмет
//         * - QUIT: ESC/q — выйти из игры
//         * - NONE: неизвестная клавиша
//         */
//        public enum Type {
//            MOVE, USE_ITEM, SELECT_INDEX, QUIT, NONE
//        }
//
//        // Поля (final = immutable)
//        private final Type type;
//        private final Direction direction;   // для MOVE
//        private final ItemType itemType;     // для USE_ITEM
//        private final int selectedIndex;     // для SELECT_INDEX (0-9)
//
//        /**
//         * Приватный конструктор. Используйте фабричные методы!
//         */
//        private InputCommand(Type type, Direction direction, ItemType itemType, int selectedIndex) {
//            this.type = type;
//            this.direction = direction;
//            this.itemType = itemType;
//            this.selectedIndex = selectedIndex;
//        }

        // ========== Фабричные методы (static) ==========

//        /** Команда движения: WASD */
//        public static InputCommand move(Direction direction) {
//            return new InputCommand(Type.MOVE, direction, null, -1);
//        }
//
//        /** Команка "использовать предмет": hjke */
//        public static InputCommand useItem(ItemType itemType) {
//            return new InputCommand(Type.USE_ITEM, null, itemType, -1);
//        }
//
//        /** Команда выбора предмета: 0-9 */
//        public static InputCommand selectIndex(int index) {
//            if (index < 0 || index > 9) {
//                throw new IllegalArgumentException("Индекс должен быть 0-9, получен: " + index);
//            }
//            return new InputCommand(Type.SELECT_INDEX, null, null, index);
//        }
//
//        /** Команда выхода: ESC, q */
//        public static InputCommand quit() {
//            return new InputCommand(Type.QUIT, null, null, -1);
//        }
//
//        /** Пустая команда (неизвестная клавиша) */
//        public static InputCommand none() {
//            return new InputCommand(Type.NONE, null, null, -1);
//        }

//        // ========== Getters (только чтение) ==========
//
//        /** Тип команды */
//        public Type getType() {
//            return type;
//        }
//
//        /** Направление движения (для MOVE) */
//        public Direction getDirection() {
//            if (type != Type.MOVE) {
//                throw new IllegalStateException("Команда не для движения");
//            }
//            return direction;
//        }
//
//        /** Тип предмета (для USE_ITEM) */
//        public ItemType getItemType() {
//            if (type != Type.USE_ITEM) {
//                throw new IllegalStateException("Команда не для использования предмета");
//            }
//            return itemType;
//        }
//
//        /** Выбранный индекс (для SELECT_INDEX) */
//        public int getSelectedIndex() {
//            if (type != Type.SELECT_INDEX) {
//                throw new IllegalStateException("Команда не для выбора индекса");
//            }
//            return selectedIndex;
//        }
//
//        // ========== Удобные проверки ==========
//
//        /** Это команда движения? */
//        public boolean isMove() {
//            return type == Type.MOVE;
//        }
//
//        /** Это команда выхода? */
//        public boolean isQuit() {
//            return type == Type.QUIT;
//        }
//
//        // ========== toString и equals (опционально) ==========
//
//        @Override
//        public String toString() {
//            return "InputCommand{" +
//                    "type=" + type +
//                    (direction != null ? ", direction=" + direction : "") +
//                    (itemType != null ? ", itemType=" + itemType : "") +
//                    (selectedIndex >= 0 ? ", index=" + selectedIndex : "") +
//                    '}';
//        }
//
//        // equals и hashCode можно не переопределять, т.к. это data carrier
//    }
}
