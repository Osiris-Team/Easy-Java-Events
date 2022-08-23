/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.events;

import java.util.Objects;

/**
 * Makes handling exceptions easier.
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface BetterConsumer<T> {

    /**
     * @see java.util.function.Consumer
     */
    void accept(T t) throws Exception;

    /**
     * @see java.util.function.Consumer
     */
    default BetterConsumer<T> andThen(BetterConsumer<T> after) throws Exception {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}

