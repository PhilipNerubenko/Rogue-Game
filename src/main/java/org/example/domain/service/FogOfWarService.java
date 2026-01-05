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

    public void markCellAsExplored(int x, int y) {
        exploredCells.add(new Position(x, y));
    }

    public void reset() {
        visibleCells.clear();
        exploredCells.clear();
        exploredRooms.clear(); // ИССЛЕДОВАННЫЕ КОМНАТЫ
    }
    /**
     * Метод для обновления видимости при загрузке сохранения
     * Отмечает комнату игрока как исследованную
     */
    public void updateForLoadedGame(Position playerPos, char[][] map) {
        // Сбрасываем состояние
        reset();

        // Определяем комнату игрока
        currentRoom = levelGenerator.getRoomAt(playerPos.getX(), playerPos.getY());

        if (currentRoom != null) {
            // Добавляем всю комнату игрока в исследованные
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    exploredCells.add(new Position(x, y));
                }
            }
            exploredRooms.add(currentRoom);
        }

        // Помечаем все коридоры как исследованные
        if (map != null) {
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[y].length; x++) {
                    char tile = map[y][x];
                    // Коридоры и двери всегда исследованы
                    if (tile == '#' || tile == '+') {
                        exploredCells.add(new Position(x, y));
                    }
                }
            }
        }

        // Обновляем видимость
        updateVisibility(playerPos, map);
    }

    /**
     * Метод для принудительного отображения исследованной карты
     */
    public void showExploredMap(char[][] map) {
        if (map == null) return;

        // Сначала очищаем видимые клетки
        visibleCells.clear();

        // Добавляем все исследованные клетки в видимые
        visibleCells.addAll(exploredCells);

        // Также добавляем коридоры и двери
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                char tile = map[y][x];
                if (tile == '#' || tile == '+') {
                    Position pos = new Position(x, y);
                    if (!visibleCells.contains(pos)) {
                        visibleCells.add(pos);
                    }
                }
            }
        }

        // Обновляем исследованные комнаты
        updateExploredRoomsFromCells();
    }

    /**
     * Обновляет список исследованных комнат на основе исследованных клеток
     */
    private void updateExploredRoomsFromCells() {
        exploredRooms.clear();

        // Получаем все комнаты из LevelGenerator
        List<Room> allRooms = levelGenerator.getRooms();
        if (allRooms == null) return;

        for (Room room : allRooms) {
            boolean roomExplored = true;

            // Проверяем, все ли клетки комнаты исследованы
            for (int x = room.getX1(); x <= room.getX2(); x++) {
                for (int y = room.getY1(); y <= room.getY2(); y++) {
                    if (!exploredCells.contains(new Position(x, y))) {
                        roomExplored = false;
                        break;
                    }
                }
                if (!roomExplored) break;
            }

            if (roomExplored) {
                exploredRooms.add(room);
            }
        }
    }

    /**
     * Метод для принудительного отображения всей карты (используется при загрузке сохранения)
     */
    public void showFullMapForLoadedGame() {
        // Очищаем только visibleCells, но сохраняем exploredCells
        visibleCells.clear();

        // Добавляем ВСЕ исследованные клетки в видимые
        visibleCells.addAll(exploredCells);

        // Также добавляем текущую комнату целиком, если она есть
        if (currentRoom != null) {
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    visibleCells.add(new Position(x, y));
                    exploredCells.add(new Position(x, y));
                }
            }
            exploredRooms.add(currentRoom);
        }
    }

    /**
     * Метод для пометки всей карты как исследованной (при загрузке сохранения)
     */
    public void markAllAsExplored(char[][] map) {
        if (map == null) return;

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                // Помечаем все НЕ пустые клетки как исследованные
                if (map[y][x] != ' ') {
                    exploredCells.add(new Position(x, y));
                }
            }
        }

        // Также помечаем все комнаты как исследованные
        List<Room> allRooms = levelGenerator.getRooms();
        if (allRooms != null) {
            exploredRooms.addAll(allRooms);
        }
    }

    /**
     * Возвращает все исследованные клетки (для сохранения)
     */
    public Set<Position> getAllExploredCells() {
        return new HashSet<>(exploredCells);
    }

    /**
     * Возвращает все исследованные комнаты (для сохранения)
     */
    public Set<Room> getAllExploredRooms() {
        return new HashSet<>(exploredRooms);
    }

    /**
     * Восстанавливает исследованные клетки из сохранения
     */
    public void restoreExploredCells(Set<Position> cells) {
        exploredCells.clear();
        exploredCells.addAll(cells);
    }

    /**
     * Восстанавливает исследованные комнаты из сохранения
     */
    public void restoreExploredRooms(Set<Room> rooms) {
        exploredRooms.clear();
        exploredRooms.addAll(rooms);
    }
}
