package org.example.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;
import org.example.domain.entity.Player;
import org.example.domain.model.Position;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.LevelGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.example.config.GameConstants.PathToFiles.SCOREBOARD_PATH;
import static org.example.config.GameConstants.ScreenConfig.SHOW_CURSOR;

/**
 * Реализация интерфейса Renderer для библиотеки JCurses.
 * Отвечает за отрисовку игрового интерфейса в терминале.
 */
public class JCursesRenderer implements Renderer {

    // Размеры игровой карты
    private final int width;
    private final int height;
    private final CharColor defaultColor;

    // Смещение для отрисовки карты на экране
    private static final int MAP_OFFSET_X = GameConstants.Map.MAP_OFFSET_X;
    private static final int MAP_OFFSET_Y = GameConstants.Map.MAP_OFFSET_Y;

    // Цветовые константы JCurses (ограничены палитрой 0-7)
    private static final short COLOR_BLACK = CharColor.BLACK;
    private static final short COLOR_RED = CharColor.RED;
    private static final short COLOR_GREEN = CharColor.GREEN;
    private static final short COLOR_YELLOW = CharColor.YELLOW;
    private static final short COLOR_BLUE = CharColor.BLUE;
    private static final short COLOR_MAGENTA = CharColor.MAGENTA;
    private static final short COLOR_CYAN = CharColor.CYAN;
    private static final short COLOR_WHITE = CharColor.WHITE;

    /**
     * Конструктор инициализирует рендерер и библиотеку JCurses.
     */
    public JCursesRenderer() {
        this.width = GameConstants.Map.WIDTH;
        this.height = GameConstants.Map.HEIGHT;
        this.defaultColor = new CharColor(COLOR_BLACK, COLOR_BLACK);
        Toolkit.init();
        Toolkit.clearScreen(defaultColor);
    }

