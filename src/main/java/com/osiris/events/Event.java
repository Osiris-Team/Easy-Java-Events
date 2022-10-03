/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Event<T> {
    /**
     * List of actions that get executed when this event happens. <br>
     * See also: <br>
     * {@link #execute(Object)} <br>
     * {@link #addAction(BetterBiConsumer, Consumer)} <br>
     */
    private final List<Action<T>> actions;
    private final List<Action<T>> actionsToRemove = new ArrayList<>();
    public Predicate<Object> defaultActionRemoveCondition;
    public Consumer<Exception> onConditionException;
    public int secondsBetweenChecks = 0;
    public Runnable cleanerRunnable;
    private static final Map<Integer, WrappedRunnable> map = new HashMap<>();
    /**
     * Single thread that executes {@link #cleanerRunnable} of every event. <br>
     * Responsible for event garbage collection. <br>
     */
    private static Thread mainCleanerThread = new Thread(() -> {
        try{
            while (true){
                Thread.sleep(1000);
                map.forEach((sleepSeconds, wrappedRunnable) -> {
                    wrappedRunnable.currentSleepSeconds--;
                    if(wrappedRunnable.currentSleepSeconds <= 0){
                        wrappedRunnable.currentSleepSeconds = sleepSeconds;
                        for (Event<?> event : wrappedRunnable.events) {
                            event.cleanerRunnable.run();
                        }
                        List<Event<?>> removableEvents = new ArrayList<>(0);
                        for (Event<?> event : wrappedRunnable.events) {
                            synchronized (event.actions){
                                if(event.actions.isEmpty())
                                    removableEvents.add(event);
                            }
                        }
                        for (Event<?> removableEvent : removableEvents) {
                            wrappedRunnable.events.remove(removableEvent);
                        }
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
    static{
        mainCleanerThread.setName("Easy-Java-Events-Cleaner");
        mainCleanerThread.start();
    }

    /**
     * Creates a new event with an empty {@link #actions} list.
     */
    public Event() {
        this.actions = new ArrayList<>();
    }

    /**
     * Creates a new event.
     *
     * @param actions See {@link #actions}.
     */
    public Event(List<Action<T>> actions) {
        this.actions = actions;
    }


    /**
     * Executes all the {@link #actions} for this event. <br>
     * If {@link #defaultActionRemoveCondition} is not null and valid the action gets not executed and removed from {@link #actions}. <br>
     * Note that you will get a {@link NullPointerException} when the condition check throws an exception and {@link #onConditionException} is null. <br>
     *
     * @param t optional object to pass over to the action, so that it has more information about the occured event.
     * @return this event for chaining.
     */
    public Event<T> execute(T t) {
        synchronized (actions) {
            for (Action<T> action : actions) {
                try {
                    if (!markActionAsRemovableIfNeeded(action)) {
                        action.onEvent.accept(action, t);
                        action.executionCount++;
                    }
                } catch (Exception e) {
                    action.onException.accept(e);
                }
            }
            removeActionsToRemove();
        }
        return this;
    }

    /**
     * Convenience method that throws {@link RuntimeException}
     * when something goes wrong inside the provided event code.
     */
    public Action<T> addAction(BetterConsumer<T> onEvent) {
        return addAction((action, value) -> {
            onEvent.accept(value);
        }, ex -> {
            throw new RuntimeException(ex);
        }, false, null);
    }

    /**
     * Convenience method that throws {@link RuntimeException}
     * when something goes wrong inside the provided event code.
     */
    public Action<T> addOneTimeAction(BetterConsumer<T> onEvent) {
        return addAction((action, value) -> {
            onEvent.accept(value);
        }, ex -> {
            throw new RuntimeException(ex);
        }, true, null);
    }

    /**
     * Convenience method, which ignores {@link Action} parameter from
     * {@link BetterBiConsumer}. Useful when the action does not
     * need to be referenced inside the event. <br>
     * See {@link #addAction(BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> addAction(BetterConsumer<T> onEvent, Consumer<Exception> onException) {
        return addAction((action, value) -> {
            onEvent.accept(value);
        }, onException, false, null);
    }

    /**
     * Convenience method, which ignores {@link Action} parameter from
     * {@link BetterBiConsumer}. Useful when the action does not
     * need to be referenced inside the event. <br>
     * See {@link #addAction(BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> addOneTimeAction(BetterConsumer<T> onEvent, Consumer<Exception> onException) {
        return addAction((action, value) -> {
            onEvent.accept(value);
        }, onException, true, null);
    }

    /**
     * Creates and adds a new action to the {@link #actions} list. <br>
     * Gets ran every time {@link #execute(Object)} was called. <br>
     * Usage: <br>
     * <pre>
     * event.addAction((action, value) -> {
     *     // do stuff
     * }, Exception::printStackTrace());
     * </pre>
     *
     * @param onEvent     See {@link Action#onEvent}.
     * @param onException See {@link Action#onException}.
     */
    public Action<T> addAction(BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException) {
        return addAction(onEvent, onException, false, null);
    }

    /**
     * Creates and adds a new action to the {@link #actions} list. <br>
     * Gets ran only once, when {@link #execute(Object)} was called and removed from the {@link #actions} list directly after being run. <br>
     * See {@link #addAction(BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> addOneTimeAction(BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException) {
        return addAction(onEvent, onException, true, null);
    }

    /**
     * See {@link #addAction(BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> addAction(BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException, boolean isOneTime) {
        return addAction(onEvent, onException, isOneTime, null);
    }

    /**
     * See {@link #addAction(BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> addAction(BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException, boolean isOneTime, Object object) {
        synchronized (actions) {
            Action<T> action = new Action(this, onEvent, onException, isOneTime, object);
            actions.add(action);
            if(cleanerRunnable != null)
                synchronized (map){
                    WrappedRunnable wrappedRunnable = map.get(secondsBetweenChecks);
                    if(wrappedRunnable == null){
                        wrappedRunnable = new WrappedRunnable(secondsBetweenChecks);
                        map.put(secondsBetweenChecks, wrappedRunnable);
                    }
                    wrappedRunnable.events.add(this);
                }
            return action;
        }
    }

    /**
     * Returns true if this action can be removed and adds it to the {@link #actionsToRemove} list. <br>
     */
    public boolean markActionAsRemovableIfNeeded(Action<T> action) {
        synchronized (actionsToRemove) {
            boolean removable = false;
            try {
                if (action.removeCondition != null) { // this has priority
                    if (action.removeCondition.test(action.object)) {
                        removable = true;
                        actionsToRemove.add(action);
                    }
                } else if (defaultActionRemoveCondition != null) {
                    if (defaultActionRemoveCondition.test(action.object)) {
                        removable = true;
                        actionsToRemove.add(action);
                    }
                }
            } catch (Exception e) {
                if (onConditionException == null) throw new RuntimeException(e);
                else onConditionException.accept(e);
                removable = true;
                actionsToRemove.add(action);
            }
            return removable;
        }
    }

    /**
     * Provided action will be removed from this event... <br>
     * ... before it gets executed the next time. <br>
     * ... by the {@link #cleanerRunnable}. <br>
     *
     * @return this event for chaining.
     */
    public Event<T> markActionAsRemovable(Action<T> action) {
        synchronized (actionsToRemove) {
            actionsToRemove.add(action); // To make sure it gets removed by the cleaner thread
            action.removeCondition = obj -> true; // To make sure it gets removed before being executed the next time
        }
        return this;
    }

    public void removeActionsToRemove() {
        synchronized (actionsToRemove) {
            if (!actionsToRemove.isEmpty()) {
                for (Action<T> action : actionsToRemove) {
                    actions.remove(action);
                }
                actionsToRemove.clear();
            }
        }
    }

    /**
     * Returns a copy of {@link #actions}.
     */
    public List<Action<T>> getActionsCopy() {
        synchronized (actions) {
            return new ArrayList<>(actions);
        }
    }

    /**
     * Returns a copy of {@link #actionsToRemove}.
     */
    public List<Action<T>> getActionsToRemoveCopy() {
        synchronized (actionsToRemove) {
            return new ArrayList<>(actionsToRemove);
        }
    }

    /**
     * @see #initCleaner(int, Predicate, Consumer)
     */
    public Event<T> initCleaner() {
        initCleaner(60,
                this.defaultActionRemoveCondition,
                (this.onConditionException != null ? this.onConditionException : ex -> {
                    throw new RuntimeException(ex);
                }));
        return this;
    }

    /**
     * Similar to {@link #initSimpleCleaner(int)} but checks the {@link #actions} via {@link #markActionAsRemovableIfNeeded(Action)}.
     *
     * @param secondsBetweenChecks              the amount of seconds between each check.
     * @param defaultActionRemoveCondition when true, removes that action from the list.
     * @param onConditionException         gets executed when something went wrong during condition checking.
     * @return this event for chaining.
     */
    public Event<T> initCleaner(int secondsBetweenChecks, Predicate<Object> defaultActionRemoveCondition, Consumer<Exception> onConditionException) {
        this.secondsBetweenChecks = secondsBetweenChecks;
        this.defaultActionRemoveCondition = defaultActionRemoveCondition;
        this.onConditionException = onConditionException;
        synchronized (map){
            WrappedRunnable wrappedRunnable = map.get(secondsBetweenChecks);
            if(wrappedRunnable == null){
                wrappedRunnable = new WrappedRunnable(secondsBetweenChecks);
                map.put(secondsBetweenChecks, wrappedRunnable);
            }
            this.cleanerRunnable = () -> {
                try {
                    synchronized (actions) {
                        for (Action<T> action : actions) {
                            try {
                                markActionAsRemovableIfNeeded(action);
                            } catch (Exception e) {
                                onConditionException.accept(e);
                            }
                        }
                        removeActionsToRemove();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            wrappedRunnable.events.add(this);
        }
        return this;
    }

    /**
     * @see #initSimpleCleaner(int)
     */
    public Event<T> initSimpleCleaner() {
        initSimpleCleaner(60);
        return this;
    }

    /**
     * Simple thread that removes actions from this event periodically if they exist inside the {@link #actionsToRemove} list. <br>
     * It won't check the actions via the {@link Action#removeCondition} to save performance. <br>
     * Does nothing if already initialised. <br>
     * The initialised thread throws {@link RuntimeException} if something went wrong. <br>
     *
     * @param secondsBetweenChecks the amount of seconds between each check.
     * @return this event for chaining.
     * @see #removeActionsToRemove()
     */
    public Event<T> initSimpleCleaner(int secondsBetweenChecks) {
        this.secondsBetweenChecks = secondsBetweenChecks;
        synchronized (map){
            WrappedRunnable wrappedRunnable = map.get(secondsBetweenChecks);
            if(wrappedRunnable == null){
                wrappedRunnable = new WrappedRunnable(secondsBetweenChecks);
                map.put(secondsBetweenChecks, wrappedRunnable);
            }
            this.cleanerRunnable = () -> {
                try {
                    removeActionsToRemove();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            wrappedRunnable.events.add(this);
        }
        return this;
    }
}
