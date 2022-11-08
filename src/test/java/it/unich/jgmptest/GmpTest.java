package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.GMP;
import it.unich.jgmp.MPF;
import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;

public class GmpTest {

    @Test
    void testMiscellaneous() {
        assertTrue(GMP.getVersion().length() > 0);
        assertTrue(GMP.getMajorVersion() >= 0);
        assertTrue(GMP.getMinorVersion() >= 0);
        assertTrue(GMP.getPatchLevel() >= 0);
        assertTrue(GMP.getBitsPerLimb() > 0);
        assertTrue(GMP.getJGmpVersion().length() >= 3);
    }

    @Test
    void testSprintf() {
        assertEquals("12 23 2/3 2" + GMP.getDecimalSeparator() + "50",
                GMP.sprintf("%Zd %d %Qd %.2Ff", new MPZ(12), Integer.valueOf(23), new MPQ(2, 3), new MPF(2.5)));
    }

    @Test
    void testSscanf() {
        String s = "12 3/2 2" + GMP.getDecimalSeparator() + "5";
        var z = new MPZ();
        var q = new MPQ();
        var f = new MPF();
        GMP.sscanf(s, "%Zd %Qd %Ff", z, q, f);
        assertEquals(new MPQ(3, 2), q);
        assertEquals(new MPZ(12), z);
        assertEquals(new MPF(2.5), f);
    }
}
