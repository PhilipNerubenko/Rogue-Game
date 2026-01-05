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

import java.io.IOException;

import static org.example.config.GameConstants.control.*;

/**
 * Обработчик ввода пользователя
 */
public class InputHandler {

    private boolean awaitingSelection = false;
    private ItemType pendingItemType = null;

    private final AutosaveService autosaveService;
    private GameSession session;
    private SessionStat sessionStat;

    public InputHandler() {
        this.autosaveService = new AutosaveService();
    }

    public void setGameSession(GameSession session) {
        this.session = session;
    }

    public void setSessionStat(SessionStat sessionStat) {
        this.sessionStat = sessionStat;
    }

    public InputCommand readCommand() {
        InputChar inputChar = Toolkit.readCharacter();
        int keyCode = inputChar.getCode();
        char keyChar = inputChar.getCharacter();

        // Обработка ESC для автосохранения
        if (keyCode == ESC_KEY_CODE) {
            // Если ожидаем выбор - просто отменяем выбор
            if (awaitingSelection) {
                resetAwaitingState();
                return InputCommand.none();
            }

            // Иначе сохраняем и выходим
            if (session != null && session.getPlayer() != null && !session.getPlayer().isDead()) {
                try {
                    boolean saved = autosaveService.saveGame(session, sessionStat);
                    if (saved) {
                        System.out.println("Game autosaved on ESC");
                    }
                } catch (Exception e) {
                    System.err.println("Autosave failed: " + e.getMessage());
                }
            }
            return InputCommand.quit();
        }

        // Если ожидаем выбор предмета
        if (awaitingSelection) {
            return handleSelectionInput(keyChar);
        }

        // Обработка обычных команд
        return handleRegularInput(keyCode, keyChar);
    }

    private InputCommand handleRegularInput(int keyCode, char keyChar) {
        // Движение
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

        // Использование предметов (только если не ожидаем выбора)
        if (!awaitingSelection) {
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
        }

        // Пропуск хода
        if (keyChar == '.' || keyChar == ' ') {
            return InputCommand.none();
        }

        return InputCommand.none();
    }

    private InputCommand handleSelectionInput(char keyChar) {
        // Отмена выбора ESC уже обработана выше

        // Выбор цифры 0-9
        if (keyChar >= '0' && keyChar <= '9') {
            int index = Character.getNumericValue(keyChar);
            return InputCommand.selectIndex(index);
        }

        // Любая другая клавиша - игнорируем
        return InputCommand.none();
    }

    // Геттеры и сеттеры для состояния выбора
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

    // Метод для загрузки игры
    public boolean loadSavedGame(GameSession session, SessionStat sessionStat,
                                 org.example.domain.service.LevelGenerator levelGenerator) {
        return autosaveService.loadAndRestoreGame(session, sessionStat, levelGenerator);
    }

    // Проверка наличия сохранений
    public boolean hasSavedGames() {
        return autosaveService.hasSaves();
    }
}