package org.example.presentation;

import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;

/**
 * Реализация интерфейса Renderer на базе JCurses.
 * ЕДИНСТВЕННЫЙ класс, который знает про Toolkit.
 */
public class JCursesRenderer implements Renderer {

    private final int width;
    private final int height;
    private final CharColor defaultColor;

    public JCursesRenderer() {
        this.width = GameConstants.Map.WIDTH;
        this.height = GameConstants.Map.HEIGHT;
        this.defaultColor = new CharColor(CharColor.BLACK, CharColor.BLACK);

        // Инициализация JCurses
        Toolkit.init();
        Toolkit.clearScreen(defaultColor);
    }

    @Override
    public void drawChar(int x, int y, char symbol, int color) {
        // Преобразуем int цвет в CharColor
        Toolkit.printString(
                String.valueOf(symbol),
                x + 3, // Смещение по X (как у вас в App.java)
                y,
                new CharColor(CharColor.BLACK, (short) color)
        );
    }

    @Override
    public void drawString(int x, int y, String text, int color) {
        Toolkit.printString(
                text,
                x + 3,
                y,
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