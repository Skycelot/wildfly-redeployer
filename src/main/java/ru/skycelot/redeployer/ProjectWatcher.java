package ru.skycelot.redeployer;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;

public class ProjectWatcher implements Runnable {

    private final Path projectPath;
    private final Path projectTargetPath;
    private final BlockingQueue<Event> eventsQueue;

    public ProjectWatcher(Path projectPath, Path projectTargetPath, BlockingQueue<Event> eventsQueue) {
        this.projectPath = projectPath;
        this.projectTargetPath = projectTargetPath;
        this.eventsQueue = eventsQueue;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            projectPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            try {
                while (!Thread.interrupted()) {
                    WatchKey watchKey = watchService.take();
                    for (WatchEvent<?> untypedWatchEvent : watchKey.pollEvents()) {
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) untypedWatchEvent;
                        if (projectTargetPath.getFileName().equals(watchEvent.context().getFileName())) {
                            if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                Event queueEvent = new Event(Event.Type.TARGET_DIRECTORY_CREATED);
                                eventsQueue.offer(queueEvent);
                            } else if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                Event queueEvent = new Event(Event.Type.TARGET_DIRECTORY_REMOVED);
                                eventsQueue.offer(queueEvent);
                            }
                        }
                    }
                    watchKey.reset();
                }
            } catch (InterruptedException e) {
                //terminated
            }
            watchService.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
