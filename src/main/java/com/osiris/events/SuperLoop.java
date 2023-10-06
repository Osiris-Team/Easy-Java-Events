package com.osiris.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SuperLoop implements Serializable {
    public final int sleepIntervallMillis;
    public final Thread thread;
    public final List<LoopCode> list = new ArrayList<>();

    public SuperLoop() {
        this(1000);
    }

    public SuperLoop(int sleepIntervallMillis) {
        this.sleepIntervallMillis = sleepIntervallMillis;
        this.thread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(sleepIntervallMillis);
                    synchronized (list) {
                        for (LoopCode loopCode : list) {
                            loopCode.intervalLeft--;
                            if (loopCode.intervalLeft <= 0) {
                                loopCode.intervalLeft = loopCode.interval;
                                for (Runnable runnable : loopCode.runnables) {
                                    runnable.run();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setName(sleepIntervallMillis + "ms-" + this.getClass().getSimpleName() + "#" + Integer.toHexString(this.hashCode()));
        thread.start();
    }

    public void add(int interval, Runnable runnable) {
        synchronized (list) {
            LoopCode loopCode = null;
            for (LoopCode le : list) {
                if (le.interval == interval)
                    loopCode = le;
            }
            if (loopCode == null) {
                loopCode = new LoopCode(interval);
                list.add(loopCode);
            }
            loopCode.runnables.add(runnable);
        }
    }

    public void addAll(int interval, Collection<Runnable> runnables) {
        synchronized (list) {
            LoopCode loopCode = null;
            for (LoopCode le : list) {
                if (le.interval == interval)
                    loopCode = le;
            }
            if (loopCode == null) {
                loopCode = new LoopCode(interval);
                list.add(loopCode);
            }
            loopCode.runnables.addAll(runnables);
        }
    }

    public void remove(int interval) {
        synchronized (list) {
            List<LoopCode> toRemove = new ArrayList<>();
            for (LoopCode l : list) {
                if (l.interval == interval)
                    toRemove.add(l);
            }
            list.removeAll(toRemove);
        }
    }

    public void remove(Runnable runnable) {
        synchronized (list) {
            for (LoopCode l : list) {
                l.runnables.remove(runnable);
            }
        }
    }

    public void removeAll(Collection<Runnable> runnables) {
        synchronized (list) {
            for (LoopCode l : list) {
                l.runnables.removeAll(runnables);
            }
        }
    }
}
