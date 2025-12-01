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
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    visibleCells.add(new Position(x, y));
                    // ✅ ВАЖНО: сразу добавляем в exploredCells
                    exploredCells.add(new Position(x, y));
                }
            }
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
        // Правильный алгоритм Брезенхэма для ray casting
        double x = startX;
        double y = startY;

        // Нормализуем вектор
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return;

        double stepX = dx / length ;
        double stepY = dy / length ;

        boolean hitWall = false;
        Room playerInRoom = levelGenerator.getRoomAt(startX, startY);

        for (int i = 0; i < VISION_RADIUS; i++) {
            x += stepX;
            y += stepY;

            int intX = (int) Math.round(x);
            int intY = (int) Math.round(y);

            // Проверяем границы
            if (intX < 0 || intX >= map[0].length || intY < 0 || intY >= map.length) break;


            char cell = map[intY][intX];
            Position pos = new Position(intX, intY);    //текущая позиция луча бегущего по направлению

            // Если игрок в комнате Она видна всегда и луч останавливается  на стенах

            if (playerInRoom != null) {
                if ( cell == '|' || cell == '~' || cell == ' '){
                    break;
                }
                if ( cell == '+' || cell == '#' ){
                    visibleCells.add(pos);
                    continue;
                }

            }


            // Если игрок в коридоре  луч останавливается на стенах и земле

            if (playerInRoom == null) {
                if ( cell == '|' || cell == '~' || cell == ' ' ){
                    break;
                }
                if ( cell == '+' || cell == '#' ){
                    visibleCells.add(pos);
                }


                // Если дверь — заглядываем в комнату и останавливаемся
                if (cell == '+') {
                    visibleCells.add(pos); // ✅ Добавляем саму дверь
                    Room adjacentRoom = findAdjacentRoom(intX, intY);
                    if (adjacentRoom != null && adjacentRoom != currentRoom) {
                        addVisibleRoomInterior(intX, intY, stepX, stepY, adjacentRoom);

                    }
                    break;
                }


            }




        }
    }

    /**
     * Добавить видимые клетки внутри комнаты (через дверь)
     */
    private void addVisibleRoomInterior(int doorX, int doorY, double dx, double dy, Room room) {
        // Добавляем 2-3 клетки в глубь комнаты в направлении взгляда
        for (int depth = 1; depth <= 8; depth++) {
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
     * По двери определяет комнату
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

    public void markCellAsExplored(int x, int y) {
        exploredCells.add(new Position(x, y));
    }
}
