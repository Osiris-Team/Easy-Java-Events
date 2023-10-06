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
public interface SConsumer<T> extends Serializable {

    /**
     * @see java.util.function.Consumer
     */
    void accept(T t) throws Exception;

    /**
     * @see java.util.function.Consumer
     */
    default SConsumer<T> andThen(SConsumer<T> after) throws Exception {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}

