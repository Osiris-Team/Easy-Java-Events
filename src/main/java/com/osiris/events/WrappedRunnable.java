package com.osiris.events;

import java.util.HashSet;
import java.util.Set;

class WrappedRunnable {
    public int currentSleepSeconds;
    public Set<Event<?>> events = new HashSet<>();

    public WrappedRunnable(int currentSleepSeconds) {
        this.currentSleepSeconds = currentSleepSeconds;
    }
}
