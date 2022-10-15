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
 * Denotes a method for linking with the native code. 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Link {
    /**
     * Alternative name of the native function.
     * If not specified, Java method name is assumed.
     */
    String name() default "";

    /**
     * true, if the target method uses Java calling convention;
     * false, if the Linker should generate a prologue
     * for translating arguments according to the native ABI.
     */
    boolean naked() default false;
}
