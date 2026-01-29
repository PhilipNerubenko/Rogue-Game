package org.example.domain.enums;

// Энум для классификации передвижения
//Первые 4 для всех, оставшиеся последние 4 для Змея
public enum Direction {
    NORTH(0, -1),
    SOUTH(0, 1),
    WEST(-1, 0),
    EAST(1, 0),
    NORTH_WEST(-1, -1),  // Для змеиного мага
    NORTH_EAST(1, -1),   // Для змеиного мага
    SOUTH_WEST(-1, 1),   // Для змеиного мага
    SOUTH_EAST(1, 1);    // Для змеиного мага

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }
}
