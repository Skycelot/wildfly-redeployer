package ru.skycelot.redeployer;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;

public class ProjectTargetWatcher implements Runnable {

    private final Path projectTargetPath;
    private final BlockingQueue<Event> eventsQueue;

    public ProjectTargetWatcher(Path projectTargetPath, BlockingQueue<Event> eventsQueue) {
        this.projectTargetPath = projectTargetPath;
        this.eventsQueue = eventsQueue;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            projectTargetPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            try {
                while (!Thread.interrupted()) {
                    WatchKey watchKey = watchService.take();
                    for (WatchEvent<?> untypedWatchEvent : watchKey.pollEvents()) {
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) untypedWatchEvent;
                        if (watchEvent.context().getFileName().toString().endsWith(".war")) {
                            Event queueEvent = new Event(Event.Type.WAR_FILE_ACTIVITY, watchEvent.context());
                            eventsQueue.offer(queueEvent);
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
