package it.unich.jgmp.nativelib;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.PointerType;

public class RandomStatePointer extends PointerType {
    final private static int SIZE = MPZPointer.SIZE + 4 + Native.POINTER_SIZE;

    public RandomStatePointer() {
        setPointer(new Memory(SIZE));
    }
}
