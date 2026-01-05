package org.example.presentation;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.datalayer.SessionStat;
import org.example.datalayer.Statistics;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;
import org.example.domain.entity.Player;
import org.example.domain.model.Position;
import org.example.domain.model.Room;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.LevelGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static org.example.config.GameConstants.PathToFiles.SCOREBOARD_PATH;
import static org.example.config.GameConstants.ScreenConfig.SHOW_CURSOR;

public class JCursesRenderer implements Renderer {

    private final int width;
    private final int height;
    private final CharColor defaultColor;
    private static final int MAP_OFFSET_X = GameConstants.Map.MAP_OFFSET_X;
    private static final int MAP_OFFSET_Y = GameConstants.Map.MAP_OFFSET_Y;

    // Цветовые константы JCurses (только 0-7)
    private static final short COLOR_BLACK = CharColor.BLACK;
    private static final short COLOR_RED = CharColor.RED;
    private static final short COLOR_GREEN = CharColor.GREEN;
    private static final short COLOR_YELLOW = CharColor.YELLOW;
    private static final short COLOR_BLUE = CharColor.BLUE;
    private static final short COLOR_MAGENTA = CharColor.MAGENTA;
    private static final short COLOR_CYAN = CharColor.CYAN;
    private static final short COLOR_WHITE = CharColor.WHITE;

    public JCursesRenderer() {
        this.width = GameConstants.Map.WIDTH;
        this.height = GameConstants.Map.HEIGHT;
        this.defaultColor = new CharColor(COLOR_BLACK, COLOR_BLACK);
        Toolkit.init();
        Toolkit.clearScreen(defaultColor);
    }

    @Override
    public void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, LevelGenerator levelGen) {
        if (map == null || fog == null) return;

        // Получаем два набора клеток из FogOfWarService
        Set<Position> allExploredCells = fog.getAllExploredCells();      // ВСЕ исследованные клетки
        Set<Position> currentVisibleCells = fog.getCurrentVisibleCells(); // Что видно ПРЯМО СЕЙЧАС

//        // Отладочная информация
//        System.out.println("[RENDER] Drawing map:");
//        System.out.println("[RENDER]   Total explored cells: " + allExploredCells.size());
//        System.out.println("[RENDER]   Current visible cells: " + currentVisibleCells.size());
//        System.out.println("[RENDER]   Map size: " + map.length + "x" + (map.length > 0 ? map[0].length : 0));

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                char tile = map[y][x];
                Position pos = new Position(x, y);

                // Пустые клетки всегда черные
                if (tile == ' ') {
                    drawChar(x, y, ' ', COLOR_BLACK);
                    continue;
                }

                boolean isExplored = allExploredCells.contains(pos);
                boolean isCurrentlyVisible = currentVisibleCells.contains(pos);

                if (!isExplored) {
                    // Не исследовано - черная клетка
                    drawChar(x, y, ' ', COLOR_BLACK);
                } else if (isCurrentlyVisible) {
                    // Исследовано и видно сейчас - ЯРКИЕ цвета
                    short color = getBrightTileColor(tile);
                    drawChar(x, y, tile, color);
                } else {
                    // Исследовано, но не видно сейчас - ТУСКЛЫЕ цвета
                    short color = getDimTileColor(tile);
                    if (color != -1) {
                        drawChar(x, y, tile, color);
                    } else {
                        // Для некоторых символов (враги, игрок) - не показываем если не видны
                        drawChar(x, y, ' ', COLOR_BLACK);
                    }
                }
            }
        }

