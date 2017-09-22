package ru.skycelot.redeployer;

import java.util.concurrent.BlockingQueue;

public class Clock implements Runnable {

    private final BlockingQueue<Event> eventsQueue;

    public Clock(BlockingQueue<Event> eventsQueue) {
        this.eventsQueue = eventsQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Thread.sleep(1000);
                Event event = new Event(Event.Type.CLOCK_TICK);
                eventsQueue.offer(event);
            }
        } catch (InterruptedException e) {
            //terminated
        }
    }
}
