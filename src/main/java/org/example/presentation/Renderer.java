package org.example.presentation;

import org.example.domain.entity.Inventory;
import org.example.domain.entity.Player;
import org.example.domain.service.FogOfWarService;
import org.example.domain.factory.LevelGenerator;

/**
 * Интерфейс рендерера (контракт для отрисовки игрового интерфейса).
 * Абстрагирует логику отрисовки от конкретной графической библиотеки.
 * Может быть реализован с использованием JCurses, Swing, LibGDX и т.д.
 */
public interface Renderer {

    // ========== ОСНОВНЫЕ МЕТОДЫ ОТРИСОВКИ ==========

    /**
     * Нарисовать один символ в заданных координатах.
     * @param x Столбец (0..WIDTH-1)
     * @param y Строка (0..HEIGHT-1)
     * @param symbol Символ для отрисовки
     * @param color Цвет (используйте константы из GameConstants.Colors)
     */
    void drawChar(int x, int y, char symbol, int color);

    /**
     * Вывести строку текста.
     * @param x Начальная координата X
     * @param y Начальная координата Y
     * @param text Текст для отрисовки
     * @param color Цвет текста
     */
    void drawString(int x, int y, String text, int color);

    /**
     * Очистить весь экран (залить фоном).
     */
    void clearScreen();

    /**
     * Обновить экран (отобразить все накопленные изменения).
     */
    void refresh();

    /**
     * Очистить конкретную строку (заполнить пробелами).
     * @param y Номер строки для очистки
     */
    void clearLine(int y);

    // ========== ОТРИСОВКА ИГРОВОЙ КАРТЫ ==========

    /**
     * Отрисовать карту с учетом "тумана войны".
     * @param map Двумерный массив символов карты
     * @param player Игрок (для определения видимости)
     * @param fog Сервис тумана войны
     * @param levelGen Генератор уровней
     */
    void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, LevelGenerator levelGen);

    // ========== ОТРИСОВКА ИНТЕРФЕЙСА ПОЛЬЗОВАТЕЛЯ ==========

    /**
     * Нарисовать панель статуса игрока.
     * @param playerHealth Текущее здоровье игрока
     * @param maxHealth Максимальное здоровье игрока
     * @param pX Координата X игрока
     * @param pY Координата Y игрока
     * @param level Текущий уровень
     * @param treasures Количество собранных сокровищ
     */
    void drawStatusBar(int playerHealth, int maxHealth, int pX, int pY, int level, int treasures);

    /**
     * Нарисовать инвентарь игрока.
     * @param inventory Объект инвентаря
     */
    void drawInventory(Inventory inventory);

    /**
     * Показать информационное сообщение.
     * @param line Номер строки для отображения (обычно нижние строки экрана)
     * @param message Текст сообщения
     * @param color Цвет сообщения
     */
    void drawMessage(int line, String message, int color);

    /**
     * Отрисовать экран меню.
     * @param currentOption Индекс текущей выбранной опции меню
     */
    void drawMenuScreen(int currentOption);

    /**
     * Отрисовать таблицу рекордов.
     */
    void drawScoreboard();

    // ========== СЛУЖЕБНЫЕ МЕТОДЫ ==========

    /**
     * Получить ширину экрана (в символах).
     * @return Ширина экрана
     */
    int getWidth();

    /**
     * Получить высоту экрана (в символах).
     * @return Высота экрана
     */
    int getHeight();

    /**
     * Завершить работу рендерера (освободить ресурсы).
     */
    void shutdown();
}