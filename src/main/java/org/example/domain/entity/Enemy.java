package org.example.domain.entity;

import java.awt.*;

public class Enemy {
    private char type;
    private int health;
    private int agility;
    private int speed;
    private int strength;
    private int hostility;
    private int x;
    private int y;
    private int diagX = 1;
    private int diagY = 1;

    // Дополнительные свойста из ТЗ
    private Color color;
    private int specialAbility;
    private boolean isInvisible;
    private int restTurns; // Для огра

    // Битовые маски способностей
    public static final int ABILITY_VAMPIRE_DRAIN = 1;    // Вампир: поглощает макс. HP
    public static final int ABILITY_FIRST_MISS = 2;       // Вампир: первая атака промах
    public static final int ABILITY_TELEPORT = 4;         // Призрак: телепортация
    public static final int ABILITY_INVISIBLE = 8;        // Призрак: невидимость
    public static final int ABILITY_OGRE_CHARGE = 16;     // Огр: движение на 2 клетки
    public static final int ABILITY_OGRE_REST = 32;       // Огр: отдых после атаки
    public static final int ABILITY_SNAKE_SLEEP = 64;     // Змеиный маг: сон игрока
    public static final int ABILITY_DIAGONAL_MOVE = 128;  // Змеиный маг: диагональное движение

    public Enemy(char type, int health, int agility, int speed, int strength, int hostility, Color color, int specialAbility) {
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

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getHostility() {
        return hostility;
    }

    public void setHostility(int hostility) {
        this.hostility = hostility;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getSpecialAbility() {
        return specialAbility;
    }

    public void setSpecialAbility(int specialAbility) {
        this.specialAbility = specialAbility;
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

    // Проверка, есть ли у врага определённая способность
    public boolean hasAbility(int abilityMask) {
        return (specialAbility & abilityMask) == abilityMask;
    }

    public void removeAbility(int abilityMask) {
        specialAbility &= ~abilityMask;
    }

//    public boolean isWillCounterAttack() { return willCounterAttack; }
//    public void setWillCounterAttack(boolean willCounterAttack) {
//        this.willCounterAttack = willCounterAttack;
//    }
}
