package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Optional;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;
import it.unich.jgmp.MPZ.PrimalityStatus;
import it.unich.jgmp.RandState;

public class MpzTest {

    public static final String MAX_ULONG = "18446744073709551615";

    public static final MPZ zMaxUlong = new MPZ(MAX_ULONG);

    @Test
    void testInit() {
        assertEquals(new MPZ(0), MPZ.init());
        assertEquals(new MPZ(0), MPZ.init2(1000));
        assertEquals(new MPZ(15), new MPZ(15).realloc2(10000));
    }

    @Test
    void testAssignment() {
        var a = new MPZ();
        assertEquals(new MPZ(15), a.set(new MPZ(15)));
        assertEquals(zMaxUlong, a.setUi(-1));
        assertEquals(new MPZ(-3), a.set(-3));
        assertEquals(new MPZ(5), a.set(5.2));
        assertEquals(new MPZ(5), a.set(new MPQ(21, 4)));
        assertThrows(ArithmeticException.class, () -> a.set(Double.POSITIVE_INFINITY));
        assertEquals(0, a.set("-1A", 16));
        assertEquals(new MPZ(-26), a);
        assertEquals(-1, a.set("2", 63));
        assertEquals(new MPZ(-26), a);
        var z2 = new MPZ(-26);
        var z3 = new MPZ(2).swap(z2);
        assertEquals(new MPZ(-26), z3);
        assertEquals(new MPZ(2), z2);
    }

    @Test
    void testInitAndAssignment() {
        assertEquals(new MPZ(15), MPZ.initSet(new MPZ(15)));
        assertEquals(new MPZ(15), MPZ.initSetUi(15));
        assertEquals(zMaxUlong, MPZ.initSetUi(-1));
        assertEquals(new MPZ(-1), MPZ.initSet(-1));
        assertEquals(new MPZ(15), MPZ.initSet(15.2));
        assertEquals(new Pair<>(0, new MPZ(15)), MPZ.initSet("15", 10));
        assertEquals(new Pair<>(-1, new MPZ(0)), MPZ.initSet("15", 63));
        assertEquals(new Pair<>(-1, new MPZ(0)), MPZ.initSet("99", 7));
    }

    @Test
    void testConstructors() {
        assertEquals(new MPZ(0), new MPZ());
        assertEquals(new MPZ(15), new MPZ("15"));
        assertEquals(new MPZ(15), new MPZ(15.4));
        assertEquals(new MPZ(5), new MPZ(new MPQ(21, 4)));
    }

    @Test
    void testConversions() {
        assertEquals(4l, new MPZ(-4).getUi());
        assertEquals(-4l, new MPZ(-4).getSi());
        assertEquals(-4.0, new MPZ(-4).getD());
        assertEquals(new Pair<>(-0.5, 3l), new MPZ(-4).getD2Exp());
        assertEquals("125", new MPZ(125).getStr(10));
        assertEquals(null, new MPZ(125).getStr(63));
    }

