/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the name of the native shared library to load.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Library {
    String value();
}
