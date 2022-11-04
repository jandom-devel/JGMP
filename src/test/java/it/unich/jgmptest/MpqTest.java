package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        var z = MPQ.init();
        assertEquals(new MPQ(0, 1), z);
        assertEquals(new MPQ(15, 1), z.set(new MPQ(15)));
        assertEquals(new MPQ(15, 1), z.set(new MPZ(15)));
        assertEquals(new MPQ(2, 3), z.set(2, 3));
        assertEquals(new MPQ(2, 3), z.setUi(2, 3));
        assertEquals(0, z.set("-1A", 16));
        assertEquals(new MPQ(-26, 1), z);
        assertEquals(-1, z.set("2", 63));
        assertEquals(new MPQ(-26, 1), z);
        assertEquals(0, z.set("-1A/7", 16));
        assertEquals(new MPQ(-26, 7), z);

        var z2 = new MPQ(-26);
        var z3 = new MPQ(2).swap(z2);
        assertEquals(new MPQ(-26), z3);
        assertEquals(new MPQ(2), z2);
    }

    @Test
    void testConversions() {
        var z = new MPQ();
        assertEquals(-5.25, new MPQ(-21, 4).getD());
        assertEquals(new MPQ(21, 4), z.set(5.25));
        assertThrows(IllegalArgumentException.class, () -> z.set(Double.POSITIVE_INFINITY));
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
        assertEquals(new MPQ(1, 16), new MPQ(1).div2Exp(4));
        assertEquals(new MPQ(-5), new MPQ(5).neg());
        assertEquals(new MPQ(5), new MPQ(-5).abs());
        assertEquals(new MPQ(-1, 5), new MPQ(-5).inv());
    }

    @Test
    void testArithmetic2() {
        var z = new MPQ();
        assertEquals(new MPQ(7), z.addAssign(z, new MPQ(7)));
        assertEquals(new MPQ(2), z.subAssign(z, new MPQ(5)));
        assertEquals(new MPQ(6), z.mulAssign(z, new MPQ(3)));
        assertEquals(new MPQ(6, 5), z.divAssign(z, new MPQ(5)));
        assertEquals(new MPQ(12, 5), z.mul2ExpAssign(z, 1));
        assertEquals(new MPQ(6, 5), z.div2ExpAssign(z, 1));
        assertEquals(new MPQ(-6, 5), z.negAssign(z));
        assertEquals(new MPQ(6, 5), z.absAssign(z));
        assertEquals(new MPQ(5, 6), z.invAssign(z));
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
    void testSerialization() throws IOException, ClassNotFoundException {
        var n = new MPQ(1524132, 7);
        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(n);
        var arr = baos.toByteArray();
        oos.close();

        var ois = new ObjectInputStream(new ByteArrayInputStream(arr));
        var n2 = ois.readObject();
        assertEquals(n, n2);
    }

    @Test
    void testNumDem() {
        var q = new MPQ(3, 5);
        var n = q.getNum();
        var d = q.getDen();
        assertEquals(new MPZ(3), n);
        assertEquals(new MPZ(5), d);
        d.set(1);
        assertEquals(new MPQ(3, 5), q);
        q.setNum(new MPZ(1));
        assertEquals(new MPQ(1, 5), q);
        q.setDen(new MPZ(3));
        assertEquals(new MPQ(1, 3), q);
    }

}
