package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;

public class MpqTest {

    @Test
    void testInitAssignments() {
        var a = MPQ.init();
        assertEquals(new MPQ(0, 1), a);
        assertSame(a, a.set(new MPQ(15)));
        assertEquals(new MPQ(15, 1), a);
        assertSame(a, a.set(new MPZ(15)));
        assertEquals(new MPQ(15, 1), a);
        assertSame(a, a.set(2, 3));
        assertEquals(new MPQ(2, 3), a);
        assertSame(a, a.setUi(2, 3));
        assertEquals(new MPQ(2, 3), a);
        assertEquals(0, a.set("-1A", 16));
        assertEquals(new MPQ(-26, 1), a);
        assertEquals(-1, a.set("2", 63));
        assertEquals(new MPQ(-26, 1), a);
        assertEquals(0, a.set("-1A/7", 16));
        assertEquals(new MPQ(-26, 7), a);

        var b = new MPQ(-26);
        var c = new MPQ(2).swap(b);
        assertEquals(new MPQ(-26), c);
        assertEquals(new MPQ(2), b);
    }

    @Test
    void testConversions() {
        var a = new MPQ();
        assertEquals(-5.25, new MPQ(-21, 4).getD());
        assertSame(a, a.set(5.25));
        assertEquals(new MPQ(21, 4), a);
        assertThrows(ArithmeticException.class, () -> a.set(Double.POSITIVE_INFINITY));
        assertEquals("21/4", new MPQ(21, 4).getStr(10));
        assertEquals("-21/4", new MPQ(-21, 4).getStr(10));
        assertEquals(null, new MPQ(21, 4).getStr(63));
    }

    void testConstructors() {
        assertEquals(new MPQ(0, 1), new MPQ());
        assertEquals(new MPQ(15, 1), new MPQ(new MPQ(15)));
        assertEquals(new MPQ(15, 1), new MPQ(new MPZ(15)));
        assertEquals(new MPQ(21, 4), new MPQ(5.25));
        assertEquals(new MPQ(-26, 1), new MPQ("-1A", 16));
        assertEquals(new MPQ(-26, 7), new MPQ("-1A/7", 16));
    }

    @Test
    void testArithmetic1() {
        assertEquals(new MPQ(15), new MPQ(8).add(new MPQ(7)));
        assertEquals(new MPQ(1), new MPQ(8).sub(new MPQ(7)));
        assertEquals(new MPQ(56), new MPQ(8).mul(new MPQ(7)));
        assertEquals(new MPQ(48), new MPQ(3).mul2Exp(4));
        assertEquals(new MPQ(2, 3), new MPQ(4).div(new MPQ(6)));
        assertThrows(ArithmeticException.class, () -> new MPQ(4).div(new MPQ(0)));
        assertEquals(new MPQ(1, 16), new MPQ(1).div2Exp(4));
        assertEquals(new MPQ(-5), new MPQ(5).neg());
        assertEquals(new MPQ(5), new MPQ(-5).abs());
        assertEquals(new MPQ(-1, 5), new MPQ(-5).inv());
        assertThrows(ArithmeticException.class, () -> new MPQ().inv());
    }

    @Test
    void testArithmetic2() {
        var a = new MPQ();
        assertEquals(new MPQ(7), a.addAssign(a, new MPQ(7)));
        assertEquals(new MPQ(2), a.subAssign(a, new MPQ(5)));
        assertEquals(new MPQ(6), a.mulAssign(a, new MPQ(3)));
        assertEquals(new MPQ(6, 5), a.divAssign(a, new MPQ(5)));
        assertEquals(new MPQ(12, 5), a.mul2ExpAssign(a, 1));
        assertEquals(new MPQ(6, 5), a.div2ExpAssign(a, 1));
        assertEquals(new MPQ(-6, 5), a.negAssign(a));
        assertEquals(new MPQ(6, 5), a.absAssign(a));
        assertEquals(new MPQ(5, 6), a.invAssign(a));
    }

    @Test
    void testArithmetic3() {
        var a = new MPQ();
        assertEquals(new MPQ(7), a.addAssign(new MPQ(7)));
        assertEquals(new MPQ(2), a.subAssign(new MPQ(5)));
        assertEquals(new MPQ(6), a.mulAssign(new MPQ(3)));
        assertEquals(new MPQ(6, 5), a.divAssign(new MPQ(5)));
        assertEquals(new MPQ(12, 5), a.mul2ExpAssign(1));
        assertEquals(new MPQ(6, 5), a.div2ExpAssign(1));
        assertEquals(new MPQ(-6, 5), a.negAssign());
        assertEquals(new MPQ(6, 5), a.absAssign());
        assertEquals(new MPQ(5, 6), a.invAssign());
    }

    @Test
    void testComparison() {
        var a = new MPQ(10);
        var b = new MPQ(2);
        assertTrue(a.cmp(b) > 0);
        assertTrue(b.cmp(a) < 0);
        assertTrue(b.cmp(b) == 0);
        assertTrue(a.cmp(new MPZ(12)) < 0);
        assertTrue(a.cmp(-1, 3) > 0);
        assertTrue(a.cmpUi(-1, 3) < 0);
        assertFalse(a.equal(b));
        assertTrue(a.equal(a));

        assertEquals(1, new MPQ(2, 3).sgn());
        assertEquals(-1, new MPQ(-2, 3).sgn());
        assertEquals(0, new MPQ(0, 3).sgn());

        assertTrue(a.compareTo(b) > 0);
        assertTrue(b.compareTo(a) < 0);
        assertTrue(b.compareTo(b) == 0);
    }

    @Test
    void testHashCodeUsesDenominator() {
        var a = new MPQ(2, 3);
        var b = new MPQ(2, 5);

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
        assertEquals(new MPQ(2, 3).hashCode(), a.hashCode());
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        var a = new MPQ(1524132, 7);
        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(a);
        var arr = baos.toByteArray();
        oos.close();

        var ois = new ObjectInputStream(new ByteArrayInputStream(arr));
        var b = ois.readObject();
        assertEquals(a, b);
    }

    @Test
    void testNumDem() {
        var a = new MPQ(3, 5);
        var n = a.getNum();
        var d = a.getDen();
        assertEquals(new MPZ(3), n);
        assertEquals(new MPZ(5), d);
        d.set(1);
        assertEquals(new MPQ(3, 5), a);
        a.setNum(new MPZ(1));
        assertEquals(new MPQ(1, 5), a);
        a.setDen(new MPZ(3));
        assertEquals(new MPQ(1, 3), a);
    }

}
