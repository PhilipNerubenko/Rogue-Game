package org.example.domain.service;

import org.example.domain.dto.VisibleMapDto;
import org.example.domain.entity.Player;
import org.example.domain.model.Position;

import java.util.Set;

import static org.example.config.GameConstants.Colors.*;
import static org.example.config.GameConstants.Icons.*;
import static org.example.domain.enums.ItemType.TREASURE;

public class MapVisibilityService {

    private final FogOfWarService fogService;

    public MapVisibilityService(FogOfWarService fogService) {
        this.fogService = fogService;
    }

    public VisibleMapDto prepareVisibleMap(char[][] fullMap, Player player) {
        int height = fullMap.length;
        int width = fullMap[0].length;

        char[][] symbols = new char[height][width];
        short[][] colors = new short[height][width];

        Set<Position> allExplored = fogService.getAllExploredCells();
        Set<Position> currentVisible = fogService.getCurrentVisibleCells();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position pos = new Position(x, y);
                char tile = fullMap[y][x];

                if (tile == ' ') {
                    symbols[y][x] = ' ';
                    colors[y][x] = COLOR_BLACK;
                } else if (!allExplored.contains(pos)) {
                    symbols[y][x] = ' ';
                    colors[y][x] = COLOR_BLACK;
                } else if (currentVisible.contains(pos)) {
                    symbols[y][x] = tile;
                    colors[y][x] = getBrightTileColor(tile);
                } else {
                    symbols[y][x] = tile;
                    colors[y][x] = getDimTileColor(tile);
                }
            }
        }
        return new VisibleMapDto(symbols, colors);
    }

    /**
     * Возвращает яркие цвета для клеток, видимых в текущий момент.
     *
     * @param tile символ клетки
     * @return цвет для отрисовки
     */
    private short getBrightTileColor(char tile) {
        return switch (tile) {
            case FLOOR -> COLOR_CYAN;           // Пол
            case W_WALL, H_WALL -> COLOR_WHITE;     // Стены
            case CORRIDOR -> COLOR_YELLOW;         // Коридор
            case DOOR -> COLOR_MAGENTA;        // Дверь
            case TREASURES -> COLOR_GREEN;          // Сокровище
            case FOOD -> COLOR_GREEN;          // Еда
            case ELIXIR -> COLOR_BLUE;           // Эликсир
            case SCROLL -> COLOR_MAGENTA;        // Свиток
            case WEAPON -> COLOR_RED;            // Оружие

            // Существа
            case ZOMBIE -> COLOR_GREEN;          // Зомби
            case VAMPIRE -> COLOR_RED;            // Вампир
            case GHOST -> COLOR_WHITE;          // Призрак
            case OGRE -> COLOR_YELLOW;         // Огр
            case SNAKE_MAGE -> COLOR_WHITE;           // Змеиный маг
            case PLAYER -> COLOR_YELLOW;         // Игрок

            // Выход
            case 'E', EXIT -> COLOR_GREEN;

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
            case '.', '|', '~', CORRIDOR, '+' -> COLOR_BLUE;

            // Предметы
            case '$', ',', '!', '?', ')' -> COLOR_BLUE;

            // Существа и игрок - скрываем
            case 'z', 'v', 'g', 'o', 's', '@' -> -1;

            // Выход
            case 'E', '⇧' -> COLOR_BLUE;

            default -> COLOR_BLUE;
        };
    }
}
