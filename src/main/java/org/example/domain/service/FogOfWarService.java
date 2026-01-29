package org.example.domain.service;

import org.example.config.GameConstants;
import org.example.domain.factory.LevelGenerator;
import org.example.domain.model.Position;
import org.example.domain.model.Room;

import java.util.*;

/**
 * Сервис "тумана войны" - скрывает монстров и артефакты в невидимых областях.
 * Использует ray casting для определения видимости клеток.
 */
public class FogOfWarService {

    // Множество всех видимых клеток (включая затемненные)
    private final Set<Position> visibleCells = new HashSet<>();

    // Множество исследованных клеток (остаются видимыми как затемненные)
    private final Set<Position> exploredCells = new HashSet<>();

    // Множество исследованных комнат
    private final Set<Room> exploredRooms = new HashSet<>();

    // Текущая комната игрока
    private Room currentRoom = null;

    // Клетки, видимые в данный момент (яркие)
    private final Set<Position> currentVisibleCells = new HashSet<>();

    // Константа радиуса обзора
    private static final int VISION_RADIUS = GameConstants.Map.VISION_RADIUS;

    // Генератор уровня для получения информации о комнатах
    private final LevelGenerator levelGenerator;

    public FogOfWarService(LevelGenerator levelGenerator) {
        this.levelGenerator = levelGenerator;
    }

    /**
     * Обновить видимость после перемещения игрока
     */
    public void updateVisibility(Position playerPos, char[][] map) {
        if (map == null || playerPos == null) return;

        // Очищаем временные данные
        visibleCells.clear();
        currentVisibleCells.clear();

        int playerX = playerPos.getX();
        int playerY = playerPos.getY();

        // Проверка границ карты
        if (playerY < 0 || playerY >= map.length ||
                playerX < 0 || playerX >= map[playerY].length) {
            return;
        }

        // 1. Определяем текущую комнату игрока
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);

