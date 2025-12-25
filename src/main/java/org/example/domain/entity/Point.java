package org.example.domain.entity;

import java.io.Serializable;

/**
 * Класс для представления точки на игровой карте.
 * Аналог структуры coordinates_t из C-кода.
 */
public class Point implements Serializable {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    /**
     * Конструктор по умолчанию (0, 0)
     */
    public Point() {
        this(0, 0);
    }

    /**
     * Конструктор с координатами
     * @param x координата X
     * @param y координата Y
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Конструктор копирования
     * @param other другая точка
     */
    public Point(Point other) {
        this(other.x, other.y);
    }

    // Геттеры и сеттеры
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

    /**
     * Установка координат
     * @param x координата X
     * @param y координата Y
     */
    public void setCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Установка координат из другой точки
     * @param other другая точка
     */
    public void setCoordinates(Point other) {
        this.x = other.x;
        this.y = other.y;
    }

    /**
     * Смещение точки на указанные значения
     * @param dx смещение по X
     * @param dy смещение по Y
     * @return новая точка
     */
    public Point add(int dx, int dy) {
        return new Point(this.x + dx, this.y + dy);
    }

    /**
     * Смещение точки на вектор из другой точки
     * @param other вектор смещения
     * @return новая точка
     */
    public Point add(Point other) {
        return new Point(this.x + other.x, this.y + other.y);
    }

    /**
     * Сравнение двух точек
     * @param other другая точка
     * @return true если координаты равны
     */
    public boolean equals(Point other) {
        if (other == null) return false;
        return this.x == other.x && this.y == other.y;
    }

    /**
     * Проверка, находится ли точка в пределах прямоугольной области
     * @param upperLeft верхний левый угол области
     * @param bottomRight нижний правый угол области
     * @return true если точка внутри
     */
    public boolean isWithin(Point upperLeft, Point bottomRight) {
        return x >= upperLeft.x && x <= bottomRight.x &&
                y >= upperLeft.y && y <= bottomRight.y;
    }

    /**
     * Рассчет расстояния до другой точки (манхэттенское расстояние)
     * @param other другая точка
     * @return расстояние
     */
    public int manhattanDistanceTo(Point other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    /**
     * Рассчет расстояния до другой точки (евклидово расстояние)
     * @param other другая точка
     * @return расстояние
     */
    public double euclideanDistanceTo(Point other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Проверка, является ли точка соседней по вертикали/горизонтали
     * @param other другая точка
     * @return true если точки соседние
     */
    public boolean isAdjacent(Point other) {
        int dx = Math.abs(this.x - other.x);
        int dy = Math.abs(this.y - other.y);
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    /**
     * Проверка, является ли точка соседней по диагонали
     * @param other другая точка
     * @return true если точки диагонально соседние
     */
    public boolean isDiagonalAdjacent(Point other) {
        int dx = Math.abs(this.x - other.x);
        int dy = Math.abs(this.y - other.y);
        return dx == 1 && dy == 1;
    }

    /**
     * Проверка, является ли точка соседней в любом направлении
     * @param other другая точка
     * @return true если точки соседние в любом направлении
     */
    public boolean isNeighbor(Point other) {
        int dx = Math.abs(this.x - other.x);
        int dy = Math.abs(this.y - other.y);
        return dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0);
    }

    /**
     * Получение направления к другой точке
     * @param other целевая точка
     * @return направление или null если точки совпадают
     */
    public Direction directionTo(Point other) {
        if (this.equals(other)) {
            return null;
        }

        int dx = other.x - this.x;
        int dy = other.y - this.y;

        // Нормализация направления
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
    }

    /**
     * Создание точки со случайными координатами в заданном диапазоне
     * @param minX минимальная X
     * @param maxX максимальная X
     * @param minY минимальная Y
     * @param maxY максимальная Y
     * @return случайная точка
     */
    public static Point randomPoint(int minX, int maxX, int minY, int maxY) {
        int x = minX + (int)(Math.random() * (maxX - minX + 1));
        int y = minY + (int)(Math.random() * (maxY - minY + 1));
        return new Point(x, y);
    }

    /**
     * Проверка валидности координат (не отрицательные)
     * @return true если координаты валидны
     */
    public boolean isValid() {
        return x >= 0 && y >= 0;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    /**
     * Перечисление направлений движения
     * Аналог directions_e из C-кода
     */
    public enum Direction implements Serializable {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        UP_LEFT,
        UP_RIGHT,
        DOWN_LEFT,
        DOWN_RIGHT,
        NONE;

        private static final long serialVersionUID = 1L;

        /**
         * Получение смещения для направления
         * @return точка смещения
         */
        public Point getOffset() {
            switch (this) {
                case UP: return new Point(0, -1);
                case DOWN: return new Point(0, 1);
                case LEFT: return new Point(-1, 0);
                case RIGHT: return new Point(1, 0);
                case UP_LEFT: return new Point(-1, -1);
                case UP_RIGHT: return new Point(1, -1);
                case DOWN_LEFT: return new Point(-1, 1);
                case DOWN_RIGHT: return new Point(1, 1);
                default: return new Point(0, 0);
            }
        }

        /**
         * Проверка, является ли направление диагональным
         * @return true если диагональное
         */
        public boolean isDiagonal() {
            return this == UP_LEFT || this == UP_RIGHT ||
                    this == DOWN_LEFT || this == DOWN_RIGHT;
        }

        /**
         * Получение направления из символа WASD
         * @param ch символ направления
         * @return направление или NONE
         */
        public static Direction fromChar(char ch) {
            switch (Character.toLowerCase(ch)) {
                case 'w': return UP;
                case 's': return DOWN;
                case 'a': return LEFT;
                case 'd': return RIGHT;
                case 'q': return UP_LEFT;
                case 'e': return UP_RIGHT;
                case 'z': return DOWN_LEFT;
                case 'c': return DOWN_RIGHT;
                default: return NONE;
            }
        }

        /**
         * Получение направления из стрелки (символа)
         * @param ch символ стрелки
         * @return направление или NONE
         */
        public static Direction fromArrow(char ch) {
            switch (ch) {
                case '↑': case '8': return UP;
                case '↓': case '2': return DOWN;
                case '←': case '4': return LEFT;
                case '→': case '6': return RIGHT;
                case '7': return UP_LEFT;
                case '9': return UP_RIGHT;
                case '1': return DOWN_LEFT;
                case '3': return DOWN_RIGHT;
                default: return NONE;
            }
        }

        /**
         * Получение символа для отображения направления
         * @return символ направления
         */
        public char toSymbol() {
            switch (this) {
                case UP: return '↑';
                case DOWN: return '↓';
                case LEFT: return '←';
                case RIGHT: return '→';
                case UP_LEFT: return '↖';
                case UP_RIGHT: return '↗';
                case DOWN_LEFT: return '↙';
                case DOWN_RIGHT: return '↘';
                default: return '•';
            }
        }

        /**
         * Получение противоположного направления
         * @return противоположное направление
         */
        public Direction opposite() {
            switch (this) {
                case UP: return DOWN;
                case DOWN: return UP;
                case LEFT: return RIGHT;
                case RIGHT: return LEFT;
                case UP_LEFT: return DOWN_RIGHT;
                case UP_RIGHT: return DOWN_LEFT;
                case DOWN_LEFT: return UP_RIGHT;
                case DOWN_RIGHT: return UP_LEFT;
                default: return NONE;
            }
        }
    }
}
