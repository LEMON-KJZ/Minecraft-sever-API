package com.example.serverapi.events;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class EventBus {
    private final int queueCapacity;
    private final CopyOnWriteArrayList<Subscription> subscribers = new CopyOnWriteArrayList<>();

    public EventBus(int queueCapacity) {
        if (queueCapacity < 1) throw new IllegalArgumentException("queueCapacity must be positive");
        this.queueCapacity = queueCapacity;
    }

    public Subscription subscribe() {
        Subscription subscription = new Subscription();
        subscribers.add(subscription);
        return subscription;
    }

    public void publish(ServerEvent event) { subscribers.forEach(subscription -> subscription.offer(event)); }
    public int subscriberCount() { return subscribers.size(); }
    public void closeAll() { subscribers.forEach(Subscription::close); }

    public final class Subscription implements AutoCloseable {
        private final BlockingQueue<ServerEvent> queue = new LinkedBlockingQueue<>(queueCapacity);
        private volatile boolean closed;
        private boolean offer(ServerEvent event) { return !closed && queue.offer(event); }
        public ServerEvent poll(Duration timeout) throws InterruptedException { return queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        public boolean isClosed() { return closed; }
        @Override public void close() { if (!closed) { closed = true; subscribers.remove(this); queue.clear(); } }
    }
}
