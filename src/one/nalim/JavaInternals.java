/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class JavaInternals {
    public static final Unsafe unsafe = getUnsafe();

    private static final long accessibleOffset = getAccessibleOffset();

    private static Unsafe getUnsafe() {
        try {
            return (Unsafe) getPrivateField(Unsafe.class, "theUnsafe").get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long getAccessibleOffset() {
        boolean hasModules = !System.getProperty("java.version").startsWith("1.");
        if (hasModules) {
            try {
                Method m0 = JavaInternals.class.getDeclaredMethod("getAccessibleOffset");
                Method m1 = JavaInternals.class.getDeclaredMethod("getAccessibleOffset");
                m1.setAccessible(true);

                // Work around JDK 17 restrictions on calling setAccessible()
                for (long offset = 8; offset < 128; offset++) {
                    if (unsafe.getByte(m0, offset) == 0 && unsafe.getByte(m1, offset) == 1) {
                        return offset;
                    }
                }
            } catch (Exception e) {
                // Fall back to default setAccessible() implementation
            }
        }
        return 0;
    }

    public static Field getPrivateField(Class<?> cls, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            setAccessible(f);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Method getPrivateMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getDeclaredMethod(name, params);
            setAccessible(m);
            return m;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> Constructor<T> getPrivateConstructor(Class<T> cls, Class<?>... params) {
        try {
            Constructor<T> c = cls.getDeclaredConstructor(params);
            setAccessible(c);
            return c;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void setAccessible(AccessibleObject ao) {
        if (accessibleOffset != 0) {
            unsafe.putByte(ao, accessibleOffset, (byte) 1);
        } else {
            ao.setAccessible(true);
        }
    }
}
