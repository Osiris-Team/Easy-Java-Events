package com.osiris.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {
    @Test
    void example() throws InterruptedException {
        Event<Integer> onValueChanged = new Event<>();
        Action<Integer> myAction = onValueChanged.addAction(value -> { // Stays in memory and gets executed every time.
            System.out.println("New value: " + value);
            // You can throw exceptions in here
        }, Exception::printStackTrace); // and catch them here.

        onValueChanged.execute(10); // Prints out "New value: 10"
        onValueChanged.execute(5); // Prints out "New value: 5"

        // One time actions that only get executed once, are also supported:
        onValueChanged.addOneTimeAction(value -> {
            System.out.println("New value: " + value + " bye!");
        }, Exception::printStackTrace);

        onValueChanged.execute(7); // Prints out "New value: 7" and "New value: 7 bye!"

        // If more and more actions get added over time
        // you must remove unused actions. There are some util methods for this case.
        // First we create a new action with additional params: isOneTime=false and object=null.
        Action<Integer> actionToRemove = onValueChanged.addAction((action, value) -> {
            System.out.println("New value: " + value + ", but I will be gone soon :/");
        }, Exception::printStackTrace, false, null);

        // Then we initialise the cleaner thread for this event, which checks
        // its actions list every 1s for actions that
        // fulfill the condition "object != null" and removes those.
        onValueChanged.initCleaner(1, object -> object != null, Exception::printStackTrace);

        // Once we want to remove the action, we simply give it an object that is not null.
        // The cleaner then removes it in the next check.
        actionToRemove.object = new Object(); // Note that you can store any type of object here.


        // Actual tests:
        Thread.sleep(3000);
        assertEquals(1, onValueChanged.getActionsCopy().size());
        onValueChanged.markActionAsRemovable(myAction);
        Thread.sleep(3000);
        onValueChanged.addAction((a, value) -> {
            System.out.println("New value: " + value);
        }, Exception::printStackTrace);
    }
}