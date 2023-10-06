package com.osiris.events;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

class WrappedRunnable implements Serializable {
    public int currentSleepSeconds;
    public Set<Event<?>> events = new HashSet<>();

    public WrappedRunnable(int currentSleepSeconds) {
        this.currentSleepSeconds = currentSleepSeconds;
    }
}
