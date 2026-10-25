package com.example.serverapi.events;

import com.example.serverapi.logging.LogBuffer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {
    @Test
    void boundedLogBufferKeepsNewestEntries() {
        LogBuffer logs = new LogBuffer(2);
        logs.append("one"); logs.append("two"); logs.append("three");
        assertEquals(java.util.List.of("two", "three"), logs.recent(10));
    }

    @Test
    void eventBusFansOutAndCleansUpSubscriptions() throws Exception {
        EventBus bus = new EventBus(4);
        EventBus.Subscription subscription = bus.subscribe();
        ServerEvent event = new ServerEvent(1, "join", Map.of("name", "Alex"));
        bus.publish(event);
        assertEquals(event, subscription.poll(Duration.ofMillis(100)));
        assertEquals(1, bus.subscriberCount());
        subscription.close();
        assertEquals(0, bus.subscriberCount());
    }

    @Test
    void slowSubscriberDoesNotBlockPublisher() {
        EventBus bus = new EventBus(1);
        bus.subscribe();
        long started = System.nanoTime();
        for (int i = 0; i < 100; i++) bus.publish(new ServerEvent(i, "log", Map.of()));
        assertTrue((System.nanoTime() - started) < 500_000_000L);
    }
}
