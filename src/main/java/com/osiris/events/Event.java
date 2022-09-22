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
    /**
     * List of actions that get executed when this event happens. <br>
     * Stores actions in a {@link HashMap} instead of an {@link ArrayList}
     * so that there is always only one action per key.
     * See also: <br>
     * {@link #execute(Object)} <br>
     * {@link #putAction(Object key, BetterBiConsumer, Consumer)} <br>
     */
    private final Map<Object, Action<T>> actionsMap;
    public Thread cleanerThread;
    public Runnable cleanerRunnable;
    public Predicate<Object> removeCondition;
    public Consumer<Exception> onConditionException;

    /**
     * Creates a new event with an empty {@link #actions} list.
     */
    public Event() {
        this.actions = new ArrayList<>();
        this.actionsMap = new HashMap<>();
    }

    /**
     * Creates a new event.
     *
     * @param actions See {@link #actions}.
     */
    public Event(List<Action<T>> actions) {
        this.actions = actions;
        this.actionsMap = new HashMap<>();
    }

    /**
     * Creates a new event.
     *
     * @param actionsMap See {@link #actionsMap}.
     */
    public Event(Map<Object, Action<T>> actionsMap) {
        this.actions = new ArrayList<>();
        this.actionsMap = actionsMap;
    }

    public Event(List<Action<T>> actions, Map<Object, Action<T>> actionsMap) {
        this.actions = actions;
        this.actionsMap = actionsMap;
    }

    /**
     * Executes all the {@link #actions} + {@link #actionsMap} for this event. <br>
     * If {@link #removeCondition} is not null and valid the action gets not executed and removed from {@link #actions}. <br>
     * Note that you will get a {@link NullPointerException} when the condition check throws an exception and {@link #onConditionException} is null. <br>
     *
     * @param t optional object to pass over to the action, so that it has more information about the occured event.
     * @return this event for chaining.
     */
    public Event<T> execute(T t) {
        synchronized (this) {
            for (Action<T> action : actions) {
                executeAction(action, t);
            }
            actionsMap.forEach((key, action) -> {
                executeAction(action, t);
            });
            if (!actionsToRemove.isEmpty()) {
                removeRemovableActions();
            }
        }
        return this;
    }

    private void executeAction(Action<T> action, T t) {
        try {
            boolean skip = false;
            try {
                if (action.removeCondition != null) { // this has priority
                    if (action.removeCondition.test(action.object)) {
                        actionsToRemove.add(action);
                        skip = true;
                    }
                } else if (removeCondition != null && removeCondition.test(action.object)) {
                    actionsToRemove.add(action);
                    skip = true;
                }
            } catch (Exception e) {
                onConditionException.accept(e);
                skip = true;
            }

            if (!skip) {
                action.onEvent.accept(action, t);
                action.executionCount++;
            }
        } catch (Exception e) {
            action.onException.accept(e);
        }
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
        synchronized (this) {
            Action<T> action = new Action(onEvent, onException, isOneTime, object);
            actions.add(action);
            if (cleanerThread != null && !cleanerThread.isAlive()) {
                cleanerThread = new Thread(cleanerRunnable);
                cleanerThread.start();
            }
            return action;
        }
    }

    /*
    PUT ACTION METHODS FOR HASHMAP:
     */

    /**
     * Convenience method that throws {@link RuntimeException}
     * when something goes wrong inside the provided event code.
     */
    public Action<T> putAction(Object key, BetterConsumer<T> onEvent) {
        return putAction(key, (action, value) -> {
            onEvent.accept(value);
        }, ex -> {
            throw new RuntimeException(ex);
        }, false, null);
    }

    /**
     * Convenience method that throws {@link RuntimeException}
     * when something goes wrong inside the provided event code.
     */
    public Action<T> putOneTimeAction(Object key, BetterConsumer<T> onEvent) {
        return putAction(key, (action, value) -> {
            onEvent.accept(value);
        }, ex -> {
            throw new RuntimeException(ex);
        }, true, null);
    }

    /**
     * Convenience method, which ignores {@link Action} parameter from
     * {@link BetterBiConsumer}. Useful when the action does not
     * need to be referenced inside the event. <br>
     * See {@link #putAction(Object, BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> putAction(Object key, BetterConsumer<T> onEvent, Consumer<Exception> onException) {
        return putAction(key, (action, value) -> {
            onEvent.accept(value);
        }, onException, false, null);
    }

    /**
     * Convenience method, which ignores {@link Action} parameter from
     * {@link BetterBiConsumer}. Useful when the action does not
     * need to be referenced inside the event. <br>
     * See {@link #putAction(Object, BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> putOneTimeAction(Object key, BetterConsumer<T> onEvent, Consumer<Exception> onException) {
        return putAction(key, (action, value) -> {
            onEvent.accept(value);
        }, onException, true, null);
    }

    /**
     * Creates and puts a new action to the {@link #actionsMap}. <br>
     * Gets ran every time {@link #execute(Object)} was called. <br>
     * Usage: <br>
     * <pre>
     * event.putAction((action, value) -> {
     *     // do stuff
     * }, Exception::printStackTrace());
     * </pre>
     *
     * @param onEvent     See {@link Action#onEvent}.
     * @param onException See {@link Action#onException}.
     */
    public Action<T> putAction(Object key, BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException) {
        return putAction(key, onEvent, onException, false, null);
    }

    /**
     * Creates and puts a new action to the {@link #actionsMap}. <br>
     * Gets ran only once, when {@link #execute(Object)} was called and removed from the {@link #actionsMap} directly after being run. <br>
     * See {@link #putAction(Object, BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> putOneTimeAction(Object key, BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException) {
        return putAction(key, onEvent, onException, true, null);
    }

    /**
     * See {@link #putAction(Object, BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> putAction(Object key, BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException, boolean isOneTime) {
        return putAction(key, onEvent, onException, isOneTime, null);
    }

    /**
     * See {@link #putAction(Object, BetterBiConsumer, Consumer)} for details. <br>
     */
    public Action<T> putAction(Object key, BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException, boolean isOneTime, Object object) {
        synchronized (this) {
            Action<T> action = new Action(onEvent, onException, isOneTime, object);
            actionsMap.put(key, action);
            if (cleanerThread != null && !cleanerThread.isAlive()) {
                cleanerThread = new Thread(cleanerRunnable);
                cleanerThread.start();
            }
            return action;
        }
    }

    /**
     * Only affects {@link #actionsMap}. <br>
     * See {@link #removeAction(Action)} for details. <br>
     */
    public Event<T> removeActionByKey(Object key) {
        synchronized (this) {
            actionsMap.remove(key);
        }
        return this;
    }

    /**
     * Removes the provided action from the {@link #actions} and {@link #actionsToRemove} lists,
     * as well as the {@link #actionsMap}.
     * Note that you can remove an action directly in its event code
     * via {@link Action#remove()}.
     *
     * @return this event for chaining.
     */
    public Event<T> removeAction(Action<T> action) {
        synchronized (this) {
            actions.remove(action);
            actionsToRemove.remove(action);
            if (!actionsMap.isEmpty()) {
                List<Object> removable = new ArrayList<>();
                actionsMap.forEach((key, action2) -> {
                    if (action2.equals(action))
                        removable.add(key);
                });
                for (Object key : removable) {
                    actionsMap.remove(key);
                }
            }
        }
        return this;
    }

    public void removeRemovableActions() {
        for (Action<T> actionsToRemove : getActionsToRemove()) {
            removeAction(actionsToRemove);
        }
    }

    /**
     * Returns a copy of {@link #actions}.
     */
    public List<Action<T>> getActions() {
        synchronized (actions) {
            return new ArrayList<>(actions);
        }
    }

    /**
     * Returns a copy of {@link #actionsMap}.
     */
    public HashMap<Object, Action<T>> getActionsMap() {
        synchronized (actionsMap) {
            return new HashMap<Object, Action<T>>(actionsMap);
        }
    }

    /**
     * Returns a copy of {@link #actionsToRemove}.
     */
    public List<Action<T>> getActionsToRemove() {
        synchronized (actionsToRemove) {
            return new ArrayList<>(actionsToRemove);
        }
    }

    /**
     * Initialises a thread that removes actions from the {@link #actions} list
     * and the {@link #actionsMap} if the provided condition is true. <br>
     * Does nothing if already initialised. <br>
     * The initialised thread throws {@link RuntimeException} if something went wrong. <br>
     * Note that when {@link #actions} is empty the cleaner thread stops. <br>
     * A new cleaner thread gets created once a new action is added. <br>
     *
     * @param msBetweenChecks      the amount of milliseconds between each check.
     * @param removeCondition      when true, removes that action from the list.
     * @param onConditionException gets executed when something went wrong during condition checking.
     * @return this event for chaining.
     */
    public Event<T> initCleaner(int msBetweenChecks, Predicate<Object> removeCondition, Consumer<Exception> onConditionException) {
        if (cleanerThread != null) return this;
        this.removeCondition = removeCondition;
        this.onConditionException = onConditionException;
        final Object thisReference = this;
        cleanerRunnable = () -> {
            try {
                while (true) {
                    Thread.sleep(msBetweenChecks);
                    synchronized (thisReference) {
                        if (actions.isEmpty())
                            break; // Stops the thread, thread gets restarted when a new action is added
                        for (Action<T> action : actions) {
                            markAsRemovableIfNeeded(action);
                        }
                        actionsMap.forEach((key, action) -> {
                            markAsRemovableIfNeeded(action);
                        });
                        removeRemovableActions();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        cleanerThread = new Thread(cleanerRunnable);
        cleanerThread.start();
        return this;
    }

    private void markAsRemovableIfNeeded(Action<T> action) {
        try {
            if (action.removeCondition != null) { // this has priority
                if (action.removeCondition.test(action.object)) actionsToRemove.add(action);
            } else if (removeCondition.test(action.object)) actionsToRemove.add(action);
        } catch (Exception e) {
            onConditionException.accept(e);
        }
    }
}
