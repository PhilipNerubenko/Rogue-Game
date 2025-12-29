package org.example.domain.entity;

import org.example.config.GameConstants;
import org.example.domain.model.Direction;
import org.example.domain.model.Position;

public class Player extends Character {
    private Position position;
    private Item equippedWeapon; // null = кулаки

    public Player() {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.equippedWeapon = Item.createFists();
        this.position = new Position(0, 0); // Временная позиция, будет обновлена в GameLoop
    }

    public Player(Position position) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.equippedWeapon = Item.createFists();
        this.position = position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void move(Direction direction) {
        this.position = new Position(
                this.position.getX() + direction.getDx(),
                this.position.getY() + direction.getDy()
        );
    }

    public Item getEquippedWeapon() {
        return equippedWeapon;
    }

    public void equip(Item weapon) {
        // Если уже экипировано оружие, кладем его в инвентарь
        if (this.equippedWeapon != null && !this.equippedWeapon.getSubType().equals("fists")) {
            this.getInventory().add(this.equippedWeapon);
        }

        this.equippedWeapon = weapon;

        // Обновляем силу персонажа при экипировке оружия
        if (weapon != null) {
            int newStrength = GameConstants.Player.START_STRENGTH + weapon.getStrength();
            this.setStrength(newStrength);
        }
    }

    public void unequipWeapon() {
        if (this.equippedWeapon != null && !this.equippedWeapon.getSubType().equals("fists")) {
            // Кладем текущее оружие в инвентарь
            this.getInventory().add(this.equippedWeapon);
        }

        // Возвращаемся к кулакам
        this.equippedWeapon = Item.createFists();
        this.setStrength(GameConstants.Player.START_STRENGTH);
    }

    // Удобный метод для получения урона с учетом оружия
    public int getAttackDamage() {
        int baseDamage = this.getStrength();

        if (equippedWeapon != null) {
            baseDamage += equippedWeapon.getStrength();
        }

        return baseDamage;
    }

    // Метод для применения предмета из инвентаря
    public boolean useItem(ItemType type, int index) {
        Item item = this.getInventory().take(type, index);
        if (item == null) {
            return false;
        }

        // Применяем эффекты предмета
        applyItemEffects(item);
        return true;
    }

    private void applyItemEffects(Item item) {
        // Восстановление здоровья (еда)
        if (item.getHealth() > 0) {
            this.heal(item.getHealth());
        }

        // Увеличение максимального здоровья (свитки/эликсиры)
        if (item.getMaxHealth() > 0) {
            int newMaxHealth = this.getMaxHealth() + item.getMaxHealth();
            this.setMaxHealth(newMaxHealth);
            // Также восстанавливаем здоровье на ту же величину
            this.heal(item.getMaxHealth());
        }

        // Увеличение ловкости (свитки/эликсиры)
        if (item.getAgility() > 0) {
            this.setAgility(this.getAgility() + item.getAgility());
        }

        // Увеличение силы (свитки/эликсиры)
        if (item.getStrength() > 0 && !item.getType().equals("weapon")) {
            this.setStrength(this.getStrength() + item.getStrength());
        }

        // Экипировка оружия
        if (item.getType().equals("weapon")) {
            this.equip(item);
        }
    }

    // Метод для получения общей стоимости сокровищ
    public int getTreasureValue() {
        // Нужно будет добавить метод в Inventory для получения стоимости сокровищ
        return 0; // TODO: реализовать
    }

    @Override
    public String getStatus() {
        String weaponInfo = equippedWeapon != null ?
                equippedWeapon.getSubType() : "fists";

        return String.format("HP: %d/%d | STR: %d | AGI: %d | Weapon: %s",
                this.getHealth(), this.getMaxHealth(),
                this.getStrength(), this.getAgility(), weaponInfo);
    }
}