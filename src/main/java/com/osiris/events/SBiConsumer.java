/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.events;

import java.io.Serializable;
import java.util.Objects;

/**
 * Makes handling exceptions easier.
 *
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface SBiConsumer<A, T> extends Serializable {

    /**
     * @see java.util.function.Consumer
     */
    void accept(A a, T t) throws Exception;

    /**
     * @see java.util.function.Consumer
     */
    default SBiConsumer<A, T> andThen(SBiConsumer<A, T> after) throws Exception {
        Objects.requireNonNull(after);
        return (A a, T t) -> {
            accept(a, t);
            after.accept(a, t);
        };
    }
}

