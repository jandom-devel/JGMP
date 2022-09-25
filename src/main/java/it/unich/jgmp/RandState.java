/**
* Copyright 2022 Gianluca Amato <gianluca.amato@unich.it>
*
* This file is part of JGMP. JGMP is free software: you can
* redistribute it and/or modify it under the terms of the GNU General Public
* License as published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* JGMP is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of a MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*
* You should have received a copy of the GNU General Public License along with
* JGMP. If not, see <http://www.gnu.org/licenses/>.
*/

package it.unich.jgmp;

import static it.unich.jgmp.nativelib.LibGMP.__gmp_randclear;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_default;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_lc_2exp_size;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_mt;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randinit_set;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randseed;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_randseed_ui;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_urandomb_ui;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_urandomm_ui;

import com.sun.jna.NativeLong;

import it.unich.jgmp.nativelib.NativeUnsignedLong;
import it.unich.jgmp.nativelib.RandStatePointer;

/**
 * The class encapsulating the {@code gmp_randstate_t} data type, which holds
 * the current state of a random number generator.
 *
 * <p>
 * An element of {@code RandState} contains a pointer to a native
 * {@code gmp_randstate_t} variable and registers itself with
 * {@link GMP#cleaner} for freeing all allocated memory during garbage
 * collection.
 * <p>
 * In determining the names and prototypes of the methods of the {@code MPZ}
 * class, we adopted the following rules:
 * <ul>
 * <li>functions {@code gmp_randclear} and {@code gmp_randinit} are not exposed
 * by the {@code RandState} class;
 * <li>if {@code baseName} begins with {@code randinit}, we create a static
 * method {@code baseName} which returns a new {@code RandState} object;
 * <li>otherwise, we create a method {@code baseName} which calls the original
 * function, implicitly using {@code this} as the first non-constant
 * {@code gmp_randstate_t} parameter;
 * </ul>
 * <p>
 * In general, all the parameters which are not provided implicitly to the
 * original GMP function through {@code this} should be provided explicitly by
 * having them in the method prototype.
 */
public class RandState {

    /**
     * The pointer to the native {@code gmp_randstate_t} object.
     */
    private RandStatePointer randstatePointer;

    /**
     * Cleaning action for the {@code RandState} class.
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

    /**
     * A private constructor which build a {@code RandState} starting from a pointer
     * to its native data object. The native object needs to be already initialized.
     */
    private RandState(RandStatePointer pointer) {
        this.randstatePointer = pointer;
        GMP.cleaner.register(this, new RandomStateCleaner(pointer));
    }

    /**
     * Returns the native pointer to the GMP object.
     */
    public RandStatePointer getPointer() {
        return randstatePointer;
    }

    /**
     * Builds the default random state.
     */
    public RandState() {
        randstatePointer = new RandStatePointer();
        __gmp_randinit_default(randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    /**
     * Builds a copy of the specified random state.
     */
    public RandState(RandState state) {
        randstatePointer = new RandStatePointer();
        __gmp_randinit_set(randstatePointer, state.randstatePointer);
        GMP.cleaner.register(this, new RandomStateCleaner(randstatePointer));
    }

    /**
     * Returns the default random state.
     */
    public static RandState randinitDefault() {
        return new RandState();
    }

    /**
     * Returns a random state for a Mersenne Twister algorithm. This algorithm is
     * fast and has good randomness properties.
     */
    public static RandState randinitMt() {
        var m = new RandStatePointer();
        __gmp_randinit_mt(m);
        return new RandState(m);
    }

    /**
     * Returns a random state for a linear congruential algorithm. See the GMP
     * function <a href=
     * "https://gmplib.org/manual/Random-State-Initialization">{@code gmp_randinit_lc_2exp}</a>.
     *
     * @apiNote both {@code c} and {@code m2exp} should be treated as unsigned
     *          longs.
     */
    public static RandState randinitLc2Exp(MPZ a, long c, long m2exp) {
        var m = new RandStatePointer();
        __gmp_randinit_lc_2exp(m, a.getPointer(), new NativeLong(c), new NativeLong(m2exp));
        return new RandState(m);
    }

    /**
     * Returns a random state for a linear congruential algorithm. See the GMP
     * function <a href=
     * "https://gmplib.org/manual/Random-State-Initialization">{@code gmp_randinit_lc_2exp_size}</a>.
     *
     * @apiNote both {@code size} should be treated as an unsigned long.
     */
    public static RandState randinitLc2ExpSize(long size) {
        var m = new RandStatePointer();
        var res = __gmp_randinit_lc_2exp_size(m, new NativeLong(size));
        if (res == 0) {
            throw new IllegalArgumentException("Parameter size is too big");
        }
        return new RandState(m);
    }

    /**
     * Returns a random state which is a copy of {@code op}.
     */
    public RandState randinitSet(RandState op) {
        return new RandState(op);
    }

    /**
     * Sets an initial seed value into state.
     */
    public RandState randseed(MPZ seed) {
        __gmp_randseed(randstatePointer, seed.getPointer());
        return this;
    }

    /**
     * Sets an initial seed value into state.
     *
     * @apiNote {@code seed} should be treated as an unsigned long.
     */
    public RandState randseedUi(long seed) {
        __gmp_randseed_ui(randstatePointer, new NativeUnsignedLong(seed));
        return this;
    }

    /**
     * Return a uniformly distributed random number of {@code n} bits, i.e. in the
     * range {@code 0} to <code>(2<sup>n</sup>-1)</code> inclusive. {@code n} must
     * be less than or equal to the number of bits in a native unsigned long.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public long urandombUi(long n) {
        return __gmp_urandomb_ui(randstatePointer, new NativeUnsignedLong(n)).longValue();
    }

    /**
     * Return a uniformly distributed random number in the range {@code 0} to
     * {@code (n - 1)} inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public long urandommUi(long n) {
        return __gmp_urandomm_ui(randstatePointer, new NativeUnsignedLong(n)).longValue();
    }
}
