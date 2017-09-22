package ru.skycelot.redeployer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -jar redeployer.jar wildfly-path [project-path]");
        }

        Path wildflyPath = Paths.get(args[0]);
        if (!Files.isDirectory(wildflyPath)) {
            throw new IllegalArgumentException("Wildfly path doesn't exist");
        }

        Path projectPath;
        if (args.length == 2) {
            projectPath = Paths.get(args[1]);
            if (!Files.isDirectory(projectPath)) {
                throw new IllegalArgumentException("Project path doesn't exist");
            }
        } else {
            projectPath = Paths.get("");
        }

        new Coordinator(projectPath,
                Paths.get(projectPath.toString(), "target"),
                Paths.get(wildflyPath.toString(), "standalone", "deployments"),
                "HH:mm:ss",
                2)
                .start();
    }
}
