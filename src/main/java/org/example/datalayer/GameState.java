package org.example.datalayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.domain.entity.Enemy;
import org.example.domain.entity.Item;
import org.example.domain.model.Position;
import org.example.domain.model.Room;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для хранения полного состояния игры для сериализации/десериализации.
 * Содержит все необходимые данные для сохранения и загрузки игры.
 */
public class GameState {

    @JsonProperty("save_timestamp")
    private String timestamp;

    @JsonProperty("player")
    private PlayerState playerState;

    @JsonProperty("level")
    private LevelState levelState;

    @JsonProperty("statistics")
    private SessionStat sessionStat;

    @JsonProperty("game_session")
    private GameSessionState gameSessionState;

    @JsonProperty("fog_of_war")
    private FogOfWarState fogOfWarState;

    /**
     * Конструктор по умолчанию. Устанавливает текущую дату и время как timestamp.
     */
    public GameState() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Конструктор с заданным timestamp.
     * @param timestamp строка с временной меткой сохранения
     */
    public GameState(String timestamp) {
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    public LevelState getLevelState() {
        return levelState;
    }

    public void setLevelState(LevelState levelState) {
        this.levelState = levelState;
    }

    public SessionStat getSessionStat() {
        return sessionStat;
    }

    public void setSessionStat(SessionStat sessionStat) {
        this.sessionStat = sessionStat;
    }

    public GameSessionState getGameSessionState() {
        return gameSessionState;
    }

    public void setGameSessionState(GameSessionState gameSessionState) {
        this.gameSessionState = gameSessionState;
    }

    public FogOfWarState getFogOfWarState() {
        return fogOfWarState;
    }

    public void setFogOfWarState(FogOfWarState fogOfWarState) {
        this.fogOfWarState = fogOfWarState;
    }

    /**
     * Вложенный класс для хранения состояния игрока.
     * Содержит все характеристики, позицию и инвентарь игрока.
     */
    public static class PlayerState {
        @JsonProperty("max_health")
        private int maxHealth;

        @JsonProperty("health")
        private int health;

        @JsonProperty("agility")
        private int agility;

        @JsonProperty("strength")
        private int strength;

        @JsonProperty("position_x")
        private int positionX;

        @JsonProperty("position_y")
        private int positionY;

        @JsonProperty("sleep_turns")
        private int sleepTurns;

        @JsonProperty("inventory")
        private List<Item> inventoryItems;

        @JsonProperty("equipped_weapon")
        private Item equippedWeapon;

        /**
         * Конструктор. Инициализирует пустой инвентарь.
         */
        public PlayerState() {
            this.inventoryItems = new ArrayList<>();
        }

        // Геттеры и сеттеры
        public int getMaxHealth() {
            return maxHealth;
        }

        public void setMaxHealth(int maxHealth) {
            this.maxHealth = maxHealth;
        }

        public int getHealth() {
            return health;
        }

        public void setHealth(int health) {
            this.health = health;
        }

        public int getAgility() {
            return agility;
        }

        public void setAgility(int agility) {
            this.agility = agility;
        }

        public int getStrength() {
            return strength;
        }

        public void setStrength(int strength) {
            this.strength = strength;
        }

        public int getPositionX() {
            return positionX;
        }

        public void setPositionX(int positionX) {
            this.positionX = positionX;
        }

        public int getPositionY() {
            return positionY;
        }

        public void setPositionY(int positionY) {
            this.positionY = positionY;
        }

        public boolean isSleepTurns() {
            return sleepTurns > 0;
        }

        public void setSleepTurns(int turns) {
            this.sleepTurns = turns;
        }

        public int getSleepTurns() {
            return sleepTurns;
        }

        public List<Item> getInventoryItems() {
            return inventoryItems;
        }

        public void setInventoryItems(List<Item> inventoryItems) {
            this.inventoryItems = inventoryItems;
        }

        public Item getEquippedWeapon() {
            return equippedWeapon;
        }

        public void setEquippedWeapon(Item equippedWeapon) {
            this.equippedWeapon = equippedWeapon;
        }
    }

    /**
     * Вложенный класс для хранения состояния уровня.
     * Содержит карту, предметы, врагов и информацию о комнатах.
     */
    public static class LevelState {
        @JsonProperty("level_number")
        private int levelNumber;

        @JsonProperty("ascii_map")
        private char[][] asciiMap;

        @JsonProperty("items")
        private List<Item> items;

        @JsonProperty("enemies")
        private List<Enemy> enemies;

        @JsonProperty("rooms")
        private List<Room> rooms;

        /**
         * Конструктор. Инициализирует пустые коллекции.
         */
        public LevelState() {
            this.items = new ArrayList<>();
            this.enemies = new ArrayList<>();
            this.rooms = new ArrayList<>();
        }

        // Геттеры и сеттеры
        public int getLevelNumber() {
            return levelNumber;
        }

        public void setLevelNumber(int levelNumber) {
            this.levelNumber = levelNumber;
        }

        public char[][] getAsciiMap() {
            return asciiMap;
        }

        public void setAsciiMap(char[][] asciiMap) {
            this.asciiMap = asciiMap;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }

        public List<Enemy> getEnemies() {
            return enemies;
        }

        public void setEnemies(List<Enemy> enemies) {
            this.enemies = enemies;
        }

        public List<Room> getRooms() {
            return rooms;
        }

        public void setRooms(List<Room> rooms) {
            this.rooms = rooms;
        }
    }

    /**
     * Вложенный класс для хранения состояния игровой сессии.
     * Содержит текущую карту, отображаемую игроку.
     */
    public static class GameSessionState {
        @JsonProperty("current_map")
        private char[][] currentMap;

        public char[][] getCurrentMap() {
            return currentMap;
        }

        public void setCurrentMap(char[][] currentMap) {
            this.currentMap = currentMap;
        }
    }

    /**
     * Вложенный класс для хранения состояния "тумана войны".
     * Отслеживает исследованные клетки и комнаты.
     */
    public static class FogOfWarState {
        @JsonProperty("explored_cells")
        private List<Position> exploredCells;

        @JsonProperty("explored_rooms")
        private List<Room> exploredRooms;

        /**
         * Конструктор. Инициализирует пустые коллекции исследованных областей.
         */
        public FogOfWarState() {
            this.exploredCells = new ArrayList<>();
            this.exploredRooms = new ArrayList<>();
        }

        // Геттеры и сеттеры
        public List<Position> getExploredCells() {
            return exploredCells;
        }

        public void setExploredCells(List<Position> exploredCells) {
            this.exploredCells = exploredCells;
        }

        public List<Room> getExploredRooms() {
            return exploredRooms;
        }

        public void setExploredRooms(List<Room> exploredRooms) {
            this.exploredRooms = exploredRooms;
        }
    }
}