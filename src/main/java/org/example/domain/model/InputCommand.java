package org.example.domain.model;

import org.example.domain.enums.Direction;
import org.example.domain.enums.ItemType;

/**
 * Команда ввода от пользователя.
 * Представляет различные действия, которые может выполнить игрок.
 * Использует паттерн "Статический фабричный метод" для создания команд.
 */
public class InputCommand {

    /**
     * Тип команды.
     */
    public enum Type {
        NONE,           // Нет команды
        MOVE,           // Перемещение
        USE_ITEM,       // Использование предмета
        SELECT_INDEX,   // Выбор индекса
        UNEQUIP_WEAPON, // Снятие оружия
        QUIT            // Выход из игры
    }

    // Поля класса объявлены как final для обеспечения неизменяемости
    private final Type type;
    private final Direction direction;
    private final ItemType itemType;
    private final int selectedIndex;

    /**
     * Приватный конструктор. Используется только статическими фабричными методами.
     */
    private InputCommand(Type type, Direction direction, ItemType itemType, int selectedIndex) {
        this.type = type;
        this.direction = direction;
        this.itemType = itemType;
        this.selectedIndex = selectedIndex;
    }

    // --- Статические фабричные методы для создания команд ---

    /**
     * Создает команду перемещения.
     */
    public static InputCommand move(Direction direction) {
        return new InputCommand(Type.MOVE, direction, null, -1);
    }

    /**
     * Создает команду использования предмета.
     */
    public static InputCommand useItem(ItemType itemType) {
        return new InputCommand(Type.USE_ITEM, null, itemType, -1);
    }

    /**
     * Создает команду выбора по индексу.
     */
    public static InputCommand selectIndex(int index) {
        return new InputCommand(Type.SELECT_INDEX, null, null, index);
    }

    /**
     * Создает команду выхода из игры.
     */
    public static InputCommand quit() {
        return new InputCommand(Type.QUIT, null, null, -1);
    }

    /**
     * Создает "пустую" команду (отсутствие действия).
     */
    public static InputCommand none() {
        return new InputCommand(Type.NONE, null, null, -1);
    }

    /**
     * Создает команду снятия оружия.
     */
    public static InputCommand unequipWeapon() {
        return new InputCommand(Type.UNEQUIP_WEAPON, null, null, -1);
    }

    // --- Геттеры ---

    public Type getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }
}