package org.example.application.records;

import org.example.application.GameInitializer;
import org.example.datalayer.SessionStat;

public record LoadedGame(
        GameInitializer initializer,
        SessionStat stat
) {}
