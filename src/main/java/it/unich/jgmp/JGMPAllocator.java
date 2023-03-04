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
 * Custom allocator functions which keeps track of the amount of memory
 * allocated by GMP.
 */
public class JGMPAllocator {
    /**
     * A private constructor since this class should never be instantiated.
     */
    private JGMPAllocator() {
    }

    /**
     * The debug level of the JGMP allocator. We do not think it is important to
     * declare it to be volatile, since it is only used for debugging purposes.
     */
    private static int debugLevel = 0;

    /**
     * Set the debug level of the JGMP allocator.
     */
    public static void setDebugLevel(int debugLevel) {
        JGMPAllocator.debugLevel = debugLevel;
    }

    /**
     * Return the current debug level of the JGMP allocator.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * The amount of memory allocated by the JGMP allocator. It is an `AtomicLong`
     * since it might be increased concurretly by multple threads.
     */
    private static AtomicLong allocatedSize = new AtomicLong();

    /**
     * Returns the amount of memory allocated by the JGMP allocator.
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
        JGMPAllocator.maxAllocationThreshold = maxAllocationThreshold;
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
        JGMPAllocator.allocationThreshold = Math.min(allocationThreshold, maxAllocationThreshold);
    }

    /**
     * Return the current allocation threshold.
     */
    public static long getAllocationThreshold() {
        return allocationThreshold;
    }

    // Keep reference to custom allocators to avoid them being garbage collected.
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
        JGMPAllocator.callThreshold = callThreshold;
    }

    /**
     * Returns the current value of the call threshold.
     */
    public static int getCallThreshold() {
        return callThreshold;
    }

    /**
     * Objecy used only for synchronizing the checkGC method.
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
                    System.out.println("Calling GC: called " + called + " times with threshold "
                            + String.format("%,d", allocationThreshold) + "bytes");
                if (called >= callThreshold) {
                    allocationThreshold = Math.min(2 * allocationThreshold, maxAllocationThreshold);
                    callThreshold = Math.min(2 * callThreshold, Short.MAX_VALUE);
                }
            }
            if (! gcCalled) {
                gcCalled = true;
                System.gc();
            }
        } else
            gcCalled = false;
    }

    /** The custom allocator. */
    private static class JGMPAlloc implements AllocFunc {

        AllocFuncByReference afp;

        JGMPAlloc(AllocFuncByReference afp) {
            this.afp = afp;
        }

        @Override
        public Pointer invoke(SizeT alloc_size) {
            if (debugLevel >= 2) {
                System.out.println("Allocate " + alloc_size.longValue() + "  bytes starting from "
                        + String.format("%,d", allocatedSize.get()) + " bytes already allocated");
                System.out.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
            }
            checkGC(allocatedSize.addAndGet(alloc_size.longValue()));
            return afp.value.invoke(alloc_size);
        }
    }

    /** The custom reallocator. */
    private static class JGMPRealloc implements ReallocFunc {
        ReallocFuncByReference rfp;

        JGMPRealloc(ReallocFuncByReference rfp) {
            this.rfp = rfp;
        }

        @Override
        public Pointer invoke(Pointer ptr, SizeT old_size, SizeT new_size) {
            if (debugLevel >= 2) {
                System.out.println("Reallocate " + old_size.longValue() + "  bytes to " + new_size.longValue()
                        + " bytes starting from " + String.format("%,d", allocatedSize.get())
                        + " bytes already allocated");
                System.out.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
            }
            long increase = new_size.longValue() - old_size.longValue();
            long increased = allocatedSize.addAndGet(increase);
            if (increase > 0)
                checkGC(increased);
            return rfp.value.invoke(ptr, old_size, new_size);
        }
    }

    /** The custom deallocator. */
    private static class JGMPFree implements FreeFunc {
        FreeFuncByReference ffp;

        JGMPFree(FreeFuncByReference ffp) {
            this.ffp = ffp;
        }

        @Override
        public void invoke(Pointer ptr, SizeT alloc_size) {
            if (debugLevel >= 2) {
                System.out.println("Free " + called + " " + alloc_size.longValue() + " bytes starting from "
                        + String.format("%,d", allocatedSize.get()) + " bytes already allocated");
                System.out.println("GC called " + called + " times with threshold "
                        + String.format("%,d", allocationThreshold) + " bytes");
            }
            allocatedSize.addAndGet(-alloc_size.longValue());
            ffp.value.invoke(ptr, alloc_size);
        }
    }

    /**
     * Enable the custom allocator.
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
     * Disable the custom allocator.
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
