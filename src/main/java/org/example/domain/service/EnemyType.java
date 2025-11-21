package org.example.domain.service;

import org.example.domain.entity.Enemy;

import java.awt.*;

import static org.example.domain.entity.Enemy.*;

public enum EnemyType {
    ZOMBIE("z", 15, 3, 1, 6, 3, Color.GREEN, 0),
    VAMPIRE("v", 20, 5, 2, 8, 5, Color.RED, ABILITY_VAMPIRE_DRAIN | ABILITY_FIRST_MISS),
    GHOST("g", 8, 4, 3, 3, 2, Color.WHITE, ABILITY_TELEPORT | ABILITY_INVISIBLE),
    OGRE("O", 30, 2, 2, 12, 4, Color.YELLOW, ABILITY_OGRE_CHARGE | ABILITY_OGRE_REST),
    SNAKE_MAGE("s", 12, 6, 2, 8, 6, Color.CYAN, ABILITY_SNAKE_SLEEP | ABILITY_DIAGONAL_MOVE);

    private final String type;
    private final int baseHealth;
    private final int agility;
    private final int speed;
    private final int strength;
    private final int hostility;
    private final Color color;
    private final int specialAbilities;

    EnemyType(String type, int baseHealth, int agility, int speed, int strength, int hostility, Color color, int specialAbilities) {
        this.type = type;
        this.baseHealth = baseHealth;
        this.agility = agility;
        this.speed = speed;
        this.strength = strength;
        this.hostility = hostility;
        this.color = color;
        this.specialAbilities = specialAbilities;
    }

    public Enemy create(int level) {
        int scaledHealth = baseHealth + (level - 1) * 5;
        int scaledAgility = agility + (level - 1);
        int scaledStrength = strength + (level - 1) * 2;
        int scaledHostility = hostility + (level - 1);

        return new Enemy(type, scaledHealth, scaledAgility, speed,
                scaledStrength, scaledHostility, color, specialAbilities);
    }
}
