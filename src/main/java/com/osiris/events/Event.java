/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Event<T>{
    /**
     * List of actions that get executed when this event happens. <br>
     * Also known under event listeners. <br>
     * Be aware of concurrency issues, use methods of this class instead to avoid them! <br>
     */
    private final List<Action<T>> actions;
    private final List<Action<T>> actionsToRemove = new ArrayList<>();
    public Thread cleanerThread;
    public Runnable cleanerRunnable;
    public Predicate<Object> removeCondition;
    public Consumer<Exception> onConditionException;

    /**
     * Creates a new event with an empty {@link #actions} list.
     */
    public Event() {
        this.actions = new ArrayList<>();
    }

    /**
     * Creates a new event.
     * @param actions See {@link #actions}.
     */
    public Event(List<Action<T>> actions) {
        this.actions = actions;
    }

    /**
     * Executes all the {@link #actions} for this event. <br>
     * If {@link #removeCondition} is not null and valid the action gets not executed and removed from {@link #actions}. <br>
     * Note that you will get a {@link NullPointerException} when the condition check throws an exception and {@link #onConditionException} is null. <br>
     * @param t optional object to pass over to the action, so that it has more information about the occured event.
     * @return this event for chaining.
     */
    public Event<T> execute(T t){
        synchronized (actions){
            synchronized (actionsToRemove){
                for (Action<T> action:
                        actions) {
                    try{
                        boolean skip = false;
                        try{
                            if(action.removeCondition != null){ // this has priority
                                if(action.removeCondition.test(action.object)){
                                    actionsToRemove.add(action);
                                    skip = true;
                                }
                            }
                            else if(removeCondition!=null && removeCondition.test(action.object)){
                                actionsToRemove.add(action);
                                skip = true;
                            }
                        } catch (Exception e) {
                            onConditionException.accept(e);
                            skip = true;
                        }

                        if(!skip){
                            action.onEvent.accept(t);
                            if(action.isOneTime) actionsToRemove.add(action);
                        }
                    } catch (Exception e) {
                        action.onException.accept(e);
                    }
                }
                if(!actionsToRemove.isEmpty()) {
                    actions.removeAll(actionsToRemove);
                    actionsToRemove.clear();
                }
            }
        }
        return this;
    }

    /**
     * Creates and adds a new action to the {@link #actions} list. <br>
     * Gets ran every time {@link #execute(Object)} was called. <br>
     * @param onEvent See {@link Action#onEvent}.
     * @param onException See {@link Action#onException}.
     */
    public Action<T> addAction(ConsumerWithException<T> onEvent, Consumer<Exception> onException){
        return addAction(onEvent, onException, false, null);
    }

    /**
     * Creates and adds a new action to the {@link #actions} list. <br>
     * Gets ran only once, when {@link #execute(Object)} was called and removed from the {@link #actions} list directly after being run. <br>
     * @param onEvent See {@link Action#onEvent}.
     * @param onException See {@link Action#onException}.
     */
    public Action<T> addOneTimeAction(ConsumerWithException<T> onEvent, Consumer<Exception> onException){
        return addAction(onEvent, onException, true, null);
    }

    public Action<T> addAction(ConsumerWithException<T> onEvent, Consumer<Exception> onException, boolean isOneTime){
        return addAction(onEvent, onException, isOneTime, null);
    }

    public Action<T> addAction(ConsumerWithException<T> onEvent, Consumer<Exception> onException, boolean isOneTime, Object object){
        synchronized (actions){
            Action<T> action = new Action<>(onEvent, onException, isOneTime, object);
            actions.add(action);
            if(cleanerThread != null && !cleanerThread.isAlive()){
                cleanerThread = new Thread(cleanerRunnable);
                cleanerThread.start();
            }
            return action;
        }
    }

    /**
     * Removes the provided action from the {@link #actions} and {@link #actionsToRemove} lists.
     * @return this event for chaining.
     */
    public Event<T> removeAction(Action<T> action){
        synchronized (actions){
            synchronized (actionsToRemove){
                actions.remove(action);
                actionsToRemove.remove(action);
            }
        }
        return this;
    }

    /**
     * Returns a copy of {@link #actions}.
     */
    public List<Action<T>> getActions(){
        synchronized (actions){
            return new ArrayList<>(actions);
        }
    }

    /**
     * Returns a copy of {@link #actionsToRemove}.
     */
    public List<Action<T>> getActionsToRemove(){
        synchronized (actionsToRemove){
            return new ArrayList<>(actionsToRemove);
        }
    }

    /**
     * Initialises a thread that removes actions from the {@link #actions} list if the provided condition is true. <br>
     * Does nothing if already initialised. <br>
     * The initialised thread throws {@link RuntimeException} if something went wrong. <br>
     * Note that when {@link #actions} is empty the cleaner thread stops. <br>
     * A new cleaner thread gets created once a new action is added. <br>
     * @param msBetweenChecks the amount of milliseconds between each check.
     * @param removeCondition when true, removes that action from the list.
     * @param onConditionException gets executed when something went wrong during condition checking.
     * @return this event for chaining.
     */
    public Event<T> initCleaner(int msBetweenChecks, Predicate<Object> removeCondition, Consumer<Exception> onConditionException){
        if (cleanerThread != null) return this;
        this.removeCondition = removeCondition;
        this.onConditionException = onConditionException;
        cleanerRunnable = () -> {
            try{
                while (true){
                    Thread.sleep(msBetweenChecks);
                    synchronized (actions){
                        if(actions.isEmpty()) break; // Stops the thread, thread gets restarted when a new action is added
                        synchronized (actionsToRemove){
                            for (Action<T> action :
                                    actions) {
                                try{
                                    if(action.removeCondition != null){ // this has priority
                                        if(action.removeCondition.test(action.object)) actionsToRemove.add(action);
                                    }
                                    else if(removeCondition.test(action.object)) actionsToRemove.add(action);
                                } catch (Exception e) {
                                    onConditionException.accept(e);
                                }
                            }
                        }
                        actions.removeAll(actionsToRemove);
                        actionsToRemove.clear();
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
}
