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
    private Set<Room> exploredRooms = new HashSet<>();
    private Room currentRoom = null;
    private Set<Position> currentVisibleCells = new HashSet<>();

    private static final int VISION_RADIUS = GameConstants.Map.VISION_RADIUS;
    private LevelGenerator levelGenerator;

    public FogOfWarService(LevelGenerator levelGenerator) {
        this.levelGenerator = levelGenerator;
    }

    /**
     * Обновить видимость после перемещения игрока
     */
    public void updateVisibility(Position playerPos, char[][] map) {
        if (map == null || playerPos == null) return;

        visibleCells.clear();
        currentVisibleCells.clear();

        int playerX = playerPos.getX();
        int playerY = playerPos.getY();

        // Проверка границ
        if (playerY < 0 || playerY >= map.length ||
                playerX < 0 || playerX >= map[playerY].length) {
            return;
        }

        // 1. Определяем текущую комнату
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);

        // 2. Если игрок в комнате
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

        // 3. RAY CASTING
        for (int angle = 0; angle < 360; angle += 5) {
            double radian = Math.toRadians(angle);
            castRay(playerX, playerY, Math.cos(radian), Math.sin(radian), map, true);
        }

        // 4. Добавляем в общие видимые
        visibleCells.addAll(currentVisibleCells);

        // 5. Добавляем в исследованные
        exploredCells.addAll(visibleCells);
    }

    /**
     * Метод для обновления видимости при загрузке сохранения
     */
    public void updateForLoadedGame(Position playerPos, char[][] map) {
//        System.out.println("=== FOG OF WAR: UPDATING FOR LOADED GAME ===");

        if (map == null || playerPos == null) {
            System.err.println("[FogOfWar] ERROR: Invalid parameters!");
            return;
        }

//        System.out.println("[FogOfWar] Player position: (" + playerPos.getX() + ", " + playerPos.getY() + ")");
//        System.out.println("[FogOfWar] Map dimensions: " + map.length + "x" + (map.length > 0 ? map[0].length : 0));
//        System.out.println("[FogOfWar] Explored cells before update: " + exploredCells.size());
//        System.out.println("[FogOfWar] Explored rooms before update: " + exploredRooms.size());

        // Очищаем ТОЛЬКО видимые клетки
        visibleCells.clear();
        currentVisibleCells.clear();

        int playerX = playerPos.getX();
        int playerY = playerPos.getY();

        // Определяем текущую комнату
        currentRoom = levelGenerator.getRoomAt(playerX, playerY);
//        System.out.println("[FogOfWar] Current room: " + (currentRoom != null ?
//                "(" + currentRoom.getX1() + "," + currentRoom.getY1() + ")" : "null"));

        // Сначала добавляем ВСЕ исследованные клетки в visibleCells
//        System.out.println("[FogOfWar] Adding all explored cells to visible cells...");
        visibleCells.addAll(exploredCells);
//        System.out.println("[FogOfWar] Visible cells after adding explored: " + visibleCells.size());

        // Теперь вычисляем, что видно СЕЙЧАС (для ярких цветов)
        if (currentRoom != null) {
//            System.out.println("[FogOfWar] Player is in room, adding entire room to current visible...");
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    if (y >= 0 && y < map.length && x >= 0 && x < map[y].length) {
                        Position pos = new Position(x, y);
                        currentVisibleCells.add(pos);
                        // Убедимся, что клетка также в видимых
                        visibleCells.add(pos);
                        // И в исследованных (на случай если не была)
                        exploredCells.add(pos);
                    }
                }
            }
            exploredRooms.add(currentRoom);
        }

        // RAY CASTING для коридоров
//        System.out.println("[FogOfWar] Performing ray casting...");
        for (int angle = 0; angle < 360; angle += 5) {
            double radian = Math.toRadians(angle);
            castRay(playerX, playerY, Math.cos(radian), Math.sin(radian), map, true);
        }

//        System.out.println("[FogOfWar] After update:");
//        System.out.println("[FogOfWar]   Current visible cells: " + currentVisibleCells.size());
//        System.out.println("[FogOfWar]   All visible cells: " + visibleCells.size());
//        System.out.println("[FogOfWar]   Explored cells: " + exploredCells.size());
//        System.out.println("[FogOfWar]   Explored rooms: " + exploredRooms.size());

        // Проверка: все ли клетки текущей комнаты в exploredCells
        if (currentRoom != null) {
            int roomCells = 0;
            int exploredInRoom = 0;
            for (int x = currentRoom.getX1(); x <= currentRoom.getX2(); x++) {
                for (int y = currentRoom.getY1(); y <= currentRoom.getY2(); y++) {
                    roomCells++;
                    if (exploredCells.contains(new Position(x, y))) {
                        exploredInRoom++;
                    }
                }
            }
//            System.out.println("[FogOfWar] Room check: " + exploredInRoom + "/" + roomCells + " cells explored");
        }

