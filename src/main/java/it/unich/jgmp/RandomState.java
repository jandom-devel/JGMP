package it.unich.jgmp;

import static it.unich.jgmp.nativelib.LibGMP.__gmp_randclear;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_default;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp_size;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_mt;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_set;

import com.sun.jna.NativeLong;

import it.unich.jgmp.nativelib.RandomStatePointer;

public class RandomState {

    /**
     * The pointer to the native MPZ object.
     */
    private RandomStatePointer randstatePointer;

    /**
     * Cleaner for the `RandomState` class.
     */
    private static class RandomStateCleaner implements Runnable {
        private RandomStatePointer randstatePointer;

        RandomStateCleaner(RandomStatePointer randstate) {
            this.randstatePointer = randstate;
        }

        @Override
        public void run() {
            __gmp_randclear(randstatePointer);
        }
    }

    private RandomState(RandomStatePointer randstate) {
        randstatePointer = randstate;
        GMP.cleaner.register(this, new RandomStateCleaner(randstate));
    }

    public RandomState() {
        randstatePointer = new RandomStatePointer();
        __gmp_randinit_default(randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    public RandomState(RandomState state) {
        randstatePointer = new RandomStatePointer();
        __gmp_randinit_set(randstatePointer, state.randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    public static RandomState create() {
        return new RandomState();
    }

    public static RandomState mt() {
        var m = new RandomStatePointer();
        __gmp_randinit_mt(m);
        return new RandomState(m);
    }

    public static RandomState lc(MPZ a, long c, long m2exp) {
        var m = new RandomStatePointer();
        __gmp_randinit_lc_2exp(m, a.getPointer(), new NativeLong(c), new NativeLong(m2exp));
        return new RandomState(m);
    }

    public static RandomState lc(long size) {
        var m = new RandomStatePointer();
        var res = __gmp_randinit_lc_2exp_size(m, new NativeLong(size));
        if (res == 0) {
            throw new IllegalArgumentException("Parameter size is too big");
        }
        return new RandomState(m);
    }

    public static RandomState valueOf(RandomState state) {
        return new RandomState(state);
    }

    public RandomStatePointer getPointer() {
        return randstatePointer;
    }

}
