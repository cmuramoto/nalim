/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIBackend;
import jdk.vm.ci.runtime.JVMCICompiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Links native methods to the provided machine code using JVMCI.
 */
public class Linker {
    private static final JVMCIBackend jvmci = JVMCI.getRuntime().getHostJVMCIBackend();
    private static final ConcurrentHashMap<String, Boolean> nativeLibraries = new ConcurrentHashMap<>();
    private static final CallingConvention callingConvention = CallingConvention.getInstance();

    static {
        loadLibrary("java");
    }

    public static void loadLibrary(String name) {
        if (nativeLibraries.putIfAbsent(name, Boolean.TRUE) == null) {
            if (name.indexOf('/') >= 0 || name.indexOf('\\') > 0) {
                System.load(name);
            } else {
                System.loadLibrary(name);
            }
        }
    }

    public static long findAddress(String symbol) {
        try {
            Method m = JavaInternals.getPrivateMethod(ClassLoader.class, "findNative", ClassLoader.class, String.class);
            return (long) m.invoke(null, Linker.class.getClassLoader(), symbol);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void linkClass(Class<?> c) {
        Library library = c.getAnnotation(Library.class);
        if (library != null) {
            loadLibrary(library.value());
        }

        for (Method m : c.getDeclaredMethods()) {
            if (m.getAnnotation(Link.class) != null || m.getAnnotation(Code.class) != null) {
                linkMethod(m);
            }
        }
    }

    public static void linkMethod(Method m) {
        checkMethodType(m);

        Code code = m.getAnnotation(Code.class);
        if (code != null) {
            installCode(m, code.value());
            return;
        }

        Link link = m.getAnnotation(Link.class);
        if (link != null) {
            linkMethod(m, link.name(), link.naked());
        } else {
            linkMethod(m, m.getName(), false);
        }
    }

    public static void linkMethod(Method m, String symbol, boolean naked) {
        checkMethodType(m);

        Library library = m.getAnnotation(Library.class);
        if (library != null) {
            loadLibrary(library.value());
        }

        if (symbol.isEmpty()) {
            symbol = m.getName();
        }
        long address = findAddress(symbol);
        if (address == 0) {
            throw new IllegalArgumentException("Symbol not found: " + symbol);
        }

        ByteBuffer buf = ByteBuffer.allocate(100).order(ByteOrder.nativeOrder());
        if (!naked) {
            callingConvention.javaToNative(buf, m.getParameterTypes());
        }
        callingConvention.emitCall(buf, address);

        installCode(m, buf.array(), buf.position());
    }

    private static void checkMethodType(Method m) {
        int modifiers = m.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isNative(modifiers)) {
            throw new IllegalArgumentException("Method must be static native: " + m);
        }
    }

    public static void installCode(Method m, byte[] code) {
        installCode(m, code, code.length);
    }

    public static void installCode(Method m, byte[] code, int length) {
        ResolvedJavaMethod rm = jvmci.getMetaAccess().lookupJavaMethod(m);

        HotSpotCompiledNmethod nm = new HotSpotCompiledNmethod(
                m.getName(),
                code,
                length,
                new Site[0],
                new Assumptions.Assumption[0],
                new ResolvedJavaMethod[0],
                new HotSpotCompiledCode.Comment[0],
                new byte[0],
                1,
                new DataPatch[0],
                true,
                0,
                null,
                (HotSpotResolvedJavaMethod) rm,
                JVMCICompiler.INVOCATION_ENTRY_BCI,
                1,
                0,
                false
        );

        jvmci.getCodeCache().setDefaultCode(rm, nm);
    }
}
