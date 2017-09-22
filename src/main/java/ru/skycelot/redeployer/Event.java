package ru.skycelot.redeployer;

import java.nio.file.Path;

public class Event {
    public Type type;
    public Path file;

    public Event(Type type) {
        this.type = type;
    }

    public Event(Type type, Path file) {
        this.type = type;
        this.file = file;
    }

    @Override
    public String toString() {
        return type + (file != null ? ": " + file : "");
    }

    public enum Type {
        TARGET_DIRECTORY_CREATED,
        TARGET_DIRECTORY_REMOVED,
        WAR_FILE_ACTIVITY,
        UNDEPLOY_COMPLETED,
        DEPLOY_COMPLETED,
        DEPLOY_FAILED,
        CLOCK_TICK
    }
}
