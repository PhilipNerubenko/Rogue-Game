package org.example.presentation;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jcurses.system.CharColor;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.Inventory;
import org.example.domain.entity.ItemType;
import org.example.domain.entity.Player;
import org.example.domain.model.Room;
import org.example.domain.service.FogOfWarService;
import org.example.domain.service.LevelGenerator;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.example.config.GameConstants.PathToFiles.SCOREBOARD_PATH;

import static org.example.config.GameConstants.ScreenConfig.SHOW_CURSOR;

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

    public void drawMapWithFog(char[][] map, Player player, FogOfWarService fog, LevelGenerator levelGen) {
        fog.updateVisibility(player.getPosition(), map);

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                char tile = map[y][x];
                Room room = levelGen.getRoomAt(x, y); // ← Теперь используем levelGen
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
    public void drawStatusBar(int playerHealth, int maxHealth, int playerX, int playerY, int level, int treasures) {
        String status = String.format(
                "HP: %d/%d | Pos: %d,%d | Level: %d | $: %d",
                playerHealth, maxHealth, playerX, playerY, level, treasures
        );
        drawString(3, GameConstants.Map.HEIGHT + 1, status, CharColor.CYAN);
    }

    @Override
    public void drawInventory(Inventory inventory) {
        int startY = 5;
        int startX = GameConstants.Map.WIDTH + 10;

        drawString(startX, startY, "=== BACKPACK ===", CharColor.WHITE);

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

        CharColor menuColor = new CharColor(CharColor.BLACK, CharColor.WHITE);
        for (int i = 0; i < menuHeight; i++) {
            Toolkit.printString(strings[i], shiftX, shiftY + i, menuColor);
        }

        int optionRow = shiftY + 3 + currentOption;
        CharColor pointerColor = new CharColor(CharColor.BLACK, CharColor.YELLOW);

        Toolkit.printString("<<<", shiftX + 5, optionRow, pointerColor);
        Toolkit.printString(">>>", shiftX + 24, optionRow, pointerColor);

    }


      @Override
    public void drawScoreboard() {
        clearScreen();

        // Загружаем статистику из JSON-файла через Jackson
          java.util.List<SessionStat> stats = new ArrayList<>();
          ObjectMapper mapper = new ObjectMapper();
          try {
              File file = new File(SCOREBOARD_PATH);

              if (!file.exists()) {
                  drawString(5, 10, "Scoreboard file not found!", CharColor.YELLOW);
                  drawString(5, 11, "Play a game first to create it.", CharColor.WHITE);
                  drawString(5, 13, "Press any key...", CharColor.YELLOW);
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

              drawString(5, 10, "Error loading scoreboard!", CharColor.RED);
              drawString(5, 11, "File: " + SCOREBOARD_PATH, CharColor.RED);
              drawString(5, 13, "Press any key to continue...", CharColor.YELLOW);
              refresh();
              Toolkit.readCharacter();
              return;
          }

          // --- Отрисовка таблицы ---
          String[] headers = {"treasures", "level", "enemies", "food", "elixirs", "scrolls", "attacks", "missed", "moves"};
          int startX = 0; // Смещение слева (как в вашем примере)
          int startY = 1;
          int cellWidth = 10; // 1 палка + 9 символов содержимого

          // Граница: 91 символ (10 палок + 9 блоков по 9 символов)
          String border = "-".repeat(91);

          // Шаблон: палка + 9 пробелов, повторяется 9 раз
          String separatorTemplate = "|" + "         |".repeat(headers.length);

          int currentY = startY;

          // Верхняя граница
          drawString(startX, currentY++, border, CharColor.WHITE);

          // Заголовок (левое выравнивание, 9 символов)
          StringBuilder headerLine = new StringBuilder("|");
          for (String header : headers) {
              headerLine.append(String.format("%-9s", header)).append("|");
          }
          drawString(startX, currentY++, headerLine.toString(), CharColor.WHITE);

          // Граница под заголовком
          drawString(startX, currentY++, border, CharColor.WHITE);

          // Данные
          for (int i = 0; i < Math.min(stats.size(), 10); i++) {
              SessionStat s = stats.get(i);

              // 1. Белые разделители
              drawString(startX, currentY, separatorTemplate, CharColor.WHITE);

              // 2. Голубые данные (правое выравнивание, 9 символов)
              int dataX = startX + 1; // Позиция после первой палки

              drawString(dataX + 0, currentY, String.format("%9d", s.getTreasures()), CharColor.CYAN);
              drawString(dataX + 10, currentY, String.format("%9d", s.getLevelNum()), CharColor.CYAN);
              drawString(dataX + 20, currentY, String.format("%9d", s.getEnemies()), CharColor.CYAN);
              drawString(dataX + 30, currentY, String.format("%9d", s.getFood()), CharColor.CYAN);
              drawString(dataX + 40, currentY, String.format("%9d", s.getElixirs()), CharColor.CYAN);
              drawString(dataX + 50, currentY, String.format("%9d", s.getScrolls()), CharColor.CYAN);
              drawString(dataX + 60, currentY, String.format("%9d", s.getAttacks()), CharColor.CYAN);
              drawString(dataX + 70, currentY, String.format("%9d", s.getMissed()), CharColor.CYAN);
              drawString(dataX + 80, currentY, String.format("%9d", s.getMoves()), CharColor.CYAN);

              currentY++;

              // Граница под строкой
              drawString(startX, currentY++, border, CharColor.WHITE);
          }

          // Сообщение (центрировано)
          String message = "Press ESC to return...";
          int messageX = startX + (border.length() - message.length()) / 2;
          drawString(messageX, currentY + 1, message, CharColor.YELLOW);

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
        drawString(3, y, " ".repeat(width), CharColor.BLACK);
    }

    @Override
    public void shutdown() {
        Toolkit.shutdown();
        System.out.print(SHOW_CURSOR); // Показать курсор
    }
}