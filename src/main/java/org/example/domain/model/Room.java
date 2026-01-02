package org.example.domain.model;

public class Room {
    private final int id;
    private final int x1;                  // значение Х - левый верхний угол
    private final int y1;                  // значение Х - левый верхний угол
    private final int width;               // количество символов по горизонтали
    private final int height;              // количество строк по вертикали
    private final int x2;                  // значение Х - правый нижний угол
    private final int y2;                  // значение Y - правый нижний угол



    private boolean startRoom;
    private boolean exitRoom;

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

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public boolean isStartRoom() {
        return startRoom;
    }

    public void setStartRoom(boolean startRoom) {
        this.startRoom = startRoom;
    }

    public boolean isExitRoom() {
        return exitRoom;
    }

    public void setExitRoom(boolean exitRoom) {
        this.exitRoom = exitRoom;
    }
}
