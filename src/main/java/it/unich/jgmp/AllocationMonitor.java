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
 *
 * In order to keep track of allocated native memory, this class uses the GMP
 * fuctions {@code mp_set_memory_functions} and {@code mp_get_memory_functions}
 * ( see the *
 * <a href="https://gmplib.org/manual/Floating_002dpoint-Functions" target=
 * "_blank">Custom Allocation</a> page of the GMP manual). Since this slow downs
 * allocation, the feature is normally disabled and may be enable by calling the
 * {@code enable()} static method.
 *
 * It is important to enable allocation monitor when the a program builds many
 * big JGMP objects. In this case, since the size occupied by a JGMP object in
 * the Java heap is only a fraction of the size occupied in native memory, the
 * program may consume all the native memory without the JVM feeling the need to
 * call the garbage collector to reclaim heap space. This may happen, in
 * particular, when making use of the immutable API.
 */
public class AllocationMonitor {
    /**
     * A private constructor, since this class should never be instantiated.
     */
    private AllocationMonitor() {
    }

    /**
     * The debug level of the allocation monitor. We do not think it is important to
     * declare this variabi as volatile, since it is only used for debugging
     * purposes.
     */
    private static int debugLevel = 0;

    /**
     * Set the debug level of the allocation monitor. The greater the value, the
     * more debug messages are sent to the standard error. Zero and negative numbers
     * mean that no debug messages is generated.
     */
    public static void setDebugLevel(int debugLevel) {
        AllocationMonitor.debugLevel = debugLevel;
    }

    /**
     * Return the current debug level of the allocation monitor.
     *
     * @see setDebugLevel
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * The amount of native memory allocate by GMP, as computed by the allocation
     * monitor. It is an `AtomicLong` since it might be increased concurretly by
     * multple threads.
     */
    private static AtomicLong allocatedSize = new AtomicLong();

    /**
     * Returns the amount of native memory allocated by JGMP, as computed by the
     * allocation monitor
     */
    public static long getAllocatedSize() {
        return allocatedSize.get();
    }

    /**
     * The threshold of allocated memory. Any allocation or reallocation of native
     * memory by GMP which causes `allocatedSize` to become larger than this value
     * causes a call to the JVM Garabage Collector. The default value is the maximum
     * Java heap size.
     */
    private static volatile long allocationThreshold = Runtime.getRuntime().maxMemory() / 16;

    /**
     * The maximum value that allocationThreshold may assume.
     */
    private static volatile long maxAllocationThreshold = Runtime.getRuntime().maxMemory();

    /**
     * Set the maximum allocation threadshold. The default value is equal to the
     * maximum size of the heap returned by `Runtime.getRuntime().maxMemory()`.
     */
    public static synchronized void setMaxAllocationThreshold(long maxAllocationThreshold) {
        AllocationMonitor.maxAllocationThreshold = maxAllocationThreshold;
        allocationThreshold = Math.min(allocationThreshold, maxAllocationThreshold);
    }

    /**
     * Return the current value of the maximum allocation threshold.
     */
    public static long getMaxAllocationThreshold() {
        return maxAllocationThreshold;
    }

    /**
     * Set the current allocation threadshold. The initial value is equal to 1/16th
     * of the maximum size of the heap returned by
     * `Runtime.getRuntime().maxMemory()`.
     */
    public static void setAllocationThreshold(long allocationThreshold) {
        AllocationMonitor.allocationThreshold = Math.min(allocationThreshold, maxAllocationThreshold);
    }

    /**
     * Return the current allocation threshold.
     */
    public static long getAllocationThreshold() {
        return allocationThreshold;
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
     * Count number of times the garbage collector has been called from within the
     * custom allocator.
     */
    private static volatile int called = 0;

    /**
     * Count number of times the garbage collector has been called from within the
     * custom allocator.
     */
    public static int getCalled() {
        return called;
    }

    private static volatile int callThreshold = 1;

    /**
     * Set the current call threshold.
     */
    public static void setCallThreshold(int callThreshold) {
        AllocationMonitor.callThreshold = callThreshold;
    }

    /**
     * Returns the current value of the call threshold.
     */
    public static int getCallThreshold() {
        return callThreshold;
    }

    /**
     * Object used solely for synchronizing the checkGC method.
     */
    private static Object gcMonitor = new Object();

    /**
     * Is true if System.gc() has been already called, but at the moment has not
     * obtained any success.
     */
    private static boolean gcCalled = false;

    /**
     * Check if the garbage collector needs to be invoke and update the call and
     * allocation thresholds.
     */
    static void checkGC(long newSize) {
        if (newSize >= allocationThreshold) {
            synchronized (gcMonitor) {
                called += 1;
                if (debugLevel >= 1)
                    System.err.println("Calling GC: called " + called + " times with threshold "
                            + String.format("%,d", allocationThreshold) + "bytes");
                if (called >= callThreshold) {
                    allocationThreshold = Math.min(2 * allocationThreshold, maxAllocationThreshold);
                    callThreshold = Math.min(2 * callThreshold, Short.MAX_VALUE);
                }
            }
            if (!gcCalled) {
                gcCalled = true;
                System.gc();
            }
        } else
            gcCalled = false;
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
                System.err.println("Allocate " + alloc_size.longValue() + "  bytes starting from "
                        + String.format("%,d", allocatedSize.get()) + " bytes already allocated");
                System.err.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
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
                System.err.println("Reallocate " + old_size.longValue() + "  bytes to " + new_size.longValue()
                        + " bytes starting from " + String.format("%,d", allocatedSize.get())
                        + " bytes already allocated");
                System.err.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
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
                System.err.println("Free " + called + " " + alloc_size.longValue() + " bytes starting from "
                        + String.format("%,d", allocatedSize.get()) + " bytes already allocated");
                System.err.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
            }
            allocatedSize.addAndGet(-alloc_size.longValue());
            ffp.value.invoke(ptr, alloc_size);
        }
    }

    /**
     * Enable the allocation monitor. Nothing happens if the monitor is already
     * enable.
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
