package com.example.serverapi.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupService {
    private BackupService() {}

    public static Path resolveBackupPath(Path backupDirectory, String name) {
        if (name == null || name.isBlank() || name.contains("\0")) throw new IllegalArgumentException("Invalid backup name");
        Path root = backupDirectory.toAbsolutePath().normalize();
        Path result = root.resolve(name).normalize();
        if (!result.startsWith(root)) throw new IllegalArgumentException("Backup path escapes backup directory");
        return result;
    }

    public static Path createZip(Path worldDirectory, Path backupDirectory) throws IOException {
        Files.createDirectories(backupDirectory);
        Path target = resolveBackupPath(backupDirectory, "backup-" + Instant.now().toEpochMilli() + ".zip");
        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW); ZipOutputStream zip = new ZipOutputStream(output)) {
            try (var files = Files.walk(worldDirectory)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String entryName = worldDirectory.relativize(file).toString().replace('\\', '/');
                        zip.putNextEntry(new ZipEntry(entryName));
                        try (InputStream input = Files.newInputStream(file)) { input.transferTo(zip); }
                        zip.closeEntry();
                    } catch (IOException error) { throw new BackupFailure(error); }
                });
            }
        } catch (BackupFailure failure) { throw failure.cause; }
        return target;
    }

    private static final class BackupFailure extends RuntimeException {
        private final IOException cause;
        private BackupFailure(IOException cause) { this.cause = cause; }
    }
}
