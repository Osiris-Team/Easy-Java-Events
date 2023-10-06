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
 *
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface BetterBiConsumer<A, T> {

    /**
     * @see java.util.function.Consumer
     */
    void accept(A a, T t) throws Exception;

    /**
     * @see java.util.function.Consumer
     */
    default BetterBiConsumer<A, T> andThen(BetterBiConsumer<A, T> after) throws Exception {
        Objects.requireNonNull(after);
        return (A a, T t) -> {
            accept(a, t);
            after.accept(a, t);
        };
    }
}