//        System.out.println("=== FOG OF WAR: UPDATE COMPLETE ===\n");
    }

    private void castRay(int startX, int startY, double dx, double dy, char[][] map, boolean forCurrentVisibility) {
        if (map == null || map.length == 0) return;

        double x = startX;
        double y = startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return;

        double stepX = dx / length;
        double stepY = dy / length;

        Room playerInRoom = levelGenerator.getRoomAt(startX, startY);

        for (int i = 0; i < VISION_RADIUS; i++) {
            x += stepX;
            y += stepY;

            int intX = (int) Math.round(x);
            int intY = (int) Math.round(y);

            if (intX < 0 || intY < 0 || intY >= map.length || intX >= map[intY].length) {
                break;
            }

            char cell = map[intY][intX];
            Position pos = new Position(intX, intY);

            if (playerInRoom != null) {
                if (cell == '|' || cell == '~' || cell == ' ') {
                    break;
                }
                if (forCurrentVisibility) {
                    currentVisibleCells.add(pos);
                } else {
                    visibleCells.add(pos);
                }
                continue;
            }

            if (cell == '|' || cell == '~' || cell == ' ') {
                break;
            }

            if (forCurrentVisibility) {
                currentVisibleCells.add(pos);
            } else {
                visibleCells.add(pos);
            }

            if (cell == '+') {
                Room adjacentRoom = findAdjacentRoom(intX, intY);
                if (adjacentRoom != null && adjacentRoom != currentRoom) {
                    addVisibleRoomInterior(intX, intY, stepX, stepY, adjacentRoom, forCurrentVisibility);
                }
                break;
            }
        }
    }

    private void addVisibleRoomInterior(int doorX, int doorY, double dx, double dy, Room room, boolean forCurrentVisibility) {
        for (int depth = 1; depth <= 8; depth++) {
            int viewX = doorX + (int)(dx * depth);
            int viewY = doorY + (int)(dy * depth);

            if (viewX >= room.getX1() && viewX <= room.getX2() &&
                    viewY >= room.getY1() && viewY <= room.getY2()) {
                if (forCurrentVisibility) {
                    currentVisibleCells.add(new Position(viewX, viewY));
                } else {
                    visibleCells.add(new Position(viewX, viewY));
                }
            } else {
                break;
            }
        }
    }

    private Room findAdjacentRoom(int doorX, int doorY) {
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
        exploredRooms.clear();
        currentVisibleCells.clear();
    }

    public Set<Position> getAllExploredCells() {
        return Collections.unmodifiableSet(exploredCells);
    }

    public Set<Room> getAllExploredRooms() {
        return Collections.unmodifiableSet(exploredRooms);
    }

    public Set<Position> getCurrentVisibleCells() {
        return Collections.unmodifiableSet(currentVisibleCells);
    }

    /**
     * Восстанавливает исследованные клетки из сохранения
     */
    public void restoreExploredCells(Set<Position> cells) {
//        System.out.println("=== FOG OF WAR: RESTORING EXPLORED CELLS ===");

        if (cells == null) {
//            System.out.println("[FogOfWar] ERROR: cells set is null!");
            exploredCells.clear();
            return;
        }

//        System.out.println("[FogOfWar] Cells to restore: " + cells.size());

        if (cells.isEmpty()) {
//            System.out.println("[FogOfWar] WARNING: Empty cells set provided!");
        }

        // Очищаем старые данные
        exploredCells.clear();

        // Добавляем новые данные
        exploredCells.addAll(cells);

//        System.out.println("[FogOfWar] Explored cells after restore: " + exploredCells.size());

        // Детальная информация
//        System.out.println("[FogOfWar] Sample restored cells (first 5):");
        int count = 0;
        for (Position pos : exploredCells) {
            if (count++ < 5) {
//                System.out.println("[FogOfWar]   Cell (" + pos.getX() + ", " + pos.getY() + ")");
            }
        }

//        System.out.println("=== FOG OF WAR: EXPLORED CELLS RESTORED ===\n");
    }

    /**
     * Восстанавливает исследованные комнаты из сохранения
     */
    public void restoreExploredRooms(Set<Room> rooms) {
        if (rooms == null) {
//            System.out.println("[FogOfWar] restoreExploredRooms: rooms is null!");
            return;
        }

        exploredRooms.clear();
        exploredRooms.addAll(rooms);
//        System.out.println("[FogOfWar] Restored " + rooms.size() + " explored rooms from save");

        if (rooms.isEmpty()) {
//            System.out.println("[FogOfWar] WARNING: Empty explored rooms set restored!");
        }
    }

    public void debugInfo() {
        System.out.println("=== Fog of War Debug ===");
        System.out.println("Explored cells: " + exploredCells.size());
        System.out.println("Explored rooms: " + exploredRooms.size());
        System.out.println("Visible cells: " + visibleCells.size());
        System.out.println("Current visible cells: " + currentVisibleCells.size());
    }
}