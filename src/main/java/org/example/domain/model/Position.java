package org.example.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Position {
    private int x; // Координата по оси X
    private int y; // Координата по оси Y

    @JsonCreator
    public Position(
            @JsonProperty("x") int x,
            @JsonProperty("y") int y) {
        this.x = x;
        this.y = y;
    }

    // Конструктор по умолчанию (требуется для десериализации Jackson)
    public Position() {
        this(0, 0); // Инициализация позиции в начале координат
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

    @Override
    public boolean equals(Object o) {
        // Проверка на идентичность объектов
        if (this == o) return true;
        // Проверка на null и соответствие классов
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        // Две позиции равны, если равны их координаты X и Y
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        // Генерация хэш-кода на основе координат
        return Objects.hash(x, y);
    }
}