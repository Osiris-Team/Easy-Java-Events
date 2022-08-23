# Easy-Java-Events
EJE provides accessible methods for handling events/actions/listeners.
Add this as dependency to your project via
[Maven/Gradle/Sbt/Leinigen](https://jitpack.io/#Osiris-Team/Easy-Java-Events/LATEST) (requires Java 8 or higher).

It should take you 30 seconds to learn how this lib works. 
Just skim over the code below.
```java
class EventTest {
    @Test
    void example() {
        Event<Integer> onValueChanged = new Event<>();
        Action<Integer> myAction = onValueChanged.addAction(value -> { // Stays in memory and gets executed every time.
            System.out.println("New value: "+value);
            // You can throw exceptions in here
        }, Exception::printStackTrace); // and catch them here.

        onValueChanged.execute(10); // Prints out "New value: 10"
        onValueChanged.execute(5); // Prints out "New value: 5"

        // One time actions that only get executed once, are also supported:
        onValueChanged.addOneTimeAction(value -> {
            System.out.println("New value: "+value+" bye!");
        }, Exception::printStackTrace);

        onValueChanged.execute(7); // Prints out "New value: 7" and "New value: 7 bye!"

        // If more and more actions get added over time
        // you must remove unused actions. There are some util methods for this case.
        // First we create a new action with additional params: isOneTime=false and object=null.
        Action<Integer> actionToRemove = onValueChanged.addAction((action, value) -> {
            System.out.println("New value: "+value+", but I will be gone soon :/");
        }, Exception::printStackTrace, false, null);

        // Then we initialise the cleaner thread for this event, which checks
        // its actions list every 100ms for actions that
        // fulfill the condition "object != null" and removes those.
        onValueChanged.initCleaner(100, object -> object != null, Exception::printStackTrace);

        // Once we want to remove the action, we simply give it an object that is not null.
        // The cleaner then removes it in the next check.
        actionToRemove.object = new Object(); // Note that you can store any type of object here.
    }
}
