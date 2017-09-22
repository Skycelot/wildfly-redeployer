package ru.skycelot.redeployer;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;

public class WildflyDeploymentsWatcher implements Runnable {

    private final Path wildflyDeploymentPath;
    private final BlockingQueue<Event> eventsQueue;

    public WildflyDeploymentsWatcher(Path wildflyDeploymentPath, BlockingQueue<Event> eventsQueue) {
        this.wildflyDeploymentPath = wildflyDeploymentPath;
        this.eventsQueue = eventsQueue;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            wildflyDeploymentPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            try {
                while (!Thread.interrupted()) {
                    WatchKey watchKey = watchService.take();
                    for (WatchEvent<?> untypedWatchEvent : watchKey.pollEvents()) {
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) untypedWatchEvent;
                        if (watchEvent.context().getFileName().toString().endsWith(".undeployed")) {
                            Event queueEvent = new Event(Event.Type.UNDEPLOY_COMPLETED, watchEvent.context());
                            eventsQueue.offer(queueEvent);
                        } else if (watchEvent.context().getFileName().toString().endsWith(".deployed")) {
                            Event queueEvent = new Event(Event.Type.DEPLOY_COMPLETED, watchEvent.context());
                            eventsQueue.offer(queueEvent);
                        } else if (watchEvent.context().getFileName().toString().endsWith(".failed")) {
                            Event queueEvent = new Event(Event.Type.DEPLOY_FAILED, watchEvent.context());
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
