package org.example.domain.entity;

import org.example.domain.model.Direction;
import org.example.domain.model.Position;

// Персонаж + позиция + инвентарь
public class Player {
    private Position position;
    private Item equippedWeapon; // null = кулаки
    private Inventory inventory;

    public void move(Direction dir) { /* обновить position */ }
    public void equip(Item weapon) { /* сменить оружие */ }
}
