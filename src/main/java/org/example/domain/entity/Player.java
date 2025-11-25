package org.example.domain.entity;

import org.example.config.GameConstants;
import org.example.domain.model.Direction;
import org.example.domain.model.Position;
import org.example.domain.service.InventoryService;

// Персонаж + позиция + инвентарь
public class Player extends Character  {
    private Position position;
    private Item equippedWeapon; // null = кулаки
//    private Inventory inventory;
//    private int maxHealth = GameConstants.Player.START_MAX_HEALTH;
//    private int health = GameConstants.Player.START_HEALTH;
//    private int agility = GameConstants.Player.START_AGILITY;
//    private int strength = GameConstants.Player.START_STRENGTH;
    private InventoryService inventoryService;

    public Player(Position position) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.position = position;
        this.inventoryService = new InventoryService();
    }

    public Player(Position position, Inventory inventory) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.position = position;
        this.setInventory(inventory);
    }
    // ВОТ ЭТОГО МЕТОДА НЕ ХВАТАЕТ:
    public Position getPosition() {
        return position;
    }

    public void move(Direction direction) {
        this.position = new Position(
                this.position.getX() + direction.getDx(),
                this.position.getY() + direction.getDy()
        );
    }
    public void equip(Item weapon) { /* TODO */ }

    public InventoryService getInventoryService() {
        return inventoryService;
    }
}
