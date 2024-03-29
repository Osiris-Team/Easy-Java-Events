/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.events;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Action<T> {
    /**
     * If you want to disable this, changing this to false will
     * have no affect. Instead, set {@link #removeCondition} to null.
     */
    public final boolean isOneTime;
    /**
     * Skips all actions that come after this one and prevents their execution.
     */
    public boolean isSkipNextActions = false;
    public Event<T> event;
    /**
     * Holds code. Gets executed on an event.
     * Has itself as first parameter and the value T as second one.
     */
    public BetterBiConsumer<Action<T>, T> onEvent;
    /**
     * Holds code. Gets executed when an exception was thrown in the code held by {@link #onEvent}. <br>
     * Can be null. <br>
     */
    public Consumer<Exception> onException;
    public long executionCount = 0;
    /**
     * Can be null. <br>
     * If not null, {@link Event#defaultActionRemoveCondition} gets ignored for this action. <br>
     * If true this action gets removed from the {@link Event#getActionsCopy()} list. <br>
     * This actions' {@link #object} is used to test this condition.
     */
    public Predicate<Object> removeCondition;
    /**
     * Optional object of your choice, to hold information specific to this action. <br>
     * Can be any type of object, like a String for example (remember to cast). <br>
     * Can be null. <br>
     */
    public Object object;



    /**
     * Creates an action.
     *
     * @param onEvent     See {@link Action#onEvent}.
     * @param onException See {@link Action#onException}.
     * @param isOneTime   See {@link Action#isOneTime}.
     * @param object      See {@link Action#object}.
     */
    public Action(Event<T> event, BetterBiConsumer<Action<T>, T> onEvent, Consumer<Exception> onException, boolean isOneTime, Object object) {
        this.event = event;
        this.onEvent = onEvent;
        this.onException = onException;
        this.isOneTime = isOneTime;
        this.object = object;
        if (isOneTime) oneTime();
    }

    /**
     * Marks this action to be removed from the event It's attached to. <br>
     * Note that this will not happen directly, see {@link Event#markActionAsRemovable(Action)} for details.
     */
    public Action<T> remove() {
        event.markActionAsRemovable(this);
        return this;
    }

    public Action<T> oneTime() {
        removeCondition = obj -> executionCount >= 1;
        return this;
    }

    public Action<T> skipNextActions(){
        isSkipNextActions = true;
        return this;
    }

    public Action<T> skipNextActions(boolean b){
        isSkipNextActions = b;
        return this;
    }
}
