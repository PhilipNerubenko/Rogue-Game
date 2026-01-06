package org.example.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {
    private final int id;
    private final int x1;                  // X координата левого верхнего угла
    private final int y1;                  // Y координата левого верхнего угла
    private final int width;               // ширина комнаты в символах
    private final int height;              // высота комнаты в строках
    private final int x2;                  // X координата правого нижнего угла (вычисляется)
    private final int y2;                  // Y координата правого нижнего угла (вычисляется)

    private boolean startRoom;             // флаг начальной комнаты
    private boolean exitRoom;              // флаг выходной комнаты

    /**
     * Основной конструктор для создания комнаты из JSON.
     * Используется Jackson для десериализации.
     */
    @JsonCreator
    public Room(
            @JsonProperty("id") int roomID,
            @JsonProperty("x1") int x1,
            @JsonProperty("y1") int y1,
            @JsonProperty("width") int width,
            @JsonProperty("height") int height) {
        this.id = roomID;
        this.x1 = x1;
        this.y1 = y1;
        this.width = width;
        this.height = height;
        this.x2 = x1 + width;   // вычисляем правую границу
        this.y2 = y1 + height;  // вычисляем нижнюю границу
        this.startRoom = false;
        this.exitRoom = false;
    }

    /**
     * Конструктор по умолчанию для Jackson.
     * Создает комнату с нулевыми параметрами.
     */
    public Room() {
        this(0, 0, 0, 0, 0);
    }

    /**
     * Конструктор для создания комнаты с использованием объекта Position.
     * @param roomID идентификатор комнаты
     * @param roomPosition позиция левого верхнего угла
     * @param width ширина комнаты
     * @param height высота комнаты
     */
    public Room(int roomID, Position roomPosition, int width, int height) {
        this.id = roomID;
        this.x1 = roomPosition.getX();
        this.y1 = roomPosition.getY();
        this.width = width;
        this.height = height;
        this.x2 = x1 + width;
        this.y2 = y1 + height;
        this.startRoom = false;
        this.exitRoom = false;
    }

    // Геттеры для координат и размеров
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }

    // Геттеры и сеттеры для флагов
    public boolean isStartRoom() { return startRoom; }
    public void setStartRoom(boolean startRoom) { this.startRoom = startRoom; }
    public boolean isExitRoom() { return exitRoom; }
    public void setExitRoom(boolean exitRoom) { this.exitRoom = exitRoom; }
}