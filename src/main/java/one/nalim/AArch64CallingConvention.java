/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import java.nio.ByteBuffer;

class AArch64CallingConvention extends CallingConvention {

    // AArch64 calling convention:
    //     Java: x1, x2, x3, x4, x5, x6, x7, x0, stack
    //   Native: x0, x1, x2, x3, x4, x5, x6, x7, stack

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>... types) {
        if (types.length >= 8) {
            // 8th Java argument clashes with the 1st native arg
            buf.putInt(0xaa0003e8);  // mov x8, x0
        }

        int index = 0;
        for (Class<?> type : types) {
            index += moveArg(buf, index, type);
        }
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        int a0 = (int) address & 0xffff;
        int a1 = (int) (address >>> 16) & 0xffff;
        int a2 = (int) (address >>> 32) & 0xffff;
        int a3 = (int) (address >>> 48);

        buf.putInt(0xd2800009 | a0 << 5);               // movz x9, #0xffff
        if (a1 != 0) buf.putInt(0xf2a00009 | a1 << 5);  // movk x9, #0xffff, lsl #16
        if (a2 != 0) buf.putInt(0xf2c00009 | a2 << 5);  // movk x9, #0xffff, lsl #32
        if (a3 != 0) buf.putInt(0xf2e00009 | a3 << 5);  // movk x9, #0xffff, lsl #48

        buf.putInt(0xd61f0120);                         // br x9
    }

    private static int moveArg(ByteBuffer buf, int index, Class<?> type) {
        if (type == float.class || type == double.class) {
            return 0;
        } else if (index >= 8 && (type.isPrimitive() || type.isArray())) {
            return 0;
        } else if (type.isPrimitive()) {
            // mov x0, x1
            buf.putInt((type == long.class ? 0xaa0003e0 : 0x2a0003e0) | index | (index + 1) << 16);
            return 1;
        } else if (type.isArray()) {
            // add x0, x1, #offset
            buf.putInt(0x91000000 | index | (index + 1) << 5 | arrayBaseOffset(type) << 10);
            return 1;
        }
        throw new IllegalArgumentException("Unsupported argument type: " + type);
    }
}
