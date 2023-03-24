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

import java.util.concurrent.atomic.AtomicLong;

import com.sun.jna.Pointer;

import it.unich.jgmp.nativelib.AllocFunc;
import it.unich.jgmp.nativelib.AllocFuncByReference;
import it.unich.jgmp.nativelib.FreeFunc;
import it.unich.jgmp.nativelib.FreeFuncByReference;
import it.unich.jgmp.nativelib.LibGmp;
import it.unich.jgmp.nativelib.ReallocFunc;
import it.unich.jgmp.nativelib.ReallocFuncByReference;
import it.unich.jgmp.nativelib.SizeT;

/**
 * The allocation monitor keeps track of the amount of native memory allocated
 * by GMP, and calls the Java garbage collector when it finds that too much
 * native memory is being used. The hope is that, by destroying JGMP objects,
 * the pressure on native memory is reduced.
 * <p>
 * In order to keep track of allocated native memory, this class uses the GMP
 * fuctions {@code mp_set_memory_functions} and {@code mp_get_memory_functions}
 * (see the
 * <a href="https://gmplib.org/manual/Custom-Allocation" target= "_blank">Custom
 * Allocation</a> page of the GMP manual). Since this slows down allocation, the
 * feature is normally disabled and may be enable by calling the
 * {@code enable()} static method.
 * <p>
 * It is important to enable allocation monitor when a program builds many big
 * JGMP objects. In this case, since the size occupied by a JGMP object in the
 * Java heap is only a fraction of the size occupied in native memory, the
 * program may consume all the native memory without the JVM feeling the need to
 * call the garbage collector to reclaim heap space. This may happen, in
 * particular, when making use of the immutable API.
 * <p>
 * The current allocator has three tunables: {@code allocationThreshold},
 * {@code lowerThreshold} and {@code maxTimeout}. Every allocation or
 * reallocation of native memory by the GMP library makes the allocated size
 * larger than the allocation threshold, causes a call to the Java garabage
 * collector. Then, we wait until the allocated memory falls below the lower
 * threshold, or until a timeout has expired. The length of the timeout is
 * dinamically computed by the allocation monitor, but it never exceed the value
 * of the {@code maxTimeout} tunable.
 */
public class AllocationMonitor {
    /**
     * A private constructor, since this class should never be instantiated.
     */
    private AllocationMonitor() {
    }

    /**
     * The debug level of the allocation monitor. We do not think it is important to
     * declare this variable as volatile, since it is only used for debugging
     * purposes.
     */
    private static int debugLevel = 0;

    /**
     * Set the debug level of the allocation monitor. The greater the value, the
     * more debug messages are sent to the standard error. Zero and negative numbers
     * mean that no debug messages are generated.
     */
    public static void setDebugLevel(int debugLevel) {
        AllocationMonitor.debugLevel = debugLevel;
    }

    /**
     * Return the current debug level of the allocation monitor.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * The amount of native memory allocated by GMP, as recorded by the allocation
     * monitor. It is an `AtomicLong` since it might be increased concurrently by
     * multple threads.
     */
    private static AtomicLong allocatedSize = new AtomicLong();

    /**
     * Return the amount of native memory allocated by JGMP, as recorded by the
     * allocation monitor.
     */
    public static long getAllocatedSize() {
        return allocatedSize.get();
    }

    /**
     * The allocation threshold.
     */
    private static volatile long allocationThreshold = Runtime.getRuntime().maxMemory();

    /**
     * Return the current allocation threshold.
     */
    public static long getAllocationThreshold() {
        return allocationThreshold;
    }

    /**
     * Set the current allocation threshold. This method also sets the default value
     * for the lower threshold, which is 15/16 of the allocation threshold.
     */
    public static void setAllocationThreshold(long value) {
        allocationThreshold = value;
        lowerThreshold = value / 16 * 15;
    }

    /**
     * The lower threshold.
     */
    private static volatile long lowerThreshold = allocationThreshold / 16 * 15;

    /**
     * Return the current value of the lower threshold.
     */
    public static long getLowerThreshold() {
        return lowerThreshold;
    }

    /**
     * Set the current value of the lower threshold.
     */
    public static void setLowerThreshold(long value) {
        lowerThreshold = value;
    }

    /**
     * Number of steps in which we divide the timeout interval.
     */
    private static final int TIMEOUT_STEPS = 10;

    /**
     * The maximum delay for a single timeout step.
     */
    private static int maxStepTimeout = 200 / TIMEOUT_STEPS;

    /**
     * The current delay for a single timeout step.
     */
    private static int stepTimeout = maxStepTimeout;

    /**
     * Set the maximum timeout value. The default value is 200 ms.
     */
    public static void setTimeout(int value) {
        maxStepTimeout = value / TIMEOUT_STEPS;
        stepTimeout = value / TIMEOUT_STEPS;
    }

    /**
     * Return the current timeout value.
     */
    public static int getTimeout() {
        return stepTimeout * TIMEOUT_STEPS;
    }

    /**
     * Keep track of the number of times that GC has been called by the allocation
     * monitor.
     */
    private static volatile int gcCalls = 0;

