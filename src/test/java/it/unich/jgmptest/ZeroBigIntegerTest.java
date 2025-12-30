package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPZ;

public class ZeroBigIntegerTest {

    @Test
    void testZeroToBigInteger() {
        var z = new MPZ(0);
        BigInteger bi = z.getBigInteger();
        assertEquals(BigInteger.ZERO, bi);
    }
}
