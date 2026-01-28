package org.example.presentation.views;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.*;
import org.example.domain.dto.VisibleMapDto;
import org.example.domain.input.ItemSelectionState;
import org.example.domain.interfaces.Renderer;
import org.example.domain.model.Position;
import org.example.domain.model.SaveSlotUiModel;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.MapVisibilityService;

import java.util.List;

import static org.example.config.GameConstants.Colors.*;
import static org.example.config.GameConstants.PathToFiles.AUTOSAVE_MAX;
import static org.example.config.GameConstants.ScreenConfig.*;

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

    @Override
    public int readCharacter() {
        InputChar input = Toolkit.readCharacter();
        return input.getCode();
    }

    @Override
    public void drawMap(VisibleMapDto visibleMap) {
        for (int y = 0; y < visibleMap.height(); y++) {
            for (int x = 0; x < visibleMap.width(); x++) {
                drawChar(x, y, visibleMap.getSymbol(x, y), visibleMap.getColor(x, y));
            }
        }
    }

    /**
     * Отрисовывает символ в указанной позиции.
     *
     * @param x      координата X
     * @param y      координата Y
     * @param symbol символ для отрисовки
     * @param color  цвет символа
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
     * @param x     координата X
     * @param y     координата Y
     * @param text  текст для отрисовки
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
     * Отрисовывает статусную строку с информацией об игроке.
     *
     * @param playerHealth текущее здоровье
     * @param maxHealth    максимальное здоровье
     * @param pX           координата X игрока
     * @param pY           координата Y игрока
     * @param level        текущий уровень
     * @param treasures    количество сокровищ
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
     * Отрисовывает сообщение на указанной строке.
     *
     * @param line    номер строки
     * @param message текст сообщения
     * @param color   цвет сообщения
     */
    @Override
    public void drawMessage(int line, String message, int color) {
        clearLine(line);
        short safeColor = (short) Math.max(COLOR_BLACK, Math.min(color, COLOR_WHITE));
        drawString(MAP_OFFSET_X, line, "> " + message, safeColor);
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
    public void drawLoadGameScreen(int currentOption, List<SaveSlotUiModel> saveSlots) {
        clearScreen();

        final int slotWidth = 44;
        final int headerHeight = 3;
        final int slotRowHeight = 1;

        int screenWidth = Toolkit.getScreenWidth();
        int screenHeight = Toolkit.getScreenHeight();

        int totalHeight = headerHeight + saveSlots.size() * slotRowHeight + 3;
        int shiftX = Math.max(0, (screenWidth - slotWidth) / 2);
        int shiftY = Math.max(0, (screenHeight - totalHeight) / 2);

        // Цвета: обычный, указатель и выделенный текст
        CharColor normalColor = new CharColor(COLOR_BLACK, COLOR_WHITE);
        CharColor pointerColor = new CharColor(COLOR_BLACK, COLOR_YELLOW);
        CharColor selectedTextColor = new CharColor(COLOR_BLACK, COLOR_YELLOW); // Желтый текст для выделения

        // Рамка и заголовок
        String border = "+" + "-".repeat(slotWidth - 2) + "+";
        Toolkit.printString(" ".repeat((slotWidth - "LOAD GAME".length()) / 2) +
                        "LOAD GAME",
                shiftX, shiftY + 1, normalColor);
        Toolkit.printString(border, shiftX, shiftY + 2, normalColor);

        // Слоты сохранений (с динамическим цветом)
        for (int i = 0; i < saveSlots.size(); i++) {
            SaveSlotUiModel slot = saveSlots.get(i);
            int y = shiftY + headerHeight + i * slotRowHeight;

            String desc = slot.description();
            if (desc.length() > slotWidth - 6) {
                desc = desc.substring(0, slotWidth - 9) + "...";
            }

            String borders = "|";
            String slotText = String.format("       %-23s  ", desc);

            CharColor currentColor = (i == currentOption) ? selectedTextColor : normalColor;
            Toolkit.printString(slotText, shiftX, y, currentColor);
            Toolkit.printString(borders, shiftX, y, normalColor);
            Toolkit.printString(borders, shiftX + slotWidth - 1, y, normalColor);
        }

        // Нижняя рамка
        int bottomY = shiftY + headerHeight + saveSlots.size() * slotRowHeight;
        Toolkit.printString(border, shiftX, bottomY, normalColor);

        // Указатели
        if (!saveSlots.isEmpty()) {
            int pointerY = shiftY + headerHeight + currentOption * slotRowHeight;
            Toolkit.printString("<<<", shiftX + 2, pointerY, pointerColor);
            Toolkit.printString(">>>", shiftX + slotWidth - 5, pointerY, pointerColor);
        }

        // Сообщение о лимите
            String limitMsg = String.format("Showing %d/%d recent saves", saveSlots.size(), AUTOSAVE_MAX);
            int msgX = Math.max(0, (screenWidth - limitMsg.length()) / 2 - MAP_OFFSET_X);
            drawString(msgX, bottomY, limitMsg, GameConstants.Colors.COLOR_CYAN);
    }

    @Override
    public void drawScoreboard(List<SessionStat> stats) {
        clearScreen();

        if (stats == null || stats.isEmpty()) {
            drawString(5, 10, "Scoreboard is empty", CharColor.YELLOW);
            drawString(5, 11, "Play a game first to create it", CharColor.WHITE);
            drawString(5, 13, "Press any key to return...", CharColor.YELLOW);
            Toolkit.readCharacter();
            return;
        }

        drawTable(stats);

        Toolkit.readCharacter();
    }

    @Override
    public void renderWorld(GameSession session,
                            char[][] asciiMap,
                            MapVisibilityService visibilityService,
                            FogOfWarService fow,
                            ItemSelectionState selectionState,
                            Message message) {
        // 1. Подготовка экрана
        clearScreen();

       VisibleMapDto visibleMap = visibilityService.prepareVisibleMap(asciiMap, session.getPlayer());
        drawMap(visibleMap);

        // 3. Отрисовка врагов (только тех, кто в зоне видимости и не скрыт туманом)
        for (Enemy enemy : session.getEnemies()) {
            if (!enemy.isInvisible() && fow.isVisible(enemy.getX(), enemy.getY())) {
                drawChar(enemy.getX(), enemy.getY(), enemy.getType(), enemy.getColor());
            }
        }

        // 4. Отрисовка игрока (всегда поверх карты и врагов)
        Position p = session.getPlayer().getPosition();
        drawChar(p.getX(), p.getY(), GameConstants.Icons.PLAYER, COLOR_YELLOW);

        // 5. Отрисовка интерфейса (UI)
        // Если игрок сейчас выбирает предмет в меню — рисуем меню. Иначе — обычный статус-бар.
        if (selectionState.isAwaitingSelection()) {
            drawSelectionMenu(selectionState, session.getPlayer());
        } else {
            drawGameUI(session);
        }

        // 6. Отрисовка игровых сообщений (события боя, поднятие предметов)
        drawMessages(message);
    }

    private void drawGameUI(GameSession session) {
        Player player = session.getPlayer();
        // Статус-бар (здоровье, уровень, золото)
        drawStatusBar(
                player.getHealth(),
                player.getMaxHealth(),
                player.getPosition().getX(),
                player.getPosition().getY(),
                session.getLevelNum(),
                player.getTreasureValue()
        );

        // Подсказки по управлению
        drawString(3, GameConstants.Map.HEIGHT + 4,
                "WASD:move | h:weapon | j:food | k:elixir | e:scroll | q:unequip | ESC:save&exit",
                COLOR_CYAN);

        // Инвентарь и предметы на уровне
        drawInventory(player);
        drawLevelItems(session.getCurrentLevelItems());
    }

    private void drawLevelItems(List<Item> items) {
        int x = 84, y = 20;
        drawString(x, y++, "=== ITEMS ON LEVEL ===", COLOR_CYAN);
        if (items.isEmpty()) {
            drawString(x, y, "No items on this level", COLOR_WHITE);
        } else {
            int max = Math.min(5, items.size());
            for (int i = 0; i < max; i++) {
                Item item = items.get(i);
                drawString(x, y++, String.format("%d. %s at (%d,%d)", i + 1, item.getType(), item.getX(), item.getY()), COLOR_WHITE);
            }
        }
    }

    private void drawInventory(Player player) {
        int y = 0;
        int x = 84;
        drawString(x, y++, "=== INVENTORY ===", COLOR_CYAN);
        Inventory inv = player.getInventory();

        if (inv.getTreasureValue() > 0) {
            drawString(x + 2, y++, "Treasure: " + inv.getTreasureValue() + " gold", COLOR_YELLOW);
        }

        for (org.example.domain.enums.ItemType type : org.example.domain.enums.ItemType.values()) {
            if (type == org.example.domain.enums.ItemType.TREASURE) continue;
            List<Item> items = inv.getItems(type);
            if (!items.isEmpty()) {
                drawString(x + 2, y++, type.name() + ": " + items.size(), getItemTypeColor(type));
                for (int i = 0; i < Math.min(2, items.size()); i++) {
                    drawString(x + 4, y++, "- " + formatItemShortInfo(items.get(i)), COLOR_WHITE);
                }
            }
        }

        Item eq = player.getEquippedWeapon();
        if (eq != null && !eq.getSubType().equals("fists")) {
            drawString(x, y++, "Equipped: " + eq.getSubType() + " (STR+" + eq.getStrength() + ")", COLOR_GREEN);
        }
    }

    private void drawSelectionMenu(ItemSelectionState state, Player player) {
        org.example.domain.enums.ItemType type = state.getPendingItemType();
        int menuX = 45, menuY = 5, menuW = 33, menuH = 15;

        // Очистка области меню
        for (int i = menuY; i < menuY + menuH; i++) {
            drawString(menuX, i, " ".repeat(menuW + 2), COLOR_BLACK);
        }

        // Рамка
        String border = "+" + "-".repeat(menuW) + "+";
        drawString(menuX, menuY, border, COLOR_YELLOW);
        drawString(menuX, menuY + menuH - 1, border, COLOR_YELLOW);
        drawString(menuX + 2, menuY + 1, " Select " + type + " ", COLOR_CYAN);

        List<Item> items = player.getInventory().getItems(type);
        int currentY = menuY + 3;

        if (type == org.example.domain.enums.ItemType.WEAPON) {
            drawString(menuX + 2, currentY++, "0. Unequip", COLOR_WHITE);
        }

        for (int i = 0; i < Math.min(items.size(), 9); i++) {
            drawString(menuX + 2, currentY++, (i + 1) + ". " + items.get(i).getSubType(), COLOR_WHITE);
        }
    }

    // Вспомогательные методы для цветов и текста
    private short getItemTypeColor(org.example.domain.enums.ItemType type) {
        return switch (type) {
            case WEAPON -> COLOR_RED;
            case FOOD -> COLOR_GREEN;
            case ELIXIR -> COLOR_BLUE;
            case SCROLL -> COLOR_MAGENTA;
            default -> COLOR_WHITE;
        };
    }

    private String formatItemShortInfo(Item item) {
        return item.getSubType() + (item.getStrength() > 0 ? " (STR+" + item.getStrength() + ")" : "");
    }

    private void drawMessages(Message message) {
        if (message.getMessageTimer() > 0) {
            if (message.getActiveMessageLine1() != null)
                drawMessage(MESSAGE_LINE_1, message.getActiveMessageLine1(), COLOR_YELLOW);
            if (message.getActiveMessageLine2() != null)
                drawMessage(MESSAGE_LINE_2, message.getActiveMessageLine2(), COLOR_YELLOW);
            if (message.getActiveMessageLine3() != null)
                drawMessage(MESSAGE_LINE_3, message.getActiveMessageLine3(), COLOR_YELLOW);
        }
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
     * Удалить врага с карты и из списка врагов
     *
     * @param session  игровая сессия
     * @param enemy    враг для удаления
     * @param asciiMap ASCII карта игры
     */
    @Override
    public void removeEnemy(GameSession session, Enemy enemy, char[][] asciiMap) {
        // Очистка позиции врага на карте
        Toolkit.printString(String.valueOf(asciiMap[enemy.getY()][enemy.getX()]),
                enemy.getX() + MAP_OFFSET_X, enemy.getY(),
                new CharColor(CharColor.BLACK, CharColor.WHITE));
        // Удаление врага из списка активных врагов
        session.getEnemies().remove(enemy);
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