package org.example.domain.enums;

import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;

import static org.example.config.GameConstants.Colors.*;
import static org.example.domain.entity.Enemy.*;

public enum EnemyType {
    ZOMBIE(GameConstants.Icons.ZOMBIE, 18, 3, 6, 4, COLOR_GREEN, 0),
    VAMPIRE(GameConstants.Icons.VAMPIRE, 24, 7, 8, 6, COLOR_RED, ABILITY_VAMPIRE_DRAIN | ABILITY_FIRST_MISS),
    GHOST(GameConstants.Icons.GHOST, 6, 6, 3, 2, COLOR_WHITE, ABILITY_TELEPORT | ABILITY_INVISIBLE),
    OGRE(GameConstants.Icons.OGRE, 32, 2,  14, 4, COLOR_YELLOW, ABILITY_OGRE_CHARGE | ABILITY_OGRE_REST),
    SNAKE_MAGE(GameConstants.Icons.SNAKE_MAGE, 10, 8, 7, 6, COLOR_WHITE, ABILITY_SNAKE_SLEEP | ABILITY_DIAGONAL_MOVE);


    private final char type;
    private final int baseHealth;
    private final int agility;
    private final int strength;
    private final int hostility;
    private final short color;
    private final int specialAbility;

    EnemyType(char type, int baseHealth, int agility, int strength, int hostility, short color, int specialAbility) {
        this.type = type;
        this.baseHealth = baseHealth;
        this.agility = agility;
        this.strength = strength;
        this.hostility = hostility;
        this.color = color;
        this.specialAbility = specialAbility;
    }

    public Enemy create(int level) {
        int scaledHealth = baseHealth + (level - 1) * 5;
        int scaledAgility = agility + (level - 1);
        int scaledStrength = strength + (level - 1) * 2;
        int scaledHostility = hostility + (level - 1);

        return new Enemy(type, scaledHealth, scaledAgility,
                scaledStrength, scaledHostility, color, specialAbility, false, 0);
    }
}
