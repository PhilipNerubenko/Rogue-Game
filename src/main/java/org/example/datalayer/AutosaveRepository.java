package org.example.datalayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.config.GameConstants.PathToFiles.*;
import org.example.domain.interfaces.IAutosaveRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.config.GameConstants.PathToFiles.*;

public class AutosaveRepository implements IAutosaveRepository {

    private final ObjectMapper objectMapper;

    public AutosaveRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        createAutosaveDirectory();
    }

    @Override
    public boolean save(GameState gameState) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = AUTOSAVE_PREFIX + timestamp + AUTOSAVE_EXTENSION;
            String filepath = AUTOSAVE_DIR + "/" + filename;

            objectMapper.writeValue(new File(filepath), gameState);
            cleanupOldSaves();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public GameState loadLatest() {
        try {
            List<File> saveFiles = getAutosaveFilesSorted();
            if (saveFiles.isEmpty()) {
                System.err.println("No save files found");
                return null;
            }

            File latestSave = saveFiles.get(0);
            return objectMapper.readValue(latestSave, GameState.class);
        } catch (IOException e) {
            System.err.println("Failed to load save: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GameState load(int slotIndex) {
        try {
            List<File> saveFiles = getAutosaveFilesSorted();

            // Проверяем, что индекс в допустимом диапазоне
            if (slotIndex < 0 || slotIndex >= saveFiles.size()) {
                return null;
            }

            File saveFile = saveFiles.get(slotIndex);

            return objectMapper.readValue(saveFile, GameState.class);
        } catch (IOException e) {
            System.err.println("Failed to load game state from slot " + slotIndex + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<String> getSaveInfo() {
        List<File> saves = getAutosaveFilesSorted();
        List<String> info = new ArrayList<>();
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        for (File save : saves) {
            try {
                String filename = save.getName();
                String timestampStr = filename.substring(AUTOSAVE_PREFIX.length(),
                        filename.length() - AUTOSAVE_EXTENSION.length());

                LocalDateTime timestamp = LocalDateTime.parse(timestampStr,
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

                String displayTime = timestamp.format(displayFormat);
                GameState gameState = objectMapper.readValue(save, GameState.class);
                String levelInfo = "Level " + gameState.getLevelState().getLevelNumber();

                info.add(String.format("%s - %s - %s", displayTime, levelInfo, filename));
            } catch (Exception e) {
                info.add(save.getName() + " [corrupted]");
            }
        }
        return info;
    }

    @Override
    public void cleanupOldSaves() {
        try {
            List<File> saveFiles = getAutosaveFilesSorted();
            if (saveFiles.size() > AUTOSAVE_MAX) {
                for (int i = AUTOSAVE_MAX; i < saveFiles.size(); i++) {
                    File fileToDelete = saveFiles.get(i);
                    if (fileToDelete.delete()) {
                        System.out.println("Deleted old save: " + fileToDelete.getName());
                    } else {
                        System.err.println("Failed to delete: " + fileToDelete.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up old saves: " + e.getMessage());
        }
    }

    @Override
    public boolean hasSaves() {
        return !getAutosaveFilesSorted().isEmpty();
    }

    private List<File> getAutosaveFilesSorted() {
        File dir = new File(AUTOSAVE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(AUTOSAVE_PREFIX) && name.endsWith(AUTOSAVE_EXTENSION));

        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName).reversed())
                .collect(Collectors.toList());
    }

    private void createAutosaveDirectory() {
        try {
            Path path = Paths.get(AUTOSAVE_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to create autosave directory: " + e.getMessage());
        }
    }
}