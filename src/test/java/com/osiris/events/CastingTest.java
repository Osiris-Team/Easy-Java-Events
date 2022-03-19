package com.osiris.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CastingTest {
    @Test
    void test() throws InterruptedException {
        Event<Integer> onValueChanged = new Event<>();
        Action<Integer> actionToRemove = onValueChanged.addAction(value -> {
            System.out.println("New value: "+value+", but I will be gone soon :/");
        }, Exception::printStackTrace, false, null);
        onValueChanged.initCleaner(100, object -> (Boolean) object, Exception::printStackTrace);
        actionToRemove.object = Boolean.TRUE;
        Thread.sleep(300);
        assertEquals(0, onValueChanged.getActions().size());
    }
}
