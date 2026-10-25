package com.example.serverapi.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {
    @TempDir Path temp;

    @Test
    void backupNameCannotEscapeBackupDirectory() {
        assertThrows(IllegalArgumentException.class, () -> BackupService.resolveBackupPath(temp, "..\\outside.zip"));
        assertEquals(temp.resolve("world.zip").normalize(), BackupService.resolveBackupPath(temp, "world.zip"));
    }
}
