package org.example.domain.entity;

import org.example.config.GameConstants;
import org.example.domain.model.Direction;
import org.example.domain.model.Position;

// Персонаж + позиция + инвентарь
public class Player extends Character  {
    private Position position;
    private Item equippedWeapon; // null = кулаки

//    private Inventory inventory;
//    private int maxHealth = GameConstants.Player.START_MAX_HEALTH;
//    private int health = GameConstants.Player.START_HEALTH;
//    private int agility = GameConstants.Player.START_AGILITY;
//    private int strength = GameConstants.Player.START_STRENGTH;
    private boolean alive = true;

    public Player(Position position) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.position = position;
    }

    public Player(Position position, Inventory inventory) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.position = position;
        this.inventory = inventory;
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
    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean playerAlive) {
        this.alive = playerAlive;
    }

    public void equip(Item weapon) { /* TODO */ }
}
