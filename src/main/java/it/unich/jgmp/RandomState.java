package it.unich.jgmp;

import static it.unich.jgmp.nativelib.LibGMP.__gmp_randclear;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_default;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp_size;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_mt;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_set;

import com.sun.jna.NativeLong;

import it.unich.jgmp.nativelib.RandStatePointer;

/**
 * The class encapsulating the {@code gmp_randstate_t} data type, which holds
 * the current state of a random number generator.
 */
public class RandomState {

    /**
     * The pointer to the native {@code gmp_randstate_t}  object.
     */
    private RandStatePointer randstatePointer;

    /**
     * Cleaner for the {@codde RandomState} class.
     */
    private static class RandomStateCleaner implements Runnable {
        private RandStatePointer randstatePointer;

        RandomStateCleaner(RandStatePointer randstate) {
            this.randstatePointer = randstate;
        }

        @Override
        public void run() {
            __gmp_randclear(randstatePointer);
        }
    }

    private RandomState(RandStatePointer randstate) {
        randstatePointer = randstate;
        GMP.cleaner.register(this, new RandomStateCleaner(randstate));
    }

    public RandomState() {
        randstatePointer = new RandStatePointer();
        __gmp_randinit_default(randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    public RandomState(RandomState state) {
        randstatePointer = new RandStatePointer();
        __gmp_randinit_set(randstatePointer, state.randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    public static RandomState create() {
        return new RandomState();
    }

    public static RandomState mt() {
        var m = new RandStatePointer();
        __gmp_randinit_mt(m);
        return new RandomState(m);
    }

    public static RandomState lc(MPZ a, long c, long m2exp) {
        var m = new RandStatePointer();
        __gmp_randinit_lc_2exp(m, a.getPointer(), new NativeLong(c), new NativeLong(m2exp));
        return new RandomState(m);
    }

    public static RandomState lc(long size) {
        var m = new RandStatePointer();
        var res = __gmp_randinit_lc_2exp_size(m, new NativeLong(size));
        if (res == 0) {
            throw new IllegalArgumentException("Parameter size is too big");
        }
        return new RandomState(m);
    }

    public static RandomState valueOf(RandomState state) {
        return new RandomState(state);
    }

    public RandStatePointer getPointer() {
        return randstatePointer;
    }

}
