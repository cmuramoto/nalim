/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.runtime.JVMCI;

import java.nio.ByteBuffer;

abstract class CallingConvention {

    static CallingConvention getInstance() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (!arch.contains("64")) {
            throw new IllegalStateException("Unsupported architecture: " + arch);
        }

        if (arch.contains("aarch") || arch.contains("arm")) {
            return new AArch64CallingConvention();
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return new AMD64WindowsCallingConvention();
        } else {
            return new AMD64LinuxCallingConvention();
        }
    }

    abstract void javaToNative(ByteBuffer buf, Class<?>... types);

    abstract void emitCall(ByteBuffer buf, long address);

    protected static int arrayBaseOffset(Class<?> arrayType) {
        JavaKind elementKind = JavaKind.fromJavaClass(arrayType.getComponentType());
        return JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess().getArrayBaseOffset(elementKind);
    }

    protected static void emit(ByteBuffer buf, int code) {
        if ((code >>> 24) != 0) buf.put((byte) (code >>> 24));
        if ((code >>> 16) != 0) buf.put((byte) (code >>> 16));
        if ((code >>> 8) != 0) buf.put((byte) (code >>> 8));
        if (code != 0) buf.put((byte) code);
    }
}