    /**
     * Отрисовывает карту с учетом "тумана войны".
     *
     * @param map игровая карта
     * @param player текущий игрок
     * @param fog сервис тумана войны
     * @param levelGen генератор уровней
     */
    @Override
    public void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, LevelGenerator levelGen) {
        if (map == null || fog == null) return;

        Set<Position> allExploredCells = fog.getAllExploredCells();      // Все исследованные клетки
        Set<Position> currentVisibleCells = fog.getCurrentVisibleCells(); // Клетки, видимые в текущий момент

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
                    drawChar(x, y, 'b', COLOR_WHITE);
                } else if (isCurrentlyVisible) {
                    // Видно сейчас - яркие цвета
                    short color = getBrightTileColor(tile);
                    drawChar(x, y, tile, color);
                } else {
                    // Исследовано, но не видно сейчас - тусклые цвета
                    short color = getDimTileColor(tile);
                    if (color != -1) {
                        drawChar(x, y, tile, color);
                    } else {
                        // Существа и игрок скрываются, если не видны
                        drawChar(x, y, ' ', COLOR_BLACK);
                    }
                }
            }
        }
    }

    /**
     * Возвращает яркие цвета для клеток, видимых в текущий момент.
     *
     * @param tile символ клетки
     * @return цвет для отрисовки
     */
    private short getBrightTileColor(char tile) {
        return switch (tile) {
            case '.' -> COLOR_CYAN;           // Пол
            case '|', '~' -> COLOR_WHITE;     // Стены
            case '#' -> COLOR_YELLOW;         // Коридор
            case '+' -> COLOR_MAGENTA;        // Дверь
            case '$' -> COLOR_GREEN;          // Сокровище
            case ',' -> COLOR_GREEN;          // Еда
            case '!' -> COLOR_BLUE;           // Эликсир
            case '?' -> COLOR_MAGENTA;        // Свиток
            case ')' -> COLOR_RED;            // Оружие

            // Существа
            case 'z' -> COLOR_GREEN;          // Зомби
            case 'v' -> COLOR_RED;            // Вампир
            case 'g' -> COLOR_WHITE;          // Призрак
            case 'o' -> COLOR_YELLOW;         // Огр
            case 's' -> COLOR_CYAN;           // Змеиный маг
            case '@' -> COLOR_YELLOW;         // Игрок

            // Выход
            case 'E', '⇧' -> COLOR_GREEN;

            default -> COLOR_WHITE;
        };
    }

    /**
     * Возвращает тусклые цвета для исследованных, но не видимых клеток.
     *
     * @param tile символ клетки
     * @return цвет для отрисовки или -1 если объект не должен отображаться
     */
    private short getDimTileColor(char tile) {
        return switch (tile) {
            // Стены и пол
            case '.', '|', '~', '#', '+' -> COLOR_BLUE;

            // Предметы
            case '$', ',', '!', '?', ')' -> COLOR_BLUE;

            // Существа и игрок - скрываем
            case 'z', 'v', 'g', 'o', 's', '@' -> -1;

            // Выход
            case 'E', '⇧' -> COLOR_BLUE;

            default -> COLOR_BLUE;
        };
    }

    /**
     * Отрисовывает символ в указанной позиции.
     *
     * @param x координата X
     * @param y координата Y
     * @param symbol символ для отрисовки
     * @param color цвет символа
     */
    @Override
    public void drawChar(int x, int y, char symbol, int color) {
        // Валидация цвета в пределах допустимого диапазона
        short safeColor = (short) Math.max(COLOR_BLACK, Math.min(color, COLOR_WHITE));

        Toolkit.printString(
                String.valueOf(symbol),
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y,
                new CharColor(COLOR_BLACK, safeColor)
        );
    }

    /**
     * Отрисовывает строку в указанной позиции.
     *
     * @param x координата X
     * @param y координата Y
     * @param text текст для отрисовки
     * @param color цвет текста
     */
    @Override
    public void drawString(int x, int y, String text, int color) {
        short safeColor = (short) Math.max(COLOR_BLACK, Math.min(color, COLOR_WHITE));

        Toolkit.printString(
                text,
                x + MAP_OFFSET_X,
                y + MAP_OFFSET_Y,
                new CharColor(COLOR_BLACK, safeColor)
        );
    }

    /**
     * Очищает весь экран.
     */
    @Override
    public void clearScreen() {
        Toolkit.clearScreen(defaultColor);
    }

    /**
     * Обновляет экран (для JCurses не требуется явного обновления).
     */
    @Override
    public void refresh() {
        // JCurses не требует явного обновления экрана
    }

    /**
     * Отрисовывает статусную строку с информацией об игроке.
     *
     * @param playerHealth текущее здоровье
     * @param maxHealth максимальное здоровье
     * @param pX координата X игрока
     * @param pY координата Y игрока
     * @param level текущий уровень
     * @param treasures количество сокровищ
     */
    @Override
    public void drawStatusBar(int playerHealth, int maxHealth, int pX, int pY, int level, int treasures) {
        String status = String.format(
                "HP: %d/%d | Position: %d,%d | Level: %d | Treasures: %d",
                playerHealth, maxHealth, pX, pY, level, treasures
        );
        drawString(3, GameConstants.Map.HEIGHT + 5, status, COLOR_CYAN);
    }

    /**
     * Отрисовывает инвентарь игрока.
     *
     * @param inventory инвентарь для отображения
     */
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

    /**
     * Отрисовывает сообщение на указанной строке.
     *
     * @param line номер строки
     * @param message текст сообщения
     * @param color цвет сообщения
     */
    @Override
    public void drawMessage(int line, String message, int color) {
        clearLine(line);
        short safeColor = (short) Math.max(COLOR_BLACK, Math.min(color, COLOR_WHITE));
        drawString(3, line, "> " + message, safeColor);
    }

    /**
     * Отрисовывает экран главного меню.
     *
     * @param currentOption выбранный пункт меню
     */
    @Override
    public void drawMenuScreen(int currentOption) {
        clearScreen();

        String[] menuItems = {
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

        int menuWidth = menuItems[0].length();
        int menuHeight = menuItems.length;

        int screenWidth = Toolkit.getScreenWidth();
        int screenHeight = Toolkit.getScreenHeight();

        // Центрирование меню на экране
        int shiftX = (screenWidth - menuWidth) / 2;
        int shiftY = (screenHeight - menuHeight) / 2;

        // Отрисовка рамки меню
        CharColor menuColor = new CharColor(COLOR_BLACK, COLOR_WHITE);
        for (int i = 0; i < menuHeight; i++) {
            Toolkit.printString(menuItems[i], shiftX, shiftY + i, menuColor);
        }

        // Подсветка выбранного пункта меню
        int optionRow = shiftY + 3 + currentOption;
        CharColor pointerColor = new CharColor(COLOR_BLACK, COLOR_YELLOW);
        Toolkit.printString("<<<", shiftX + 5, optionRow, pointerColor);
        Toolkit.printString(">>>", shiftX + 24, optionRow, pointerColor);
    }

    @Override
    public void drawScoreboard() {
        clearScreen();

        List<SessionStat> stats = loadScoreboardStats();

        if (stats.isEmpty()) {
            drawString(5, 10, "Scoreboard is empty or file not found", CharColor.YELLOW);
            drawString(5, 11, "Play a game first to create it", CharColor.WHITE);
            drawString(5, 13, "Press any key to return...", CharColor.YELLOW);
            refresh();
            Toolkit.readCharacter();
            return;
        }

        drawTable(stats);

        Toolkit.readCharacter();

        refresh();
    }

    private void drawTable(List<SessionStat> stats) {
        // Получаем размеры терминала
        int screenWidth = Toolkit.getScreenWidth();
        int screenHeight = Toolkit.getScreenHeight();

        String[] headers = {
                "treasures", "level", "enemies", "food",
                "elixirs", "scrolls", "attacks", "missed", "moves"
        };

        int cellWidth = 10;
        int tableWidth = headers.length * (cellWidth + 1) + 1; // 9*(10+1)+1 = 100

        // Вычисляем высоту таблицы
        int tableRows = Math.min(stats.size(), 10);
        int tableHeight = 3 + tableRows * 2; // 3 строки заголовков + 2 строки на каждую запись

        // Добавляем место для заголовка (1 строка + 1 пустая строка)
        int titleBlockHeight = 2; // заголовок + отступ
        int totalBlockHeight = titleBlockHeight + tableHeight;

        // Вычисляем начальные координаты для центрирования
        int startX = (screenWidth - tableWidth) / 2 - MAP_OFFSET_X;
        int startY = (screenHeight - totalBlockHeight) / 2 - MAP_OFFSET_Y;

        // Защита от отрицательных значений
        if (startX < 0) startX = 0;
        if (startY < 0) startY = 0;

        // Отрисовка заголовка
        String title = "SCOREBOARD";
        int titleX = startX + (tableWidth - title.length()) / 2;
        drawString(titleX, startY, title, CharColor.YELLOW);

        // Пустая строка после заголовка
        int currentY = startY + 2;

        // Граница таблицы
        String border = "+" + "-".repeat(tableWidth - 2) + "+";

        // Верхняя граница
        drawString(startX, currentY++, border, CharColor.WHITE);

        // Заголовок (левое выравнивание, 10 символов)
        StringBuilder headerLine = new StringBuilder("|");
        for (String header : headers) {
            headerLine.append(String.format("%-10s", header)).append("|");
        }
        drawString(startX, currentY++, headerLine.toString(), CharColor.WHITE);

        // Граница под заголовком
        drawString(startX, currentY++, border, CharColor.WHITE);

        // Данные
        for (int i = 0; i < tableRows; i++) {
            SessionStat s = stats.get(i);

            // Рисуем строку с данными
            String dataLine = "|" + String.format("%10d|", s.getTreasures()) +
                    String.format("%10d|", s.getLevelNum()) +
                    String.format("%10d|", s.getEnemies()) +
                    String.format("%10d|", s.getFood()) +
                    String.format("%10d|", s.getElixirs()) +
                    String.format("%10d|", s.getScrolls()) +
                    String.format("%10d|", s.getAttacks()) +
                    String.format("%10d|", s.getMissed()) +
                    String.format("%10d|", s.getMoves());
            drawString(startX, currentY++, dataLine, CharColor.WHITE);

            // Граница под строкой
            drawString(startX, currentY++, border, CharColor.WHITE);
        }

        // Сообщение (центрировано по экрану)
        String message = "Press any key to return...";
        int messageX = (screenWidth - message.length()) / 2 - MAP_OFFSET_X;
        // Если messageX отрицательный, устанавливаем в 0
        if (messageX < 0) messageX = 0;

        drawString(messageX, currentY + 1, message, CharColor.YELLOW);
    }

    private List<SessionStat> loadScoreboardStats() {
        List<SessionStat> stats = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            File file = new File(SCOREBOARD_PATH);
            if (!file.exists()) {
                return stats;
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

            // Очищаем экран и показываем сообщение об ошибке
            clearScreen();
            drawString(5, 10, "Error loading scoreboard!", CharColor.RED);
            drawString(5, 11, "File: " + SCOREBOARD_PATH, CharColor.RED);
            drawString(5, 13, "Press any key to continue...", CharColor.YELLOW);
            refresh();
            Toolkit.readCharacter();

            return new ArrayList<>();
        }

        return stats;
    }

    /**
     * Возвращает ширину игровой карты.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Возвращает высоту игровой карты.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Очищает указанную строку.
     *
     * @param y номер строки для очистки
     */
    @Override
    public void clearLine(int y) {
        drawString(3, y, " ".repeat(width), COLOR_BLACK);
    }

    /**
     * Завершает работу рендерера и восстанавливает курсор.
     */
    @Override
    public void shutdown() {
        Toolkit.shutdown();
        System.out.print(SHOW_CURSOR); // Восстановление курсора при выходе
    }
}