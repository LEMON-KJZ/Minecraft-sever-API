package com.example.serverapi.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LogBuffer {
    private final int capacity;
    private final Deque<String> entries = new ArrayDeque<>();

    public LogBuffer(int capacity) { if (capacity < 1) throw new IllegalArgumentException("capacity must be positive"); this.capacity = capacity; }
    public synchronized void append(String line) { if (line == null) return; entries.addLast(line); while (entries.size() > capacity) entries.removeFirst(); }
    public synchronized List<String> recent(int limit) { int count = Math.max(0, Math.min(limit, entries.size())); return List.copyOf(entries.stream().skip(entries.size() - count).toList()); }
}
