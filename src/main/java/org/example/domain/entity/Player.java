package org.example.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.config.GameConstants;
import org.example.domain.enums.ItemType;
import org.example.domain.enums.Direction;
import org.example.domain.model.Position;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Player extends Character {
    private Position position;
    private Item equippedWeapon; // null означает использование кулаков

    /**
     * Основной конструктор для десериализации JSON
     */
    @JsonCreator
    public Player(
            @JsonProperty("position") Position position,
            @JsonProperty("equippedWeapon") Item equippedWeapon,
            @JsonProperty("maxHealth") int maxHealth,
            @JsonProperty("health") int health,
            @JsonProperty("agility") int agility,
            @JsonProperty("strength") int strength) {
        super(maxHealth, agility, strength);
        this.setHealth(health);
        this.position = position != null ? position : new Position(0, 0);
        this.equippedWeapon = equippedWeapon != null ? equippedWeapon : Item.createFists();
    }

    /**
     * Конструктор по умолчанию для Jackson
     */
    public Player() {
        this(new Position(0, 0), Item.createFists(),
                GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
    }

    /**
     * Конструктор для нового игрока с указанной позицией
     */
    public Player(Position position) {
        super(GameConstants.Player.START_MAX_HEALTH,
                GameConstants.Player.START_AGILITY,
                GameConstants.Player.START_STRENGTH);
        this.equippedWeapon = Item.createFists();
        this.position = position;
    }

    // Геттеры и сеттеры
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Item getEquippedWeapon() {
        return equippedWeapon;
    }

    /**
     * Перемещает игрока в указанном направлении
     */
    public void move(Direction direction) {
        this.position.move(direction);
    }

    /**
     * Экипирует оружие (если уже есть экипированное оружие, помещает его в инвентарь)
     */
    public void equip(Item weapon) {
        // Сохраняем текущее оружие в инвентарь, если это не кулаки
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

    /**
     * Снимает текущее оружие и возвращается к использованию кулаков
     */
    public void unequipWeapon() {
        if (this.equippedWeapon != null && !this.equippedWeapon.getSubType().equals("fists")) {
            this.getInventory().add(this.equippedWeapon);
        }

        this.equippedWeapon = Item.createFists();
        this.setStrength(GameConstants.Player.START_STRENGTH);
    }

    /**
     * Возвращает урон атаки с учетом силы персонажа и экипированного оружия
     */
    public int getAttackDamage() {
        int baseDamage = this.getStrength();

        if (equippedWeapon != null) {
            baseDamage += equippedWeapon.getStrength();
        }

        return baseDamage;
    }

    /**
     * Использует предмет из инвентаря по типу и индексу
     * @return true если предмет был успешно использован
     */
    public boolean useItem(ItemType type, int index) {
        Item item = this.getInventory().take(type, index);
        if (item == null) {
            return false;
        }

        applyItemEffects(item);
        return true;
    }

    /**
     * Применяет эффекты предмета (восстановление здоровья, увеличение характеристик и т.д.)
     */
    public void applyItemEffects(Item item) {
        // Восстановление здоровья
        if (item.getHealth() > 0) {
            this.heal(item.getHealth());
        }

        // Увеличение максимального здоровья
        if (item.getMaxHealth() > 0) {
            int newMaxHealth = this.getMaxHealth() + item.getMaxHealth();
            this.setMaxHealth(newMaxHealth);
            this.heal(item.getMaxHealth());
        }

        // Увеличение ловкости
        if (item.getAgility() > 0) {
            this.setAgility(this.getAgility() + item.getAgility());
        }

        // Увеличение силы (кроме оружия - для оружия есть отдельный метод equip)
        if (item.getStrength() > 0 && !item.getType().equals("weapon")) {
            this.setStrength(this.getStrength() + item.getStrength());
        }

//        // Экипировка оружия
//        if (item.getType().equals("weapon")) {
//            this.equip(item);
//        }
    }

    /**
     * Возвращает общую стоимость всех сокровищ в инвентаре
     */
    public int getTreasureValue() {
        return this.getInventory().getTreasureValue();
    }

    /**
     * Возвращает строку с текущим статусом игрока
     */
    @Override
    public String getStatus() {
        String weaponInfo = equippedWeapon != null ?
                equippedWeapon.getSubType() : "fists";

        return String.format("HP: %d/%d | STR: %d | AGI: %d | Weapon: %s",
                this.getHealth(), this.getMaxHealth(),
                this.getStrength(), this.getAgility(), weaponInfo);
    }
}