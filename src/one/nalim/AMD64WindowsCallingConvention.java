/*
 * Copyright 2022 Andrei Pangin
 *
 * Licensed under the Universal Permissive License v 1.0
 * as shown at https://opensource.org/licenses/UPL
 */

package one.nalim;

import java.nio.ByteBuffer;

class AMD64WindowsCallingConvention extends CallingConvention {

    // x64 calling convention (Windows):
    //     Java: rdx,  r8,  r9, rdi, rsi, rcx, stack
    //   Native: rcx, rdx,  r8,  r9, stack

    private static final int[] MOVE_INT_ARG = {
            0x89d1,    // mov  ecx, edx
            0x4489c2,  // mov  edx, r8d
            0x4589c8,  // mov  r8d, r9d
            0x4189f9,  // mov  r9d, edi
    };

    private static final int[] MOVE_LONG_ARG = {
            0x4889d1,  // mov  rcx, rdx
            0x4c89c2,  // mov  rdx, r8
            0x4d89c8,  // mov  r8, r9
            0x4989f9,  // mov  r9, rdi
    };

    private static final int[] MOVE_ARRAY_ARG = {
            0x488d4a,  // lea  rcx, [rdx+N]
            0x498d50,  // lea  rdx, [r8+N]
            0x4d8d41,  // lea  r8, [r9+N]
            0x4c8d4f,  // lea  r9, [rdi+N]
    };

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>... types) {
        int index = 0;
        for (Class<?> type : types) {
            index += moveArg(buf, index, type);
        }
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        buf.putShort((short) 0xb848).putLong(address);  // mov rax, address
        buf.putShort((short) 0xe0ff);                   // jmp rax
    }

    private static int moveArg(ByteBuffer buf, int index, Class<?> type) {
        if (type == float.class || type == double.class) {
            return 0;
        } else if (index >= 4 && (type.isPrimitive() || type.isArray())) {
            throw new IllegalArgumentException("At most 4 integer arguments are supported");
        } else if (type.isPrimitive()) {
            emit(buf, (type == long.class ? MOVE_LONG_ARG : MOVE_INT_ARG)[index]);
            return 1;
        } else if (type.isArray()) {
            emit(buf, MOVE_ARRAY_ARG[index]);
            buf.put((byte) arrayBaseOffset(type));
            return 1;
        }
        throw new IllegalArgumentException("Unsupported argument type: " + type);
    }
}
