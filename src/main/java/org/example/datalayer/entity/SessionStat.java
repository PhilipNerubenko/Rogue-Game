package org.example.datalayer.entity;

import java.io.IOException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.datalayer.service.StatisticsService;

/**
 * Класс для хранения и управления статистикой игровой сессии.
 * Реализует Comparable для сравнения статистик (например, для таблицы рекордов).
 */
public class SessionStat implements Comparable<SessionStat> {
    @JsonProperty("treasures")
    private int treasures;   // Количество найденных сокровищ

    @JsonProperty("levelNum")
    private int levelNum;    // Текущий номер уровня

    @JsonProperty("enemies")
    private int enemies;     // Количество побежденных врагов

    @JsonProperty("food")
    private int food;        // Количество съеденной еды

    @JsonProperty("elixirs")
    private int elixirs;     // Количество использованных эликсиров

    @JsonProperty("scrolls")
    private int scrolls;     // Количество использованных свитков

    @JsonProperty("attacks")
    private int attacks;     // Количество атак

    @JsonProperty("missed")
    private int missed;      // Количество промахов

    @JsonProperty("moves")
    private int moves;       // Количество ходов

    /**
     * Конструктор по умолчанию. Инициализирует все поля нулевыми значениями.
     */
    public SessionStat() {
        reset();
    }

    /**
     * Параметризованный конструктор.
     *
     * @param treasures количество сокровищ
     * @param levelN    номер уровня
     * @param enemies   количество врагов
     * @param food      количество еды
     * @param elixirs   количество эликсиров
     * @param scrolls   количество свитков
     * @param attacks   количество атак
     * @param missed    количество промахов
     * @param moves     количество ходов
     */
    public SessionStat(int treasures, int levelN, int enemies, int food, int elixirs,
                       int scrolls, int attacks, int missed, int moves) {
        this.treasures = treasures;
        this.levelNum = levelN;
        this.enemies = enemies;
        this.food = food;
        this.elixirs = elixirs;
        this.scrolls = scrolls;
        this.attacks = attacks;
        this.missed = missed;
        this.moves = moves;
    }

    // Геттеры и сеттеры для всех полей (оставлены без изменений по требованию)
    public int getTreasures() { return treasures; }
    public void setTreasures(int treasures) { this.treasures = treasures; }
    public int getLevelNum() { return levelNum; }
    public void setLevelNum(int levelN) { this.levelNum = levelN; }
    public int getEnemies() { return enemies; }
    public void setEnemies(int enemies) { this.enemies = enemies; }
    public int getFood() { return food; }
    public void setFood(int food) { this.food = food; }
    public int getElixirs() { return elixirs; }
    public void setElixirs(int elixirs) { this.elixirs = elixirs; }
    public int getScrolls() { return scrolls; }
    public void setScrolls(int scrolls) { this.scrolls = scrolls; }
    public int getAttacks() { return attacks; }
    public void setAttacks(int attacks) { this.attacks = attacks; }
    public int getMissed() { return missed; }
    public void setMissed(int missed) { this.missed = missed; }
    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }

    /**
     * Сбрасывает всю статистику к начальным значениям.
     * Уровень устанавливается в 1, остальные значения - в 0.
     */
    public void reset() {
        this.treasures = 0;
        this.levelNum = 1;
        this.enemies = 0;
        this.food = 0;
        this.elixirs = 0;
        this.scrolls = 0;
        this.attacks = 0;
        this.missed = 0;
        this.moves = 0;
    }

    // Ниже представлены методы инкремента с сохранением статистики.
    // Каждый метод увеличивает соответствующее поле на 1 и сохраняет обновленную статистику.

    public void incrementTreasures() throws IOException {
        this.treasures++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementLevel() throws IOException {
        this.levelNum++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementEnemies() throws IOException {
        this.enemies++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementFood() throws IOException {
        this.food++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementElixirs() throws IOException {
        this.elixirs++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementScrolls() throws IOException {
        this.scrolls++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementAttacks() throws IOException {
        this.attacks++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementMissed() throws IOException {
        this.missed++;
        StatisticsService.saveCurrentStats(this);
    }

    public void incrementMoves() throws IOException {
        this.moves++;
        StatisticsService.saveCurrentStats(this);
    }

    public void addTreasures(int count) throws IOException {
        this.treasures += count;
        StatisticsService.saveCurrentStats(this);
    }


    /**
     * Сравнивает текущую статистику с другой для определения порядка сортировки.
     * Приоритет сравнения:
     * 1. Больше сокровищ - выше в рейтинге
     * 2. При равенстве сокровищ - выше уровень
     * 3. При равенстве уровня - меньше ходов лучше
     *
     * @param o другая статистика для сравнения
     * @return отрицательное число, если текущая статистика лучше,
     *         положительное, если хуже, 0 - если равны
     */
    @Override
    public int compareTo(SessionStat o) {
        if (this.treasures != o.treasures) {
            return Integer.compare(o.treasures, this.treasures); // больше сокровищ -> лучше
        }
        if (this.levelNum != o.levelNum) {
            return Integer.compare(o.levelNum, this.levelNum); // выше уровень -> лучше
        }
        return Integer.compare(this.moves, o.moves); // меньше ходов -> лучше
    }
}