package ru.skycelot.redeployer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Coordinator {

    private final BlockingQueue<Event> eventsQueue;
    private Path currentWarFile;
    private Path undeployFlagFile;
    private State currentState;
    private Instant lastTimeActivity;
    private DateTimeFormatter dateTimeFormatter;
    private final Path projectPath;
    private final ProjectWatcher projectWatcher;
    private Thread projectWatcherThread;
    private final Path projectTargetPath;
    private final ProjectTargetWatcher projectTargetWatcher;
    private Thread projectTargetWatcherThread;
    private final Path wildflyDeploymentsPath;
    private final WildflyDeploymentsWatcher wildflyDeploymentsWatcher;
    private Thread wildflyDeploymentsWatcherThread;
    private Clock clock;
    private Thread clockThread;
    private int delay;

    public Coordinator(Path projectPath, Path projectTargetPath, Path wildflyDeploymentsPath, String timeFormatPattern, int delay) {
        eventsQueue = new LinkedBlockingQueue<>();
        dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern);
        this.projectPath = projectPath;
        this.projectTargetPath = projectTargetPath;
        this.wildflyDeploymentsPath = wildflyDeploymentsPath;
        this.projectWatcher = new ProjectWatcher(projectPath, projectTargetPath, eventsQueue);
        this.projectTargetWatcher = new ProjectTargetWatcher(projectTargetPath, eventsQueue);
        this.wildflyDeploymentsWatcher = new WildflyDeploymentsWatcher(wildflyDeploymentsPath, eventsQueue);
        this.clock = new Clock(eventsQueue);
        this.delay = delay;
    }

    public void start() {
        clearState();
        startProjectWatcher();
        if (Files.isDirectory(projectTargetPath)) {
            startProjectTargetWatcher();
        }
        startClockService();
        try {
            while (!Thread.interrupted()) {
                Event event = eventsQueue.take();
                if (event.type == Event.Type.TARGET_DIRECTORY_REMOVED) {
                    stopProjectTargetWatcher();
                    clearState();
                } else if (event.type == Event.Type.TARGET_DIRECTORY_CREATED) {
                    startProjectTargetWatcher();
                    clearState();
                } else if (event.type == Event.Type.WAR_FILE_ACTIVITY && currentState == State.WATCHING) {
                    currentState = State.WAR_DELAY;
                    currentWarFile = event.file;
                    lastTimeActivity = Instant.now();
                } else if (event.type == Event.Type.WAR_FILE_ACTIVITY && currentState == State.WAR_DELAY) {
                    if (currentWarFile.getFileName().equals(event.file.getFileName())) {
                        lastTimeActivity = Instant.now();
                    }
                } else if (event.type == Event.Type.UNDEPLOY_COMPLETED && currentState == State.UNDEPLOYING) {
                    stopWildflyDeploymentsWatcher();
                    currentState = State.UNDEPLOY_DELAY;
                    undeployFlagFile = event.file;
                } else if (event.type == Event.Type.DEPLOY_COMPLETED && currentState == State.DEPLOYING) {
                    System.out.println(dateTimeFormatter.format(LocalTime.now()) + "  " + currentWarFile + " was successfully redeployed.");
                    stopWildflyDeploymentsWatcher();
                    clearState();
                } else if (event.type == Event.Type.DEPLOY_FAILED && currentState == State.DEPLOYING) {
                    System.out.println(dateTimeFormatter.format(LocalTime.now()) + "  " + currentWarFile + " redeploy failed!");
                    stopWildflyDeploymentsWatcher();
                    FileUtils.removeFile(wildflyDeploymentsPath, currentWarFile);
                    FileUtils.removeFile(wildflyDeploymentsPath, event.file);
                    clearState();
                } else if (event.type == Event.Type.CLOCK_TICK) {
                    Instant waitUntilTime = lastTimeActivity.plus(delay, ChronoUnit.SECONDS);
                    Instant now = Instant.now();
                    if (currentState == State.WAR_DELAY && waitUntilTime.isBefore(now)) {
                        System.out.println(dateTimeFormatter.format(LocalTime.now()) + "  " + currentWarFile + " detected, starting redeploy sequence...");
                        startWildflyDeploymentsWatcher();
                        boolean prevVersionExisted = FileUtils.removeFile(wildflyDeploymentsPath, currentWarFile);
                        if (prevVersionExisted) {
                            currentState = State.UNDEPLOYING;
                        } else {
                            currentState = State.DEPLOYING;
                            FileUtils.copyFile(projectTargetPath, currentWarFile, wildflyDeploymentsPath);
                        }
                    } else if (currentState == State.UNDEPLOY_DELAY && waitUntilTime.isBefore(now)) {
                        FileUtils.removeFile(wildflyDeploymentsPath, undeployFlagFile);
                        startWildflyDeploymentsWatcher();
                        currentState = State.DEPLOYING;
                        FileUtils.copyFile(projectTargetPath, currentWarFile, wildflyDeploymentsPath);
                    }
                }
            }
        } catch (InterruptedException e) {
            // terminated
        }
        clockThread.interrupt();
        projectWatcherThread.interrupt();
        if (projectTargetWatcherThread.isAlive()) {
            projectTargetWatcherThread.interrupt();
        }
        if (wildflyDeploymentsWatcherThread.isAlive()) {
            wildflyDeploymentsWatcherThread.interrupt();
        }
    }

    private void startProjectWatcher() {
        projectWatcherThread = new Thread(projectWatcher);
        projectWatcherThread.start();
    }

    private void startClockService() {
        clockThread = new Thread(clock);
        clockThread.start();
    }

    private void startProjectTargetWatcher() {
        projectTargetWatcherThread = new Thread(projectTargetWatcher);
        projectTargetWatcherThread.start();
    }

    private void stopProjectTargetWatcher() {
        projectTargetWatcherThread.interrupt();
    }

    private void startWildflyDeploymentsWatcher() {
        wildflyDeploymentsWatcherThread = new Thread(wildflyDeploymentsWatcher);
        wildflyDeploymentsWatcherThread.start();
    }

    private void stopWildflyDeploymentsWatcher() {
        wildflyDeploymentsWatcherThread.interrupt();
    }

    private void clearState() {
        currentState = State.WATCHING;
        currentWarFile = null;
        undeployFlagFile = null;
        lastTimeActivity = Instant.now();
    }

    public enum State {
        WATCHING, WAR_DELAY, UNDEPLOYING, UNDEPLOY_DELAY, DEPLOYING
    }
}
