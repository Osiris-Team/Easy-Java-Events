package com.osiris.events;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SerialisationTest {
    @Test
    void test() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream parser = new ObjectOutputStream(out);

        Event<Object> originalEvent = new Event<>().initCleaner();
        AtomicInteger actionsTriggered = new AtomicInteger();
        originalEvent.addAction((o) -> {
            actionsTriggered.incrementAndGet();
        }, Exception::printStackTrace);

        parser.writeObject(originalEvent);

        Event<?> clonedEvent = (Event<?>) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        assertNotNull(clonedEvent);
        assertEquals(1, clonedEvent.actions.size());

        clonedEvent.execute(null);
        clonedEvent.execute(null);
        clonedEvent.execute(null);
        assertEquals(3, actionsTriggered.get());

    }
}
