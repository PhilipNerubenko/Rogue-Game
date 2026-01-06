package org.example.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Enemy {
    private char type;
    private int health;
    private int agility;
    private int speed;
    private int strength;
    private int hostility;
    private int x;
    private int y;
    private int diagX = 1;  // Направление движения по диагонали по X
    private int diagY = 1;  // Направление движения по диагонали по Y

    // Дополнительные свойства из ТЗ
    private Color color;
    private int specialAbility;  // Битовое поле специальных способностей
    private boolean isInvisible;
    private int restTurns; // Для огра - счетчик отдыха после атаки

    // Битовые маски способностей
    public static final int ABILITY_VAMPIRE_DRAIN = 1;    // Вампир: поглощает макс. HP
    public static final int ABILITY_FIRST_MISS = 2;       // Вампир: первая атака промах
    public static final int ABILITY_TELEPORT = 4;         // Призрак: телепортация
    public static final int ABILITY_INVISIBLE = 8;        // Призрак: невидимость
    public static final int ABILITY_OGRE_CHARGE = 16;     // Огр: движение на 2 клетки
    public static final int ABILITY_OGRE_REST = 32;       // Огр: отдых после атаки
    public static final int ABILITY_SNAKE_SLEEP = 64;     // Змеиный маг: сон игрока
    public static final int ABILITY_DIAGONAL_MOVE = 128;  // Змеиный маг: диагональное движение

    @JsonCreator
    public Enemy(
            @JsonProperty("type") char type,
            @JsonProperty("health") int health,
            @JsonProperty("agility") int agility,
            @JsonProperty("speed") int speed,
            @JsonProperty("strength") int strength,
            @JsonProperty("hostility") int hostility,
            @JsonProperty("color") Color color,
            @JsonProperty("specialAbility") int specialAbility) {
        this.type = type;
        this.health = health;
        this.agility = agility;
        this.speed = speed;
        this.strength = strength;
        this.hostility = hostility;
        this.color = color;
        this.specialAbility = specialAbility;
        this.isInvisible = false;
        this.restTurns = 0;
    }

    // Конструктор по умолчанию для Jackson
    public Enemy() {
        this(' ', 0, 0, 0, 0, 0, Color.WHITE, 0);
    }

    // Геттеры и сеттеры для основных свойств
    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getAgility() {
        return agility;
    }

    public int getStrength() {
        return strength;
    }

    public int getHostility() {
        return hostility;
    }

    public boolean isInvisible() {
        return isInvisible;
    }

    public void setInvisible(boolean invisible) {
        isInvisible = invisible;
    }

    public int getRestTurns() {
        return restTurns;
    }

    public void setRestTurns(int restTurns) {
        this.restTurns = restTurns;
    }

    // Геттеры и сеттеры для координат и направления движения
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getDiagX() {
        return diagX;
    }

    public void setDiagX(int diagX) {
        this.diagX = diagX;
    }

    public int getDiagY() {
        return diagY;
    }

    public void setDiagY(int diagY) {
        this.diagY = diagY;
    }

    /**
     * Проверяет, есть ли у врача определённая способность
     * @param abilityMask битовая маска способности
     * @return true если способность присутствует
     */
    public boolean hasAbility(int abilityMask) {
        return (specialAbility & abilityMask) == abilityMask;
    }

    /**
     * Удаляет способность у врага
     * @param abilityMask битовая маска способности для удаления
     */
    public void removeAbility(int abilityMask) {
        specialAbility &= ~abilityMask;
    }
}