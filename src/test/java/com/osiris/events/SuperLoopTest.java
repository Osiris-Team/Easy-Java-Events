package com.osiris.events;

import org.junit.jupiter.api.Test;


class SuperLoopTest {
    @Test
    void general() {
        SuperLoop superLoop = new SuperLoop();
        superLoop.add(5, () -> {
            // Executed every 5 seconds
        });
    }
}