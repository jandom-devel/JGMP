package it.unich.jgmptest.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import it.unich.jgmp.AllocationMonitor;
import it.unich.jgmp.MPZ;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 5, jvmArgs = { "-Xms2G", "-Xmx2G" })
@Warmup(iterations = 5)
@Measurement(iterations = 5)

public class AllocationMonitorBench {

    @Param({ "1000", "10000", "100000" })
    public int fact;

    @Param({ "16", "4", "1" })
    public int allocationThresholdDivisor;

    @Param({ "false", "true" })
    public boolean monitorEnabled;

    @Setup(Level.Trial)
    public void AllocationMonitorSetup() {
        AllocationMonitor.setAllocationThreshold(Runtime.getRuntime().maxMemory() / allocationThresholdDivisor);
        AllocationMonitor.setDebugLevel(0);
        if (monitorEnabled)
            AllocationMonitor.enable();
    }

    @TearDown(Level.Trial)
    public void TearDown() {
        System.err.println();
        System.err.println("Allocation Threshold: " + String.format("%,d", AllocationMonitor.getAllocationThreshold()));
        System.err.println("Allocated Memory: " + String.format("%,d", AllocationMonitor.getAllocatedSize()));
        System.err.println("Max Allocated Memory: " + String.format("%,d", AllocationMonitor.getMaxAllocatedSize()));
        System.err.println("Timeout: " + AllocationMonitor.getTimeout());
        System.err.println("Number of times GC called: " + AllocationMonitor.getGcCalls());
    }

    @Benchmark
    public MPZ factorialMPZfast() {
        return MPZ.facUi(fact);
    }

    @Benchmark
    public MPZ factorialMPZ() {
        var x = fact;
        var f = new MPZ(1);
        while (x >= 1) {
            f.mulAssign(f, x);
            x -= 1;
        }
        return f;
    }

    @Benchmark
    public MPZ factorialMPZImmutable() {
        var x = fact;
        var f = new MPZ(1);
        while (x >= 1) {
            f = f.mul(x);
            x -= 1;
        }
        return f;
    }

    @Threads(4)
    @Benchmark
    public MPZ factorialMPZImmutableMT() {
        var x = fact;
        var f = new MPZ(1);
        while (x >= 1) {
            f = f.mul(x);
            x -= 1;
        }
        return f;
    }

}
