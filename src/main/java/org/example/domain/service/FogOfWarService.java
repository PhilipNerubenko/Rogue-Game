package org.example.domain.service;



import org.example.config.GameConstants;
import org.example.domain.model.Position;
import org.example.domain.model.Room;

import java.util.*;


/**
 * Сервич скрытия монстров и артефактов , при нахождении в другой комнате.
 *  Алгоритм Брезенхэмом
 */
public class FogOfWarService {

    private Set<Position> visibleCells = new HashSet<>();
    private Set<Position> exploredCells = new HashSet<>();
    private Set<Room> exploredRooms = new HashSet<>(); // ИССЛЕДОВАННЫЕ КОМНАТЫ
    private Room currentRoom = null; // ТЕКУЩАЯ КОМНАТА ИГРОКА

    private static final int VISION_RADIUS = GameConstants.Map.VISION_RADIUS;
    private LevelGenerator levelGenerator; // ВНЕДРЯЕМ ЗАВИСИМОСТЬ

    // Добавьте конструктор
    public FogOfWarService(LevelGenerator levelGenerator) {
        this.levelGenerator = levelGenerator;
    }


    /**
     * Обновить видимость после перемещения игрока
     */
    public void updateVisibility(Position playerPos, char[][] map) {
        visibleCells.clear();

        int playerX = playerPos.getX();
        int playerY = playerPos.getY();

        // 1. Определяем текущую комнату
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);

        // 2. ЕСЛИ ИГРОК В КОМНАТЕ
        if (currentRoom != null) {
            // Вся комната становится видимой
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    visibleCells.add(new Position(x, y));
                }
            }
            // Помечаем комнату как исследованную
            exploredRooms.add(currentRoom);
        }

        // 3. RAY CASTING из позиции игрока (для коридоров и дверей)
        for (int angle = 0; angle < 360; angle += 5) {
            double radian = Math.toRadians(angle);
            castRay(playerX, playerY, Math.cos(radian), Math.sin(radian), map);
        }

        // 4. Добавляем все видимые клетки в "исследованные"
        exploredCells.addAll(visibleCells);
    }

    /**
     * Бросить один луч и добавить видимые клетки
     */
    private void castRay(int startX, int startY, double dx, double dy, char[][] map) {
        int x = startX;
        int y = startY;

        for (int i = 0; i < VISION_RADIUS; i++) {
            x += (int) Math.round(dx);
            y += (int) Math.round(dy);

            // Проверяем границы
            if (x < 0 || x >= map[0].length || y < 0 || y >= map.length) break;

            Position currentPos = new Position(x, y);
            visibleCells.add(currentPos);

            char cell = map[y][x];

            // Останавливаемся на стене
            if (cell == '|' || cell == '~') {
                break;
            }

            // ОСОБАЯ ЛОГИКА: если видим дверь, заглядываем в комнату
            if (cell == '+') {
                // Смотрим, какая комната за дверью
                Room adjacentRoom = findAdjacentRoom(x, y);
                if (adjacentRoom != null && adjacentRoom != currentRoom) {
                    // Добавляем несколько клеток в глубь комнаты
                    addVisibleRoomInterior(x, y, dx, dy, adjacentRoom);
                }
            }
        }
    }

    /**
     * Добавить видимые клетки внутри комнаты (через дверь)
     */
    private void addVisibleRoomInterior(int doorX, int doorY, double dx, double dy, Room room) {
        // Добавляем 2-3 клетки в глубь комнаты в направлении взгляда
        for (int depth = 1; depth <= 3; depth++) {
            int viewX = doorX + (int)(dx * depth);
            int viewY = doorY + (int)(dy * depth);

            if (viewX >= room.getX1() && viewX <= room.getX2() &&
                    viewY >= room.getY1() && viewY <= room.getY2()) {
                visibleCells.add(new Position(viewX, viewY));
            } else {
                break; // Вышли за пределы комнаты
            }
        }
    }

    /**
     * Найти комнату, смежную с дверью
     */
    private Room findAdjacentRoom(int doorX, int doorY) {
        // Проверяем соседние клетки
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Room room = levelGenerator.getRoomAt(doorX + dx, doorY + dy);
                if (room != null) return room;
            }
        }
        return null;
    }

    public boolean isVisible(int x, int y) {
        return visibleCells.contains(new Position(x, y));
    }

    public boolean isExplored(int x, int y) {
        return exploredCells.contains(new Position(x, y));
    }


    public boolean isRoomExplored(Room room) {
        return exploredRooms.contains(room);
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }
}
