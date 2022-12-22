package com.osiris.events;

import java.util.ArrayList;
import java.util.List;

public class LoopCode {
    public final int interval;
    public int intervalLeft;
    public List<Runnable> runnables = new ArrayList<>();

    public LoopCode(int interval) {
        this.interval = interval;
        this.intervalLeft = interval;
    }
}
