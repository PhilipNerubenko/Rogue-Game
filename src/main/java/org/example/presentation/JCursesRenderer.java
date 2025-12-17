package org.example.presentation;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;
import org.example.domain.entity.Player;
import org.example.domain.model.Level;
import org.example.domain.model.Room;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.LevelGenerator;

/**
 * Реализация интерфейса Renderer на базе JCurses.
 * ЕДИНСТВЕННЫЙ класс, который знает про Toolkit.
 */
public class JCursesRenderer implements Renderer {

    private final int width;
    private final int height;
    private final CharColor defaultColor;
    private static final int MAP_OFFSET_X = GameConstants.Map.MAP_OFFSET_X;
    private static final int MAP_OFFSET_Y = GameConstants.Map.MAP_OFFSET_Y;

    public JCursesRenderer() {
        this.width = GameConstants.Map.WIDTH;
        this.height = GameConstants.Map.HEIGHT;
        this.defaultColor = new CharColor(CharColor.BLACK, CharColor.BLACK);
        Toolkit.init();
        Toolkit.clearScreen(defaultColor);
    }





    @Override
    public void drawChar(int x, int y, char symbol, int color) {
        // Преобразуем int цвет в CharColor
        Toolkit.printString(
                String.valueOf(symbol),
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y ,
                new CharColor(CharColor.BLACK, (short) color)
        );
    }

    @Override
    public void drawString(int x, int y, String text, int color) {
        Toolkit.printString(
                text,
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y,
                new CharColor(CharColor.BLACK, (short) color)
        );
    }

    @Override
    public void clearScreen() {
        Toolkit.clearScreen(defaultColor);
    }

    @Override
    public void refresh() {
        // JCurses не требует explicit refresh, но оставим для совместимости
    }

    public void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, Level level) {
        fog.updateVisibility(player.getPosition(), map);

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                char tile = map[y][x];
                Room room = level.getRoomAt(x, y); // ← Теперь используем levelGen
                boolean visible = fog.isVisible(x, y);
                boolean explored = fog.isExplored(x, y);

                if (visible) {
                    // ВИДИМАЯ КЛЕТКА — рисуем всё ярко
                    short color = getTileColorVisible(tile);
                    drawChar(x, y, tile, color);
               // } else if (explored && (tile == '|' || tile == '~')) {
                } else if (explored ) {
                    // ИССЛЕДОВАННАЯ СТЕНА — тусклый синий (как тёмно-серый)
//                    drawChar(x, y, tile, CharColor.BLUE); // ✓ РАБОЧИЙ ЦВЕТ
                    short color = getTileColorExplored(tile);
                    if (color != CharColor.BLACK) {
                        drawChar(x, y, tile, color);
                    } else {
                        drawChar(x, y, ' ', color);
                    }
                } else {
                    // Неизведанная или исследованный пол — чёрная
                    drawChar(x, y, ' ', CharColor.BLACK);
                }
            }
        }
    }

    /**
     * Цвета для ВИДИМЫХ клеток (текущая комната)
     */
    private short getTileColorVisible(char tile) {
        return switch (tile) {
            case '.' -> CharColor.CYAN;           // Пол — голубой
            case '|', '~' -> CharColor.WHITE;    // Стены — ЯРКО-БЕЛЫЕ
            case '#' -> CharColor.WHITE;        // Коридор — белый для проверки
           // case '#' -> CharColor.YELLOW;        // Коридор — жёлтый
            case '+' -> CharColor.MAGENTA;       // Дверь — пурпурная
            case '$' -> CharColor.GREEN;         // Сокровище — зелёное
            default -> CharColor.WHITE;
        };
    }

    private short getTileColorExplored(char tile) {
        return switch (tile) {
            case '#', '+' -> CharColor.YELLOW;   // Коридоры и двери — тускло-жёлтые
            case '|', '~' -> CharColor.BLUE;     // Стены — тускло-синие
            case '$' -> CharColor.GREEN;         // Сокровища — тускло-зелёные
            default -> CharColor.BLACK;          // Пол и всё остальное — скрыто
        };
    }

    @Override
    public void drawStatusBar(int playerHealth, int maxHealth, int level, int treasures) {
        String status = String.format(
                "HP: %d/%d | Уровень: %d | Сокровища: %d",
                playerHealth, maxHealth, level, treasures
        );
        drawString(3, GameConstants.Map.HEIGHT + 1, status, CharColor.CYAN);
    }

    @Override
    public void drawInventory(Inventory inventory) {
        int startY = 5;
        int startX = GameConstants.Map.WIDTH + 10;

        drawString(startX, startY, "=== РЮКЗАК ===", CharColor.WHITE);

        int line = startY + 2;
        for (ItemType type : ItemType.values()) {
            int count = inventory.count(type);
            if (count > 0) {
                String text = String.format("%s: %d/9", type.name(), count);
                drawString(startX, line++, text, CharColor.YELLOW);
            }
        }
    }

    @Override
    public void drawMessage(int line, String message, int color) {
        clearLine(line); // Стираем старое сообщение
        drawString(3, line, message, color);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void clearLine(int y) {
        drawString(3, y, " ".repeat(width), CharColor.BLACK);
    }

    @Override
    public void shutdown() {
        Toolkit.shutdown();
        System.out.print("\033[?25h"); // Показать курсор
    }
}