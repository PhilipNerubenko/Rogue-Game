package org.example.domain.model;

import org.example.domain.entity.ItemType;

public class InputCommand {
    public enum Type {
        NONE, MOVE, USE_ITEM, SELECT_INDEX, UNEQUIP_WEAPON, QUIT
    }

    private final Type type;
    private final Direction direction;
    private final ItemType itemType;
    private final int selectedIndex;

    private InputCommand(Type type, Direction direction, ItemType itemType, int selectedIndex) {
        this.type = type;
        this.direction = direction;
        this.itemType = itemType;
        this.selectedIndex = selectedIndex;
    }

    public static InputCommand move(Direction direction) {
        return new InputCommand(Type.MOVE, direction, null, -1);
    }

    public static InputCommand useItem(ItemType itemType) {
        return new InputCommand(Type.USE_ITEM, null, itemType, -1);
    }

    public static InputCommand selectIndex(int index) {
        return new InputCommand(Type.SELECT_INDEX, null, null, index);
    }

    public static InputCommand quit() {
        return new InputCommand(Type.QUIT, null, null, -1);
    }

    public static InputCommand none() {
        return new InputCommand(Type.NONE, null, null, -1);
    }

    public static InputCommand unequipWeapon() {
        return new InputCommand(Type.UNEQUIP_WEAPON, null, null, -1);
    }

    public Type getType() { return type; }
    public Direction getDirection() { return direction; }
    public ItemType getItemType() { return itemType; }
    public int getSelectedIndex() { return selectedIndex; }
}
