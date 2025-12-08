package org.example;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.GameSession;
import org.example.domain.model.Room;
import org.example.domain.service.*;
import org.example.presentation.GameLoop;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.List;

import static org.example.config.GameConstants.ScreenConfig.*;
import static org.example.config.GameConstants.TextMessages.*;
import static org.example.config.GameConstants.control.*;

public class App {

    public static void main(String[] args) {
        // Обработка Ctrl+C
        Signal.handle(new Signal("INT"), signal -> {
            Toolkit.shutdown();
            System.out.println("\n" + TERMINATE);
            System.exit(0);
        });

        System.out.print(HIDE_CURSOR);

        try {
            Toolkit.init();

            // Инициализация игры
            GameSession session = new GameSession();
            session.setEnemies(new ArrayList<>());

            // Создание сервисов
            CombatService combatService = new CombatService();
            EnemyAIService enemyAIService = new EnemyAIService();

            // Генерация уровня
            LevelGenerator levelGenerator = new LevelGenerator();
            char[][] asciiMap = levelGenerator.createAsciiMap(GameConstants.Map.MAP_LEVEL);

            // Отображение карты
            CharColor mapColor = new CharColor(CharColor.BLACK, CharColor.WHITE);
            for (int i = 0; i < GameConstants.Map.HEIGHT; i++) {
                Toolkit.printString(new String(asciiMap[i]), MAP_OFFSET_X, i, mapColor);
            }

            // Размещение игрока
            List<Room> rooms = levelGenerator.getRooms();
            Room startRoom = rooms.getFirst();
            int playerX = startRoom.getX1() + 1 + levelGenerator.getRand().nextInt(startRoom.getWidth() - 2);
            int playerY = startRoom.getY1() + 1 + levelGenerator.getRand().nextInt(startRoom.getHeight() - 2);

            session.setPlayerX(playerX);
            session.setPlayerY(playerY);
            session.setCurrentMap(asciiMap);
            session.setRooms(rooms);

            // Создание игрока
            org.example.domain.entity.Character player =
                    new org.example.domain.entity.Character(
                            GameConstants.CharacterBase.HEALTH,
                            GameConstants.CharacterBase.HEALTH,
                            GameConstants.CharacterBase.AGILITY,
                            GameConstants.CharacterBase.STRENGTH,
                            new ArrayList<>()
                    );
            session.setPlayer(player);

            // Создание врагов
            EnemyGenerator enemyGenerator = new EnemyGenerator();
            enemyGenerator.createEnemies(levelGenerator, player, session);

            // Отображение интерфейса
            CharColor hintColor = new CharColor(CharColor.CYAN, CharColor.BLACK);
            printLine(MESSAGE_LINE_3, CONTROL, hintColor, MAP_WIDTH);

            CharColor statusColor = new CharColor(CharColor.YELLOW, CharColor.BLACK);
            printLine(STATUS_LINE_Y,
                    "HP: " + player.getHealth() + "/" + player.getMaximumHealth(),
                    statusColor, 30);

            // Основной игровой цикл
            GameLoop gameLoop = new GameLoop(session, combatService, enemyAIService);
            gameLoop.start();

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Toolkit.shutdown();
            System.out.print(SHOW_CURSOR);
        }
    }

    private static void printLine(int y, String text, CharColor color, int maxLength) {
        // Очистка строки пробелами
        Toolkit.printString(" ".repeat(maxLength), MAP_OFFSET_X, y, color);
        // Печать нового текста
        Toolkit.printString(text, MAP_OFFSET_X, y, color);
    }
}