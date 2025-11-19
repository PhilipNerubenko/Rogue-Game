package org.example.domain.service;

import org.example.domain.entity.Enemy;

import java.awt.*;

import static org.example.domain.entity.Enemy.*;

public enum EnemyType {
    ZOMBIE("z", 30, 2, 1, 8, 3, Color.GREEN, 0),
    VAMPIRE("v", 25, 8, 2, 10, 5, Color.RED,
            ABILITY_VAMPIRE_DRAIN | ABILITY_FIRST_MISS),
    GHOST("g", 10, 9, 3, 3, 2, Color.WHITE,
            ABILITY_TELEPORT | ABILITY_INVISIBLE),
    OGRE("O", 40, 3, 2, 15, 4, Color.YELLOW,
            ABILITY_OGRE_CHARGE | ABILITY_OGRE_REST),
    SNAKE_MAGE("s", 15, 10, 2, 8, 6, Color.WHITE,
            ABILITY_SNAKE_SLEEP | ABILITY_DIAGONAL_MOVE);

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

    public Enemy create(int level, int playerAgility) {
        int scaledHealth = (int)(baseHealth * (1 + 0.25 * (level - 1)));

        int scaledStrength = (int)(strength * (1 + 0.2 * (level - 1)));

        int balancedAgility = Math.max(agility, playerAgility - 5);

        return new Enemy(type, scaledHealth, balancedAgility, speed,
                scaledStrength, hostility, color, specialAbilities);
    }
}
