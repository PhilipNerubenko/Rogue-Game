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
    private Set<Position> visibleCells = new HashSet<>();

    // Множество исследованных клеток (остаются видимыми как затемненные)
    private Set<Position> exploredCells = new HashSet<>();

    // Множество исследованных комнат
    private Set<Room> exploredRooms = new HashSet<>();

    // Текущая комната игрока
    private Room currentRoom = null;

    // Клетки, видимые в данный момент (яркие)
    private Set<Position> currentVisibleCells = new HashSet<>();

    // Константа радиуса обзора
    private static final int VISION_RADIUS = GameConstants.Map.VISION_RADIUS;

    // Генератор уровня для получения информации о комнатах
    private LevelGenerator levelGenerator;

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

        // 3. Ray casting для определения видимости в коридорах
        for (int angle = 0; angle < 360; angle += 5) {
            double radian = Math.toRadians(angle);
            castRay(playerX, playerY, Math.cos(radian), Math.sin(radian), map, true);
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

        // Очищаем только видимые клетки (исследованные сохраняем)
        visibleCells.clear();
        currentVisibleCells.clear();

        int playerX = playerPos.getX();
        int playerY = playerPos.getY();

        // Определяем текущую комнату
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);

        // Добавляем все исследованные клетки как видимые (затемненные)
        visibleCells.addAll(exploredCells);

        // Если игрок в комнате - вся комната ярко видима
        if (currentRoom != null) {
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    if (y >= 0 && y < map.length && x >= 0 && x < map[y].length) {
                        Position pos = new Position(x, y);
                        currentVisibleCells.add(pos);
                        visibleCells.add(pos);
                        exploredCells.add(pos);
                    }
                }
            }
            exploredRooms.add(currentRoom);
        }

        // Ray casting для коридоров
        for (int angle = 0; angle < 360; angle += 5) {
            double radian = Math.toRadians(angle);
            castRay(playerX, playerY, Math.cos(radian), Math.sin(radian), map, true);
        }
    }

    /**
     * Бросает луч для определения видимости клеток
     * @param startX начальная координата X
     * @param startY начальная координата Y
     * @param dx направление луча по X
     * @param dy направление луча по Y
     * @param map карта уровня
     * @param forCurrentVisibility true - для текущей видимости, false - для общей
     */
    private void castRay(int startX, int startY, double dx, double dy, char[][] map, boolean forCurrentVisibility) {
        if (map == null || map.length == 0) return;

        double x = startX;
        double y = startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return;

        // Нормализуем вектор направления
        double stepX = dx / length;
        double stepY = dy / length;

        Room playerInRoom = levelGenerator.getRoomAt(startX, startY);

        // Проходим лучом на расстояние радиуса обзора
        for (int i = 0; i < VISION_RADIUS; i++) {
            x += stepX;
            y += stepY;

            int intX = (int) Math.round(x);
            int intY = (int) Math.round(y);

            // Проверка границ карты
            if (intX < 0 || intY < 0 || intY >= map.length || intX >= map[intY].length) {
                break;
            }

            char cell = map[intY][intX];
            Position pos = new Position(intX, intY);

            // Если игрок в комнате
            if (playerInRoom != null) {
                if (cell == '|' || cell == '~' || cell == ' ') {
                    break; // Стена или пустота прерывает луч
                }
                addVisibleCell(pos, forCurrentVisibility);
                continue;
            }

            // Общая проверка препятствий
            if (cell == '|' || cell == '~' || cell == ' ') {
                break;
            }

            addVisibleCell(pos, forCurrentVisibility);

            // Если нашли дверь - добавляем видимость в соседнюю комнату
            if (cell == '+') {
                Room adjacentRoom = findAdjacentRoom(intX, intY);
                if (adjacentRoom != null && adjacentRoom != currentRoom) {
                    addVisibleRoomInterior(intX, intY, stepX, stepY, adjacentRoom, forCurrentVisibility);
                }
                break; // Дверь прерывает луч
            }
        }
    }

    /**
     * Добавляет видимую клетку в соответствующее множество
     */
    private void addVisibleCell(Position pos, boolean forCurrentVisibility) {
        if (forCurrentVisibility) {
            currentVisibleCells.add(pos);
        } else {
            visibleCells.add(pos);
        }
    }

    /**
     * Добавляет видимость внутрь комнаты через дверь
     */
    private void addVisibleRoomInterior(int doorX, int doorY, double dx, double dy, Room room, boolean forCurrentVisibility) {
        // Видимость ограничена глубиной 8 клеток от двери
        for (int depth = 1; depth <= 8; depth++) {
            int viewX = doorX + (int)(dx * depth);
            int viewY = doorY + (int)(dy * depth);

            if (viewX >= room.getX1() && viewX <= room.getX2() &&
                    viewY >= room.getY1() && viewY <= room.getY2()) {
                addVisibleCell(new Position(viewX, viewY), forCurrentVisibility);
            } else {
                break; // Вышли за границы комнаты
            }
        }
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

    /**
     * Выводит отладочную информацию
     */
    public void debugInfo() {
        System.out.println("=== Fog of War Debug ===");
        System.out.println("Explored cells: " + exploredCells.size());
        System.out.println("Explored rooms: " + exploredRooms.size());
        System.out.println("Visible cells: " + visibleCells.size());
        System.out.println("Current visible cells: " + currentVisibleCells.size());
    }
}