# Roguelike Console Game (Java 21)

A console-based roguelike RPG inspired by the 1980s classics. This project focuses on implementing a clean, layered architecture and complex gameplay mechanics using Java 21.

## 🛠 Tech Stack
* **Language:** Java 21
* **UI Library:** JCurses (or similar for terminal graphics)
* **Data Format:** JSON (for progress and settings persistence)
* **Architecture:** Layered Architecture (Presentation, Domain, Data Layer)

---

## 🏗 Application Architecture
The project is built with strict adherence to **SOLID** principles, ensuring a clear separation of concerns across three independent layers:

### 1. Presentation Layer (View/UI)
* Rendering the game world, entities, and terminal-based interface.
* Implementation of a dynamic UI: status panels, inventory, and game menus.
* Real-time handling of user keyboard input.

### 2. Domain Layer (Core Logic)
* **Procedural Generation:** An algorithm for level creation that guarantees connectivity between rooms and corridors.
* **Game Entities:** A robust class system for the player character, 5 types of enemies with unique AI behaviors, and various items.
* **Combat Mechanics:** A turn-based system calculating hit probability and damage based on attributes (Dexterity, Strength).
* **Fog of War:** Visibility implementation using **Ray Casting** and **Bresenham's algorithm**.

### 3. Data Layer (Persistence)
* A saving system that captures the game session state in JSON format.
* Storage and serialization of entity attributes, inventory state, and current map layout.
* Global leaderboard management.

---

## 🎮 Key Features

* **Levels:** 21 dungeon levels with scaling difficulty.
* **AI System:**
    * *Zombie:* Slow movement, high HP.
    * *Vampire:* Steals player's maximum HP on successful hits.
    * *Ghost:* Teleportation and invisibility mechanics.
    * *Ogre:* High damage output with a recovery phase (counter-attack logic).
    * *Snake-Mage:* Diagonal movement patterns with a chance to "sleep" the player.
* **Item Ecosystem:** Artifacts, food for health regeneration, elixirs (temporary buffs), and scrolls (permanent attribute increases).
* **Inventory System:** Limited backpack capacity with equipment management.

---

## ⌨️ Controls
| Key | Action |
|-----|--------|
| **W, A, S, D** | Move Character |
| **H** | Use Weapon (select from inventory) |
| **J** | Use Food |
| **K** | Use Elixir |
| **E** | Read Scroll |

---

## 🚀 Build and Run
1. Ensure you have **JDK 21** installed.
2. Clone the repository:
   ```bash
   git clone https://github.com/PhilipNerubenko/Rogue-Game.git
   ```

3. Build the project using Maven or Gradle (depending on your project structure).

4. If you are using Gradle, initialize the wrapper:

   - Using Makefile: ```make wrapper```
   - Directly: ```gradle wrapper --gradle-version 8.5```

5. Run the main application class. You can find all available build and run commands in the **Makefile**.

---

*This project was developed as part of an intensive Java Bootcamp to demonstrate proficiency in designing complex systems and clean code practices.*