        // 2. Если игрок в комнате - видна вся комната
        if (currentRoom != null) {
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    if (y >= 0 && y < map.length && x >= 0 && x < map[y].length) {
                        Position pos = new Position(x, y);
                        visibleCells.add(pos);
                        currentVisibleCells.add(pos);
                        exploredCells.add(pos);
                    }
                }
            }
            exploredRooms.add(currentRoom);
        }

        // НОВЫЙ КОД - Брезенхэм по периметру квадрата видимости
        int r = VISION_RADIUS;

        // Верхняя и нижняя границы (включая углы)
        for (int dx = -r; dx <= r; dx++) {
            castRayBresenham(playerX, playerY, playerX + dx, playerY - r, map);
            castRayBresenham(playerX, playerY, playerX + dx, playerY + r, map);
        }

        // Левая и правая границы (без углов, чтобы не дублировать)
        for (int dy = -r + 1; dy <= r - 1; dy++) {
            castRayBresenham(playerX, playerY, playerX - r, playerY + dy, map);
            castRayBresenham(playerX, playerY, playerX + r, playerY + dy, map);
        }

        // 4. Объединяем видимые клетки
        visibleCells.addAll(currentVisibleCells);

        // 5. Добавляем текущие видимые клетки в исследованные
        exploredCells.addAll(visibleCells);
    }

    /**
     * Метод для обновления видимости при загрузке сохранения
     */
    public void updateForLoadedGame(Position playerPos, char[][] map) {
        if (map == null || playerPos == null) return;

        visibleCells.clear();
        currentVisibleCells.clear();

        // 1. Сначала добавляем всё, что игрок уже когда-то видел (память)
        visibleCells.addAll(exploredCells);

        // 2. Определяем текущую комнату и яркость
        int playerX = playerPos.getX();
        int playerY = playerPos.getY();
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);

        if (currentRoom != null) {
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    Position pos = new Position(x, y);
                    currentVisibleCells.add(pos);
                    exploredCells.add(pos);
                }
            }
        }

        // 3. Пускаем лучи для текущего обзора
        int r = VISION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            castRayBresenham(playerX, playerY, playerX + dx, playerY - r, map);
            castRayBresenham(playerX, playerY, playerX + dx, playerY + r, map);
        }
        for (int dy = -r + 1; dy <= r - 1; dy++) {
            castRayBresenham(playerX, playerY, playerX - r, playerY + dy, map);
            castRayBresenham(playerX, playerY, playerX + r, playerY + dy, map);
        }

        visibleCells.addAll(currentVisibleCells);
    }

    /**
     * Целочисленный алгоритм Брезенхэма для луча
     * Идет от (x0,y0) до (x1,y1), добавляя клетки в видимые
     * Останавливается на стенах и границах карты
     */
    private void castRayBresenham(int x0, int y0, int x1, int y1, char[][] map) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        // Проверяем, в комнате ли игрок (для логики с дверями)
        Room playerRoom = levelGenerator.getRoomAt(x0, y0);

        while (true) {
            // Границы карты
            if (x < 0 || y < 0 || y >= map.length || x >= map[y].length) break;

            Position pos = new Position(x, y);

            // Добавляем клетку как видимую
            addVisibleCell(pos);

            char cell = map[y][x];

            // Проверка препятствий
            if (cell == '|' || cell == '~' || cell == ' ') {
                break;
            }

            // Особая обработка дверей - видимость в соседнюю комнату
//            if (cell == '+' && playerRoom != null) {
//                Room adjacent = findAdjacentRoom(x, y);
//                if (adjacent != null && adjacent != currentRoom) {
//                    // Добавляем несколько клеток комнаты в направлении луча
//                    addRoomSliceThroughDoor(x, y, sx, sy, adjacent);
//                }
//            }

            // Достигли конца луча
            if (x == x1 && y == y1) break;

            // Шаг Брезенхэма
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /**
     * Добавляет видимость в комнату через дверь
     */
    private void addRoomSliceThroughDoor(int doorX, int doorY, int dirX, int dirY, Room room) {
        // Идем от двери вглубь комнаты до 5 шагов или до границы комнаты
        for (int i = 1; i <= 5; i++) {
            int nx = doorX + dirX * i;
            int ny = doorY + dirY * i;

            if (nx >= room.getX1() && nx <= room.getX2() &&
                    ny >= room.getY1() && ny <= room.getY2()) {
                addVisibleCell(new Position(nx, ny));
            } else {
                break;
            }
        }
    }

    /**
     * Добавляет видимую клетку в соответствующее множество
     */
    private void addVisibleCell(Position pos) {
        currentVisibleCells.add(pos);
    }

    /**
     * Находит комнату, прилегающую к указанным координатам
     */
    private Room findAdjacentRoom(int doorX, int doorY) {
        // Проверяем все 8 направлений вокруг двери
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Room room = levelGenerator.getRoomAt(doorX + dx, doorY + dy);
                if (room != null) return room;
            }
        }
        return null;
    }

    /**
     * Проверяет, видима ли клетка (включая затемненные)
     */
    public boolean isVisible(int x, int y) {
        return visibleCells.contains(new Position(x, y));
    }

    /**
     * Помечает клетку как исследованную
     */
    public void markCellAsExplored(int x, int y) {
        exploredCells.add(new Position(x, y));
    }

    /**
     * Сбрасывает все данные тумана войны
     */
    public void reset() {
        visibleCells.clear();
        exploredCells.clear();
        exploredRooms.clear();
        currentVisibleCells.clear();
    }

    /**
     * Возвращает неизменяемое множество всех исследованных клеток
     */
    public Set<Position> getAllExploredCells() {
        return Collections.unmodifiableSet(exploredCells);
    }

    /**
     * Возвращает неизменяемое множество всех исследованных комнат
     */
    public Set<Room> getAllExploredRooms() {
        return Collections.unmodifiableSet(exploredRooms);
    }

    /**
     * Возвращает неизменяемое множество клеток, видимых в данный момент (ярких)
     */
    public Set<Position> getCurrentVisibleCells() {
        return Collections.unmodifiableSet(currentVisibleCells);
    }

    /**
     * Восстанавливает исследованные клетки из сохранения
     */
    public void restoreExploredCells(Set<Position> cells) {
        if (cells == null) {
            exploredCells.clear();
            return;
        }
        exploredCells.clear();
        exploredCells.addAll(cells);
    }

    /**
     * Восстанавливает исследованные комнаты из сохранения
     */
    public void restoreExploredRooms(Set<Room> rooms) {
        if (rooms == null) return;
        exploredRooms.clear();
        exploredRooms.addAll(rooms);
    }
}