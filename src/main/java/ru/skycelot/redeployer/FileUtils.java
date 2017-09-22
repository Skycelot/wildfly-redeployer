package ru.skycelot.redeployer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static boolean removeFile(Path dir, Path file) {
        try {
            Path fileToRemove = Paths.get(dir.toString(), file.getFileName().toString());
            return Files.deleteIfExists(fileToRemove);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyFile(Path srcDir, Path file, Path destDir) {
        try {
            Path srcFile = Paths.get(srcDir.toString(), file.getFileName().toString());
            Path dstFile = Paths.get(destDir.toString(), file.getFileName().toString());
            Files.copy(srcFile, dstFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
