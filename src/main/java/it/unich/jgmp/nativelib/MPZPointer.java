package it.unich.jgmp.nativelib;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.PointerType;

public class MPZPointer extends PointerType {
    final public static int SIZE = 4 + 4 + Native.POINTER_SIZE;

    public MPZPointer() {
        setPointer(new Memory(SIZE));
    }
}
