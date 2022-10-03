package com.osiris.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventCleanerTest {
    @Test
    void testCleaner() throws InterruptedException {
        Event<Void> event = new Event<Void>()
                .initCleaner(1, obj -> obj != null && ((Boolean) obj).booleanValue(), Exception::printStackTrace);
        for (int i = 0; i < 10000; i++) {
            event.addAction(value -> {
                // Do nothing
            });
        }
        List<Action<Void>> actions = event.getActionsCopy();
        assertEquals(10000, actions.size());
        for (int i = 0; i < 5000; i++) {
            actions.get(i).object = true;
        }

        Thread.sleep(5000);
        assertEquals(5000, event.getActionsCopy().size());
    }

    @Test
    void testSimpleCleaner() throws InterruptedException {
        Event<Void> event = new Event<Void>()
                .initSimpleCleaner(1);
        for (int i = 0; i < 10000; i++) {
            event.addAction(value -> {
                // Do nothing
            });
        }
        List<Action<Void>> actions = event.getActionsCopy();
        assertEquals(10000, actions.size());
        for (int i = 0; i < 5000; i++) {
            actions.get(i).remove();
        }

        Thread.sleep(1000);
        assertEquals(5000, event.getActionsCopy().size());
    }

    @Test
    void testSimpleCleaner2() throws InterruptedException {
        Event<Void> event = new Event<Void>()
                .initSimpleCleaner(1);
        for (int i = 0; i < 10000; i++) {
            event.addOneTimeAction(value -> {
                // Do nothing
            });
        }

        event.execute(null);
        event.execute(null);

        Thread.sleep(1000);
        assertEquals(0, event.getActionsCopy().size());
    }
}
