package org.example.presentation;

import org.example.domain.entity.Inventory;
import org.example.domain.entity.Player;
import org.example.domain.model.Level;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.LevelGenerator;

/**
 * Интерфейс (контракт). НЕ знает про JCurses, Toolkit и прочее.
 * Может быть реализован любой граф библиотекой.
 */
public interface Renderer {


    // ========== ДЛЯ ОТРИСОВКи КАРТЫ ==========
    /**
     * Нарисовать один символ в заданных координатах.
     * @param x Столбец (0..WIDTH-1)
     * @param y Строка (0..HEIGHT-1)
     * @param symbol Символ для отрисовки
     * @param color Цвет (используйте константы из GameConstants.Colors)
     */
    void drawChar(int x, int y, char symbol, int color);

    //вывести строку текста.
    void drawString(int x, int y, String text, int color);

    void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, Level level);

    // Очистить весь экран (залить фоном).
    void clearScreen();

    // Обновить экран (показать все изменения).
    void refresh();


    // ========== ОТРИСОВКА ИНТЕРФЕЙСА ПОЛЬЗОВАТЕЛЯ (UI) ==========
    // Нарисовать панель статуса (здоровье, уровень, сокровища).
    void drawStatusBar(int playerHealth, int maxHealth, int level, int treasures);

    // Нарисовать инвентарь
    void drawInventory(Inventory inventory);

    // Показать информационное сообщение (внизу экрана: например, "Вы нанесли 5 урона!").
    void drawMessage(int line, String message, int color);


    // ========== ГЕШДЫ ==========
    // Получить ширину экрана.
    int getWidth();

    //Получить высоту экрана.
    int getHeight();

    // Очистить строку (заполнить пробелами).
    void clearLine(int y);

    // Завершить работу UI (освободить ресурсы).
    void shutdown();
}
