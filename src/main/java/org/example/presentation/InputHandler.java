package org.example.presentation;

import jcurses.system.InputChar;
import jcurses.system.Toolkit;
import org.example.config.GameConstants;
import org.example.datalayer.AutosaveService;
import org.example.datalayer.SessionStat;
import org.example.domain.entity.GameSession;
import org.example.domain.entity.ItemType;
import org.example.domain.model.Direction;
import org.example.domain.model.InputCommand;
import org.example.domain.service.FogOfWarService;

import static org.example.config.GameConstants.control.*;

/**
 * Обработчик пользовательского ввода для игрового интерфейса.
 * Преобразует нажатия клавиш в игровые команды.
 */
public class InputHandler {

    // Флаг ожидания выбора предмета из инвентаря
    private boolean awaitingSelection = false;
    // Тип предмета, для которого ожидается выбор
    private ItemType pendingItemType = null;

    // Сервисы
    private final AutosaveService autosaveService;
    private GameSession session;
    private SessionStat sessionStat;

    /**
     * Конструктор инициализирует сервис автосохранения.
     */
    public InputHandler() {
        this.autosaveService = new AutosaveService();
    }

    // Базовые сеттеры для игровой сессии и статистики
    public void setGameSession(GameSession session) {
        this.session = session;
    }

    public void setSessionStat(SessionStat sessionStat) {
        this.sessionStat = sessionStat;
    }

    /**
     * Основной метод чтения и обработки ввода пользователя.
     * @return Команда для выполнения в игровой логике.
     */
    public InputCommand readCommand() {
        InputChar inputChar = Toolkit.readCharacter();
        int keyCode = inputChar.getCode();
        char keyChar = inputChar.getCharacter();

        // ESC - специальная клавиша для сохранения/выхода
        if (keyCode == ESC_KEY_CODE) {
            return handleEscapeKey();
        }

        // Если ожидается выбор предмета из инвентаря
        if (awaitingSelection) {
            return handleSelectionInput(keyChar);
        }

        // Обычный ввод команд
        return handleRegularInput(keyCode, keyChar);
    }

    /**
     * Обработка нажатия ESC.
     * Если идет выбор предмета - отмена, иначе - сохранение и выход.
     */
    private InputCommand handleEscapeKey() {
        if (awaitingSelection) {
            resetAwaitingState();
            return InputCommand.none();
        }

        // Автосохранение перед выходом
        if (session != null && session.getPlayer() != null && !session.getPlayer().isDead()) {
            try {
                autosaveService.saveGame(session, sessionStat);
            } catch (Exception e) {
                System.err.println("Ошибка автосохранения: " + e.getMessage());
            }
        }
        return InputCommand.quit();
    }

    /**
     * Обработка обычных игровых команд.
     */
    private InputCommand handleRegularInput(int keyCode, char keyChar) {
        // Движение (WASD или стрелки)
        if (keyCode == KEY_W || keyChar == 'W' || keyChar == 'w') {
            return InputCommand.move(Direction.NORTH);
        }
        if (keyCode == KEY_S || keyChar == 'S' || keyChar == 's') {
            return InputCommand.move(Direction.SOUTH);
        }
        if (keyCode == KEY_A || keyChar == 'A' || keyChar == 'a') {
            return InputCommand.move(Direction.WEST);
        }
        if (keyCode == KEY_D || keyChar == 'D' || keyChar == 'd') {
            return InputCommand.move(Direction.EAST);
        }

        // Использование предметов (горячие клавиши)
        if (keyChar == 'h' || keyChar == 'H') {
            return InputCommand.useItem(ItemType.WEAPON);
        }
        if (keyChar == 'j' || keyChar == 'J') {
            return InputCommand.useItem(ItemType.FOOD);
        }
        if (keyChar == 'k' || keyChar == 'K') {
            return InputCommand.useItem(ItemType.ELIXIR);
        }
        if (keyChar == 'e' || keyChar == 'E') {
            return InputCommand.useItem(ItemType.SCROLL);
        }

        // Снятие оружия
        if (keyChar == 'q' || keyChar == 'Q') {
            return InputCommand.unequipWeapon();
        }

        // Пропуск хода (пробел или точка)
        if (keyChar == '.' || keyChar == ' ') {
            return InputCommand.none();
        }

        // Неизвестная команда
        return InputCommand.none();
    }

    /**
     * Обработка ввода при выборе предмета из инвентаря.
     */
    private InputCommand handleSelectionInput(char keyChar) {
        // Выбор по цифре 0-9
        if (keyChar >= '0' && keyChar <= '9') {
            int index = Character.getNumericValue(keyChar);
            return InputCommand.selectIndex(index);
        }
        return InputCommand.none();
    }

    // Управление состоянием выбора предметов

    public boolean isAwaitingSelection() {
        return awaitingSelection;
    }

    public ItemType getPendingItemType() {
        return pendingItemType;
    }

    public void setAwaitingSelection(boolean awaiting, ItemType itemType) {
        this.awaitingSelection = awaiting;
        this.pendingItemType = itemType;
    }

    public void resetAwaitingState() {
        this.awaitingSelection = false;
        this.pendingItemType = null;
    }

    // Методы для работы с сохранениями

    /**
     * Загрузка сохраненной игры.
     */
    public boolean loadSavedGame(GameSession session, SessionStat sessionStat,
                                 org.example.domain.service.LevelGenerator levelGenerator) {
        return autosaveService.loadAndRestoreGame(session, sessionStat, levelGenerator);
    }

    /**
     * Проверка наличия сохранений.
     */
    public boolean hasSavedGames() {
        return autosaveService.hasSaves();
    }
}