//        System.out.println("[RENDER] Map drawn successfully");
    }

    /**
     * ЯРКИЕ цвета для клеток, видимых прямо сейчас
     */
    private short getBrightTileColor(char tile) {
        return switch (tile) {
            case '.' -> COLOR_CYAN;           // Пол - яркий голубой
            case '|', '~' -> COLOR_WHITE;     // Стены - ярко-белые
            case '#' -> COLOR_YELLOW;         // Коридор - ярко-желтый
            case '+' -> COLOR_MAGENTA;        // Дверь - ярко-пурпурная
            case '$' -> COLOR_GREEN;          // Сокровище - ярко-зеленое
            case ',' -> COLOR_GREEN;          // Еда - ярко-зеленая
            case '!' -> COLOR_BLUE;           // Эликсир - ярко-синий
            case '?' -> COLOR_MAGENTA;        // Свиток - ярко-пурпурный
            case ')' -> COLOR_RED;            // Оружие - ярко-красное

            // Существа (только если видны сейчас)
            case 'z' -> COLOR_GREEN;          // Зомби
            case 'v' -> COLOR_RED;            // Вампир
            case 'g' -> COLOR_WHITE;          // Призрак
            case 'o' -> COLOR_YELLOW;         // Огр
            case 's' -> COLOR_CYAN;           // Змеиный маг
            case '@' -> COLOR_YELLOW;         // Игрок

            // Выход
            case 'E', '⇧' -> COLOR_GREEN;     // Выход

            default -> COLOR_WHITE;
        };
    }

    /**
     * ТУСКЛЫЕ цвета для исследованных, но не видимых сейчас клеток
     */
    private short getDimTileColor(char tile) {
        return switch (tile) {
            // Стены и пол (тусклые)
            case '.' -> COLOR_BLUE;           // Пол - темно-синий
            case '|', '~' -> COLOR_BLUE;      // Стены - темно-синие
            case '#' -> COLOR_BLUE;           // Коридор - темно-синий
            case '+' -> COLOR_BLUE;           // Дверь - темно-синяя

            // Предметы (тусклые)
            case '$' -> COLOR_BLUE;           // Сокровище - темно-синее
            case ',' -> COLOR_BLUE;           // Еда - темно-синяя
            case '!' -> COLOR_BLUE;           // Эликсир - темно-синий
            case '?' -> COLOR_BLUE;           // Свиток - темно-синий
            case ')' -> COLOR_BLUE;           // Оружие - темно-синее

            // Существа - НЕ показываем если не видны сейчас
            case 'z', 'v', 'g', 'o', 's', '@' -> -1;

            // Выход
            case 'E', '⇧' -> COLOR_BLUE;      // Выход - темно-синий

            default -> COLOR_BLUE;            // По умолчанию темно-синий
        };
    }

    @Override
    public void drawChar(int x, int y, char symbol, int color) {
        // Проверяем цвет
        short safeColor = (short) color;
        if (color < 0 || color > 7) {
            safeColor = COLOR_WHITE;
        }

        Toolkit.printString(
                String.valueOf(symbol),
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y,
                new CharColor(COLOR_BLACK, safeColor)
        );
    }

    @Override
    public void drawString(int x, int y, String text, int color) {
        short safeColor = (short) color;
        if (color < 0 || color > 7) {
            safeColor = COLOR_WHITE;
        }

        Toolkit.printString(
                text,
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y,
                new CharColor(COLOR_BLACK, safeColor)
        );
    }

    @Override
    public void clearScreen() {
        Toolkit.clearScreen(defaultColor);
    }

    @Override
    public void refresh() {
        // JCurses не требует explicit refresh
    }

    @Override
    public void drawStatusBar(int playerHealth, int maxHealth, int pX, int pY, int level, int treasures) {
        String status = String.format(
                "HP: %d/%d | Pos: %d,%d | Level: %d | $: %d",
                playerHealth, maxHealth, pX, pY, level, treasures
        );
        drawString(3, GameConstants.Map.HEIGHT + 1, status, COLOR_CYAN);
    }

    @Override
    public void drawInventory(Inventory inventory) {
        int startY = 5;
        int startX = GameConstants.Map.WIDTH + 10;

        drawString(startX, startY, "=== BACKPACK ===", COLOR_WHITE);

        int line = startY + 2;
        for (ItemType type : ItemType.values()) {
            int count = inventory.count(type);
            if (count > 0) {
                String text = String.format("%s: %d/9", type.name(), count);
                drawString(startX, line++, text, COLOR_YELLOW);
            }
        }
    }

    @Override
    public void drawMessage(int line, String message, int color) {
        clearLine(line);
        short safeColor = (short) Math.max(COLOR_BLACK, Math.min(color, COLOR_WHITE));
        drawString(3, line, message, safeColor);
    }

    @Override
    public void drawMenuScreen(int currentOption) {
        clearScreen();

        String[] strings = {
                "           GAME  MENU           ",
                "+------------------------------+",
                "|                              |",
                "|          NEW   GAME          |",
                "|          LOAD  GAME          |",
                "|          SCOREBOARD          |",
                "|          EXIT  GAME          |",
                "|                              |",
                "+------------------------------+",
        };

        int menuWidth = strings[0].length();
        int menuHeight = strings.length;

        int screenWidth = Toolkit.getScreenWidth();
        int screenHeight = Toolkit.getScreenHeight();

        int shiftX = (screenWidth - menuWidth) / 2;
        int shiftY = (screenHeight - menuHeight) / 2;

        CharColor menuColor = new CharColor(COLOR_BLACK, COLOR_WHITE);
        for (int i = 0; i < menuHeight; i++) {
            Toolkit.printString(strings[i], shiftX, shiftY + i, menuColor);
        }

        int optionRow = shiftY + 3 + currentOption;
        CharColor pointerColor = new CharColor(COLOR_BLACK, COLOR_YELLOW);

        Toolkit.printString("<<<", shiftX + 5, optionRow, pointerColor);
        Toolkit.printString(">>>", shiftX + 24, optionRow, pointerColor);
    }

    @Override
    public void drawScoreboard() {
        clearScreen();

        java.util.List<SessionStat> stats = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(SCOREBOARD_PATH);

            if (!file.exists()) {
                drawString(5, 10, "Scoreboard file not found!", COLOR_YELLOW);
                drawString(5, 11, "Play a game first to create it.", COLOR_WHITE);
                drawString(5, 13, "Press any key...", COLOR_YELLOW);
                refresh();
                Toolkit.readCharacter();
                return;
            }

            JsonNode root = mapper.readTree(file);
            JsonNode sessionNode = root.get("sessionStats");

            if (sessionNode != null && sessionNode.isArray()) {
                SessionStat[] statArray = mapper.treeToValue(sessionNode, SessionStat[].class);
                stats = Arrays.asList(statArray);
            }

        } catch (IOException e) {
            System.err.println("Error loading scoreboard from: " + SCOREBOARD_PATH);
            e.printStackTrace();

            drawString(5, 10, "Error loading scoreboard!", COLOR_RED);
            drawString(5, 11, "File: " + SCOREBOARD_PATH, COLOR_RED);
            drawString(5, 13, "Press any key to continue...", COLOR_YELLOW);
            refresh();
            Toolkit.readCharacter();
            return;
        }

        String[] headers = {"treasures", "level", "enemies", "food", "elixirs", "scrolls", "attacks", "missed", "moves"};
        int startX = 0;
        int startY = 1;
        int cellWidth = 10;

        String border = "-".repeat(91);
        String separatorTemplate = "|" + "         |".repeat(headers.length);

        int currentY = startY;

        drawString(startX, currentY++, border, COLOR_WHITE);

        StringBuilder headerLine = new StringBuilder("|");
        for (String header : headers) {
            headerLine.append(String.format("%-9s", header)).append("|");
        }
        drawString(startX, currentY++, headerLine.toString(), COLOR_WHITE);

        drawString(startX, currentY++, border, COLOR_WHITE);

        for (int i = 0; i < Math.min(stats.size(), 10); i++) {
            SessionStat s = stats.get(i);

            drawString(startX, currentY, separatorTemplate, COLOR_WHITE);

            int dataX = startX + 1;

            drawString(dataX + 0, currentY, String.format("%9d", s.getTreasures()), COLOR_CYAN);
            drawString(dataX + 10, currentY, String.format("%9d", s.getLevelNum()), COLOR_CYAN);
            drawString(dataX + 20, currentY, String.format("%9d", s.getEnemies()), COLOR_CYAN);
            drawString(dataX + 30, currentY, String.format("%9d", s.getFood()), COLOR_CYAN);
            drawString(dataX + 40, currentY, String.format("%9d", s.getElixirs()), COLOR_CYAN);
            drawString(dataX + 50, currentY, String.format("%9d", s.getScrolls()), COLOR_CYAN);
            drawString(dataX + 60, currentY, String.format("%9d", s.getAttacks()), COLOR_CYAN);
            drawString(dataX + 70, currentY, String.format("%9d", s.getMissed()), COLOR_CYAN);
            drawString(dataX + 80, currentY, String.format("%9d", s.getMoves()), COLOR_CYAN);

            currentY++;
            drawString(startX, currentY++, border, COLOR_WHITE);
        }

        String message = "Press ESC to return...";
        int messageX = startX + (border.length() - message.length()) / 2;
        drawString(messageX, currentY + 1, message, COLOR_YELLOW);

        refresh();
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
        drawString(3, y, " ".repeat(width), COLOR_BLACK);
    }

    @Override
    public void shutdown() {
        Toolkit.shutdown();
        System.out.print(SHOW_CURSOR);
    }
}