    /**
     * Return the number of times that GC has been called by the allocation monitor.
     */
    public static int getGcCalls() {
        return gcCalls;
    }

    /**
     * The maximum amount of memory allocated by GMP.
     */
    private static long maxAllocatedSize = 0;

    /**
     * Return the maximum amount of memory ever allocated by the JGMP at the same
     * moment, as recorded by the allocation monitor.
     */
    public static long getMaxAllocatedSize() {
        return maxAllocatedSize;
    }

    // Keep reference to the custom allocators, in order to avoid them being garbage collected.
    private static AllocFunc af;
    private static ReallocFunc rf;
    private static FreeFunc ff;

    // References to the original allocators.
    private static AllocFuncByReference afpOld;
    private static ReallocFuncByReference rfpOld;
    private static FreeFuncByReference ffpOld;

    /**
     * Print debugging information.
     */
    private static void debugInfo() {
        System.err.print(" -- currenty allocated: " + String.format("%,d", getAllocatedSize()) + " bytes, ");
        System.err.print("maximum allocated: " + String.format("%,d", getMaxAllocatedSize()) + " bytes, ");
        System.err.println("GC called: " + getGcCalls() + " times, Timeout: " + getTimeout() + " ms");
    }

    /**
     * Check if the garbage collector needs to be invoked, and update the numCrossed
     * and allocation thresholds.
     */
    static void checkGC(long newSize) {
        maxAllocatedSize = Math.max(maxAllocatedSize, newSize);
        if (newSize > allocationThreshold) {
            gcCalls += 1;
            System.gc();
            int count = 0;
            do {
                try {
                    Thread.sleep(stepTimeout);
                } catch (InterruptedException e) {
                }
                ;
                count += 1;
            } while (allocatedSize.get() > lowerThreshold && count < 10);
            stepTimeout = Math.max(1, Math.min(maxStepTimeout, (count - 1) * stepTimeout + stepTimeout / 2));
            if (debugLevel >= 1) {
                System.err.print("checkGC with count " + count);
                debugInfo();
            }
        }
    }

    /**
     * The custom allocator function.
     */
    private static class JGMPAlloc implements AllocFunc {

        AllocFuncByReference afp;

        JGMPAlloc(AllocFuncByReference afp) {
            this.afp = afp;
        }

        @Override
        public Pointer invoke(SizeT alloc_size) {
            if (debugLevel >= 2) {
                System.err.print("Allocate " + String.format("%,d", alloc_size.longValue()) + " bytes");
                debugInfo();
            }
            checkGC(allocatedSize.addAndGet(alloc_size.longValue()));
            return afp.value.invoke(alloc_size);
        }
    }

    /**
     * The custom reallocator.
     */
    private static class JGMPRealloc implements ReallocFunc {
        ReallocFuncByReference rfp;

        JGMPRealloc(ReallocFuncByReference rfp) {
            this.rfp = rfp;
        }

        @Override
        public Pointer invoke(Pointer ptr, SizeT old_size, SizeT new_size) {
            if (debugLevel >= 2) {
                System.err.print("Reallocate " + String.format("%,d", old_size.longValue()) + " bytes to "
                        + String.format("%,d", new_size.longValue()) + " bytes");
                debugInfo();
            }
            long increase = new_size.longValue() - old_size.longValue();
            long increased = allocatedSize.addAndGet(increase);
            if (increase > 0)
                checkGC(increased);
            return rfp.value.invoke(ptr, old_size, new_size);
        }
    }

    /**
     * The custom deallocator.
     */
    private static class JGMPFree implements FreeFunc {
        FreeFuncByReference ffp;

        JGMPFree(FreeFuncByReference ffp) {
            this.ffp = ffp;
        }

        @Override
        public void invoke(Pointer ptr, SizeT alloc_size) {
            if (debugLevel >= 2) {
                System.err.println("Free " + String.format("%,d", alloc_size.longValue()) + " bytes");
                debugInfo();
            }
            allocatedSize.addAndGet(-alloc_size.longValue());
            ffp.value.invoke(ptr, alloc_size);
        }
    }

    /**
     * Enable the allocation monitor. Nothing happens if the monitor is already
     * enabled.
     */
    public static synchronized void enable() {
        if (afpOld == null) {
            afpOld = new AllocFuncByReference();
            rfpOld = new ReallocFuncByReference();
            ffpOld = new FreeFuncByReference();
            LibGmp.mp_get_memory_functions(afpOld, rfpOld, ffpOld);
            af = new JGMPAlloc(afpOld);
            rf = new JGMPRealloc(rfpOld);
            ff = new JGMPFree(ffpOld);
            LibGmp.mp_set_memory_functions(af, rf, ff);
        }
    }

    /**
     * Disable the allocation monitor. Nothing happens if the monitor is already
     * disabled.
     */
    public static synchronized void disable() {
        if (afpOld != null) {
            LibGmp.mp_set_memory_functions(afpOld.value, rfpOld.value, ffpOld.value);
            af = null;
            rf = null;
            ff = null;
            afpOld = null;
            rfpOld = null;
            ffpOld = null;
        }
    }
}
