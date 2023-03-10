/**
* Copyright 2022, 2023 Gianluca Amato <gianluca.amato@unich.it>
*                  and Francesca Scozzari <francesca.scozzari@unich.it>
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

import static it.unich.jgmp.nativelib.LibGmp.*;

import com.sun.jna.NativeLong;

import it.unich.jgmp.nativelib.GmpRandstateT;
import it.unich.jgmp.nativelib.NativeUnsignedLong;

/**
 * Current state of a random number generator. This class enapsulates the native
 * {@code gmp_randstate_t} data type, see the
 * <a href="https://gmplib.org/manual/Random-Number-Functions" target=
 * "_blank">Random Number Function</a> page of the GMP manual. In determining
 * the names and signatures of the methods of the {@code RandState} class, we
 * adopted the following rules:
 * <ul>
 * <li>the function {@code gmp_randclear} is only used internally;
 * <li>the obsolete function {@code gmp_randinit} is not exposed by the
 * {@code RandState} class;
 * <li>if the function name begins with {@code gmp_randinit}, we create a static
 * method caleed {@code baseName} which returns a new {@code RandState} object;
 * <li>otherwise, we create a method {@code baseName} which calls the original
 * function, implicitly using {@code this} as the first non-constant
 * {@code gmp_randstate_t} parameter.
 * </ul>
 * Other methods and constructors which conform to standard Java naming
 * conventions might be provided.
 * <p>
 * In general, all the parameters which are not provided implicitly to the
 * original GMP function through {@code this} should be provided explicitly.
 * <p>
 * Note that it is not safe for two threads to generate a random number from the
 * same {@code RandState} simultaneously, since this involves an update of the
 * object.
 */
public class RandState {

    /**
     * The pointer to the native {@code gmp_randstate_t} object.
     */
    private GmpRandstateT randstateNative;

    /**
     * Cleaning action for the {@code RandState} class.
     */
    private static class RandomStateCleaner implements Runnable {
        private GmpRandstateT randstateNative;

        RandomStateCleaner(GmpRandstateT randstateNative) {
            this.randstateNative = randstateNative;
        }

        @Override
        public void run() {
            gmp_randclear(randstateNative);
        }
    }

    /**
     * A private constructor which build a {@code RandState} starting from a pointer
     * to its native data object. The native object needs to be already initialized.
     */
    private RandState(GmpRandstateT pointer) {
        this.randstateNative = pointer;
        GMP.cleaner.register(this, new RandomStateCleaner(pointer));
    }

    /**
     * Returns the native pointer to the GMP object.
     */
    public GmpRandstateT getNative() {
        return randstateNative;
    }

    /**
     * Returns the default random state.
     */
    public static RandState randinitDefault() {
        var randstateNative = new GmpRandstateT();
        gmp_randinit_default(randstateNative);
        return new RandState(randstateNative);
    }

    /**
     * Returns a random state for a Mersenne Twister algorithm. This algorithm is
     * fast and has good randomness properties.
     */
    public static RandState randinitMt() {
        var m = new GmpRandstateT();
        gmp_randinit_mt(m);
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
        var m = new GmpRandstateT();
        gmp_randinit_lc_2exp(m, a.getNative(), new NativeLong(c), new NativeLong(m2exp));
        return new RandState(m);
    }

    /**
     * Returns a random state for a linear congruential algorithm. See the GMP
     * function <a href=
     * "https://gmplib.org/manual/Random-State-Initialization">{@code gmp_randinit_lc_2exp_size}</a>.
     *
     * @throws IllegalArgumentException if {@code size} is too big.
     *
     * @apiNote {@code size} should be treated as an unsigned long.
     */
    public static RandState randinitLc2ExpSize(long size) {
        var m = new GmpRandstateT();
        var res = gmp_randinit_lc_2exp_size(m, new NativeLong(size));
        if (res == 0) {
            throw new IllegalArgumentException(GMP.MSG_SIZE_TOO_BIG);
        }
        return new RandState(m);
    }

    /**
     * Returns a random state which is a copy of {@code op}.
     */
    public RandState randinitSet(RandState op) {
        var randstateNative = new GmpRandstateT();
        gmp_randinit_set(randstateNative, op.randstateNative);
        return new RandState(randstateNative);
    }

    /**
     * Sets an initial seed value into this.
     */
    public RandState randseed(MPZ seed) {
        gmp_randseed(randstateNative, seed.getNative());
        return this;
    }

    /**
     * Sets an initial seed value into this.
     *
     * @apiNote {@code seed} should be treated as an unsigned long.
     */
    public RandState randseedUi(long seed) {
        gmp_randseed_ui(randstateNative, new NativeUnsignedLong(seed));
        return this;
    }

    /**
     * Returns a uniformly distributed random number of {@code n} bits, in the range
     * {@code 0} to <code>(2<sup>n</sup>-1)</code> inclusive. {@code n} must be less
     * than or equal to the number of bits in a native unsigned long.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public long urandombUi(long n) {
        return gmp_urandomb_ui(randstateNative, new NativeUnsignedLong(n)).longValue();
    }

    /**
     * Returns a uniformly distributed random number in the range {@code 0} to
     * {@code (n - 1)} inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public long urandommUi(long n) {
        return gmp_urandomm_ui(randstateNative, new NativeUnsignedLong(n)).longValue();
    }

    // Constructors

    /**
     * Builds the default random state.
     */
    public RandState() {
        randstateNative = new GmpRandstateT();
        gmp_randinit_default(randstateNative);
        GMP.cleaner.register(this, new RandomStateCleaner(randstateNative));
    }

    /**
     * Builds a copy of the specified random state.
     */
    public RandState(RandState state) {
        randstateNative = new GmpRandstateT();
        gmp_randinit_set(randstateNative, state.randstateNative);
        GMP.cleaner.register(this, new RandomStateCleaner(randstateNative));
    }
}