    @Test
    void testArithmetic1() {
        assertEquals(new MPZ(15), new MPZ(8).add(new MPZ(7)));
        assertEquals(new MPZ(15), new MPZ(8).addUi(7));
        assertEquals(new MPZ(1), new MPZ(8).sub(new MPZ(7)));
        assertEquals(new MPZ(1), new MPZ(8).subUi(7));
        assertEquals(new MPZ(-1), new MPZ(8).uiSub(7));
        assertEquals(new MPZ(56), new MPZ(8).mul(new MPZ(7)));
        assertEquals(new MPZ(56), new MPZ(8).mulUi(7));
        assertEquals(new MPZ(-56), new MPZ(8).mul(-7));
        assertEquals(new MPZ(14), new MPZ(2).addmul(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(14), new MPZ(2).addmulUi(new MPZ(4), 3));
        assertEquals(new MPZ(-10), new MPZ(2).submul(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(-10), new MPZ(2).submulUi(new MPZ(4), 3));
        assertEquals(new MPZ(48), new MPZ(3).mul2Exp(4));
        assertEquals(new MPZ(-5), new MPZ(5).neg());
        assertEquals(new MPZ(5), new MPZ(-5).abs());
    }

    @Test
    void testArithmetic2() {
        var a = new MPZ(8);
        assertEquals(new MPZ(15), a.addAssign(new MPZ(7)));
        assertEquals(new MPZ(22), a.addUiAssign(7));
        assertEquals(new MPZ(15), a.subAssign(new MPZ(7)));
        assertEquals(new MPZ(8), a.subUiAssign(7));
        assertEquals(new MPZ(-1), a.uiSubAssign(7));
        assertEquals(new MPZ(-7), a.mulAssign(new MPZ(7)));
        assertEquals(new MPZ(-14), a.mulUiAssign(2));
        assertEquals(new MPZ(28), a.mulAssign(-2));
        assertEquals(new MPZ(40), a.addmulAssign(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(52), a.addmulUiAssign(new MPZ(4), 3));
        assertEquals(new MPZ(40), a.submulAssign(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(28), a.submulUiAssign(new MPZ(4), 3));
        assertEquals(new MPZ(56), a.mul2ExpAssign(1));
        assertEquals(new MPZ(-56), a.negAssign());
        assertEquals(new MPZ(56), a.absAssign());
    }

    @Test
    void testDivision1() {
        assertEquals(new MPZ(4), new MPZ(15).cdivq(new MPZ(4)));
        assertEquals(new MPZ(-1), new MPZ(15).cdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(4), new MPZ(-1)), new MPZ(15).cdivqr(new MPZ(4)));
        assertEquals(1, new MPZ(15).cdivUi(4));
        assertEquals(new MPZ(4), new MPZ(15).cdivq2Exp(2));
        assertEquals(new MPZ(-1), new MPZ(15).cdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).fdivq(new MPZ(4)));
        assertEquals(new MPZ(3), new MPZ(15).fdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(3), new MPZ(3)), new MPZ(15).fdivqr(new MPZ(4)));
        assertEquals(3, new MPZ(15).fdivUi(4));
        assertEquals(new MPZ(3), new MPZ(15).fdivq2Exp(2));
        assertEquals(new MPZ(3), new MPZ(15).fdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).tdivq(new MPZ(4)));
        assertEquals(new MPZ(3), new MPZ(15).tdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(3), new MPZ(3)), new MPZ(15).tdivqr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(-3), new MPZ(-3)), new MPZ(-15).tdivqr(new MPZ(4)));
        assertEquals(3, new MPZ(15).tdivUi(4));
        assertEquals(new MPZ(3), new MPZ(15).tdivq2Exp(2));
        assertEquals(new MPZ(3), new MPZ(15).tdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).mod(new MPZ(6)));
        assertEquals(new MPZ(3), new MPZ(-15).mod(new MPZ(-6)));
        assertEquals(new MPZ(4), new MPZ(12).divexact(new MPZ(3)));
        assertEquals(new MPZ(4), new MPZ(12).divexactUi(3));
        assertTrue(new MPZ(15).isDivisible(new MPZ(3)));
        assertTrue(new MPZ(15).isDivisibleUi(3));
        assertFalse(new MPZ(15).isDivisible2Exp(3));

        assertTrue(new MPZ(15).isCongruent(new MPZ(3), new MPZ(4)));
        assertTrue(new MPZ(15).isCongruentUi(3, 4));
        assertFalse(new MPZ(15).isCongruent2Exp(new MPZ(1), 3));
    }

    @Test
    void testDivision2() {
        var a = new MPZ(96);
        var r = new MPZ();
        assertEquals(new MPZ(24), a.cdivqAssign(new MPZ(4)));
        assertEquals(new MPZ(-16), a.cdivrAssign(new MPZ(20)));
        assertEquals(new MPZ(-5), a.cdivqrAssign(r, new MPZ(3)));
        assertEquals(new MPZ(-1), r);
        a.set(96);
        assertEquals(0, a.cdivqUiAssign(4));
        assertEquals(new MPZ(24), a);
        assertEquals(16, a.cdivrUiAssign(20));
        assertEquals(new MPZ(-16), a);
        assertEquals(1, a.cdivqrUiAssign(r, 3));
        assertEquals(new MPZ(-5), a);
        assertEquals(new MPZ(-1), r);
        a.set(15);
        assertEquals(new MPZ(4), a.cdivq2ExpAssign(2));
        assertEquals(new MPZ(-4), a.cdivr2ExpAssign(3));

        a.set(96);
        assertEquals(new MPZ(24), a.fdivqAssign(new MPZ(4)));
        assertEquals(new MPZ(4), a.fdivrAssign(new MPZ(20)));
        assertEquals(new MPZ(1), a.fdivqrAssign(r, new MPZ(3)));
        assertEquals(new MPZ(1), r);
        a.set(96);
        assertEquals(0, a.fdivqUiAssign(4));
        assertEquals(new MPZ(24), a);
        assertEquals(4, a.fdivrUiAssign(20));
        assertEquals(new MPZ(4), a);
        assertEquals(1, a.fdivqrUiAssign(r, 3));
        assertEquals(new MPZ(1), a);
        assertEquals(new MPZ(1), r);
        a.set(15);
        assertEquals(new MPZ(3), a.fdivq2ExpAssign(2));
        assertEquals(new MPZ(3), a.fdivr2ExpAssign(3));

        a.set(96);
        assertEquals(new MPZ(24), a.tdivqAssign(new MPZ(4)));
        assertEquals(new MPZ(4), a.tdivrAssign(new MPZ(20)));
        assertEquals(new MPZ(1), a.tdivqrAssign(r, new MPZ(3)));
        assertEquals(new MPZ(1), r);
        a.set(96);
        assertEquals(0, a.tdivqUiAssign(4));
        assertEquals(new MPZ(24), a);
        assertEquals(4, a.tdivrUiAssign(20));
        assertEquals(new MPZ(4), a);
        assertEquals(1, a.tdivqrUiAssign(r, 3));
        assertEquals(new MPZ(1), a);
        assertEquals(new MPZ(1), r);
        a.set(-15);
        assertEquals(new MPZ(-3), a.tdivq2ExpAssign(2));
        assertEquals(new MPZ(-3), a.tdivr2ExpAssign(3));

        a.set(15);
        assertEquals(new MPZ(3), a.modAssign(new MPZ(6)));
        assertEquals(new MPZ(3), a.modAssign(new MPZ(-10)));
        a.set(12);
        assertEquals(new MPZ(4), a.divexactAssign(new MPZ(3)));
        assertEquals(new MPZ(2), a.divexactUiAssign(2));
    }

    @Test
    void testDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> new MPZ(4).cdivq(new MPZ(0)));
        assertThrows(ArithmeticException.class, () -> new MPZ(4).fdivq(new MPZ(0)));
        assertThrows(ArithmeticException.class, () -> new MPZ(4).tdivq(new MPZ(0)));
        assertThrows(ArithmeticException.class, () -> new MPZ(4).cmp(Double.NaN));
        assertThrows(ArithmeticException.class, () -> new MPZ(4).cmpabs(Double.NaN));
    }

    @Test
    void testExponentiation1() {
        assertEquals(new MPZ(1), new MPZ(2).powm(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(1), new MPZ(2).powmUi(4, new MPZ(3)));
        assertEquals(new MPZ(1), new MPZ(2).powmSec(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(16), new MPZ(2).powUi(4));
        assertEquals(new MPZ(16), MPZ.powUi(2, 4));
    }

    @Test
    void testExponentiation2() {
        var a = new MPZ(2);
        assertEquals(new MPZ(3), a.powmAssign(new MPZ(4), new MPZ(13)));
        assertEquals(new MPZ(2), a.powmUiAssign(3, new MPZ(5)));
        assertEquals(new MPZ(3), a.powmSecAssign(new MPZ(4), new MPZ(13)));
        assertEquals(new MPZ(27), a.powUiAssign(3));
    }

    @Test
    void testRoots1() {
        assertEquals(new Pair<>(false, new MPZ(2)), new MPZ(17).root(4));
        assertEquals(new Pair<>(true, new MPZ(-3)), new MPZ(-27).root(3));
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).root(4));
        assertEquals(new Pair<>(new MPZ(2), new MPZ(1)), new MPZ(17).rootrem(4));
        assertEquals(new Pair<>(new MPZ(-3), new MPZ(-1)), new MPZ(-28).rootrem(3));
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).rootrem(4));

        assertEquals(new MPZ(8), new MPZ(65).sqrt());
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).sqrt());
        assertEquals(new Pair<>(new MPZ(8), new MPZ(1)), new MPZ(65).sqrtrem());
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).sqrtrem());
        assertTrue(new MPZ(8).isPerfectPower());
        assertFalse(new MPZ(8).isPerfectSquare());
        assertTrue(new MPZ(16).isPerfectSquare());
    }

    @Test
    void testRoots2() {
        var a = new MPZ();
        var z = new MPZ();
        z.set(17);
        assertEquals(false, z.rootAssign(4));
        assertEquals(new MPZ(2), z);
        z.set(-27);
        assertEquals(true, z.rootAssign(3));
        assertEquals(new MPZ(-3), z);
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).rootAssign(4));
        z.set(17);
        assertEquals(new MPZ(2), z.rootremAssign(a, 4));
        assertEquals(new MPZ(2), z);
        assertEquals(new MPZ(1), a);
        z.set(-28);
        assertEquals(new MPZ(-3), z.rootremAssign(a, 3));
        assertEquals(new MPZ(-3), z);
        assertEquals(new MPZ(-1), a);
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).rootremAssign(a, 4));
        assertEquals(new MPZ(-1), a);

        z.set(65);
        assertEquals(new MPZ(8), z.sqrtAssign());
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).sqrtAssign());
        z.set(65);
        assertEquals(new MPZ(8), z.sqrtremAssign(a));
        assertEquals(new MPZ(8), z);
        assertEquals(new MPZ(1), a);
        assertThrows(ArithmeticException.class, () -> new MPZ(-27).sqrtremAssign(a));
        assertEquals(new MPZ(1), a);
    }

    @Test
    void testNumberTheory1() {
        assertEquals(PrimalityStatus.PRIME, new MPZ(17).isProbabPrime(15));
        assertEquals(new MPZ(19), new MPZ(17).nextprime());
        assertEquals(new MPZ(6), new MPZ(30).gcd(new MPZ(24)));
        assertEquals(6, new MPZ(30).gcdUi(24));
        assertEquals(30, new MPZ(30).gcdUi(0));
        assertEquals(0, new MPZ(0).gcdUi(0));
        assertEquals(0, new MPZ(0).gcdUi(0));
        assertEquals(0, zMaxUlong.addUi(1).gcdUi(0));
        assertEquals(new Triplet<>(new MPZ(6), new MPZ(1), new MPZ(-1)), new MPZ(30).gcdext(new MPZ(24)));
        assertEquals(new MPZ(120), new MPZ(30).lcm(new MPZ(24)));
        assertEquals(new MPZ(120), new MPZ(30).lcmUi(24));
        assertEquals(Optional.of(new MPZ(3)), new MPZ(5).invert(new MPZ(7)));
        assertEquals(Optional.empty(), new MPZ(0).invert(new MPZ(7)));
        assertEquals(Optional.empty(), new MPZ(5).invert(new MPZ(0)));

        assertEquals(-1, new MPZ(5).jacobi(new MPZ(3)));
        assertEquals(0, new MPZ(9).legendre(new MPZ(3)));
        assertEquals(1, new MPZ(5).kronecker(new MPZ(4)));
        assertEquals(-1, new MPZ(27).kronecker(28));
        assertEquals(-1, new MPZ(27).kroneckerUi(28));
        assertEquals(1, new MPZ(27).kroneckerReverse(28));
        assertEquals(1, new MPZ(27).uiKronecker(28));
        assertEquals(new Pair<>(2l, new MPZ(3)), new MPZ(12).remove(new MPZ(2)));
        assertEquals(new MPZ(40320), MPZ.facUi(8));
        assertEquals(new MPZ(945), MPZ.dfacUi(9));
        assertEquals(new MPZ(28), MPZ.mfacUiUi(7, 3));
        assertEquals(new MPZ(210), MPZ.primorialUi(8));
        assertEquals(new MPZ(21), new MPZ(7).binUi(2));
        assertEquals(new MPZ(21), MPZ.binUiUi(7, 2));
        assertEquals(new MPZ(34), MPZ.fibUi(9));
        assertEquals(new Pair<>(new MPZ(34), new MPZ(21)), MPZ.fib2Ui(9));
        assertEquals(new MPZ(18), MPZ.lucnumUi(6));
        assertEquals(new Pair<>(new MPZ(18), new MPZ(11)), MPZ.lucnum2Ui(6));
    }

    @Test
    void testNumberTheory2() {
        var a = new MPZ();
        var s = new MPZ();
        var t = new MPZ();
        a.set(17);
        assertSame(a, a.nextprimeAssign());
        assertEquals(new MPZ(19), a);
        a.set(30);
        assertSame(a, a.gcdAssign(new MPZ(24)));
        assertEquals(new MPZ(6), a);
        a.set(30);
        assertEquals(6, a.gcdUiAssign(24));
        assertEquals(new MPZ(6), a);
        a.set(30);
        assertSame(a, a.gcdextAssign(s, t, new MPZ(24)));
        assertEquals(new MPZ(6), a);
        assertEquals(new MPZ(1), s);
        assertEquals(new MPZ(-1), t);
        a.set(30);
        assertSame(a, a.lcmAssign(new MPZ(24)));
        assertEquals(new MPZ(120), a);
        a.set(30);
        assertSame(a, a.lcmUiAssign(24));
        assertEquals(new MPZ(120), a);
        a.set(5);
        assertEquals(true, a.invertAssign(new MPZ(7)));
        assertEquals(new MPZ(3), a);
        a.set(5);
        assertEquals(false, a.invertAssign(new MPZ(0)));
        assertEquals(new MPZ(5), a);
        a.set(12);
        assertEquals(2, a.removeAssign(new MPZ(2)));
        assertEquals(new MPZ(3), a);
        a.set(7);
        assertSame(a, a.binUiAssign(2));
        assertEquals(new MPZ(21), a);
    }

    @Test
    void testComparison() {
        var a = new MPZ(10);
        var b = new MPZ(2);
        assertTrue(a.compareTo(b) > 0);
        assertEquals(0, a.cmp(10.0));
        assertTrue(a.cmp(-1) > 0);
        assertTrue(a.cmpUi(-1) < 0);
        assertTrue(a.cmpabs(b) > 0);
        assertEquals(0, a.cmpabs(-10.0));
        assertTrue(a.cmpabsUi(-1) < 0);
        assertTrue(a.sgn() > 0);
    }

    @Test
    void testBitManipulation1() {
        var a = new MPZ(65535);
        assertEquals(1, a.tstbit(15));
        assertEquals(0, a.tstbit(16));
        assertEquals(new MPZ(32767), a.combit(15));
        assertEquals(new MPZ(32767), a.clrbit(15));
        assertEquals(a, a.setbit(15));
        assertEquals(0, a.scan1(0));
        assertEquals(16, a.scan0(0));
        assertEquals(0, a.hamdist(a));
        assertEquals(16, a.popcount());

        var b = new MPZ(15);
        var c = new MPZ(17);
        assertEquals(new MPZ(1), b.and(c));
        assertEquals(new MPZ(31), b.ior(c));
        assertEquals(new MPZ(30), b.xor(c));
        assertEquals(new MPZ(-16), b.com());
    }

    @Test
    void testBitManipulation2() {
        var a = new MPZ(15);
        assertEquals(new MPZ(5), a.andAssign(new MPZ(21)));
        assertEquals(new MPZ(21), a.iorAssign(new MPZ(16)));
        assertEquals(new MPZ(22), a.xorAssign(new MPZ(3)));
        assertSame(a, a.comAssign());
        assertEquals(new MPZ(-23), a);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testRandom() {
        var rs = new RandState();
        var a = MPZ.urandomb(rs, 2);
        assertTrue(a.cmp(0) >= 0);
        assertTrue(a.cmp(3) <= 0);
        var b = MPZ.urandomm(rs, new MPZ(10));
        assertTrue(b.cmp(0) >= 0);
        assertTrue(b.cmp(10) <= 0);
        var c = MPZ.rrandomb(rs, 2);
        assertTrue(c.cmp(0) >= 0);
        assertTrue(c.cmp(3) <= 0);
        MPZ.random(10);
        MPZ.random2(10);
    }

    @Test
    void testImportExport() {
        var a = new MPZ("124485");
        var buffer = a.bufferExport(1, 1, 0, 0);
        var b = MPZ.bufferImport(1, 1, 0, 0, buffer);
        assertEquals(a, b);
    }

    @Test
    void testMiscellaneous() {
        var a = new MPZ("-213945");
        assertTrue(a.fitsSlong());
        assertFalse(a.fitsUlong());
        assertTrue(a.fitsSint());
        assertFalse(a.fitsUint());
        assertFalse(a.fitsSshort());
        assertFalse(a.fitsUshort());
        assertTrue(a.isOdd());
        assertFalse(a.isEven());
        assertTrue(a.sizeinbase(10) >= 6);
        assertTrue(a.sizeinbase(10) <= 7);
        assertThrows(IllegalArgumentException.class, () -> a.sizeinbase(-20));
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        var a = new MPZ(1524132);
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
    void testBigintegerConversion() {
        var s = "29329328922232322032";
        var z = new MPZ(s);
        var zneg = z.neg();
        var bi = new BigInteger(s);
        var bineg = bi.negate();

        assertEquals(z, new MPZ(bi));
        assertEquals(zneg, new MPZ(bineg));

        var tmp = new MPZ(0);
        tmp.set(bi);
        assertEquals(z, tmp);
        tmp.set(bineg);
        assertEquals(zneg, tmp);

        assertEquals(bi, z.getBigInteger());
        assertEquals(bineg, zneg.getBigInteger());
    }

}
