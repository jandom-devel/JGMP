package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPZ;
import it.unich.jgmp.MPZ.PrimalityStatus;
import it.unich.jgmp.MPZ.Reserve;
import it.unich.jgmp.MPZ.Unsigned;
import it.unich.jgmp.RandomState;

public class MPZTest {

    @Test
    void test_init() {
        assertEquals(new MPZ(0), new MPZ());
        assertEquals(new MPZ(0), new MPZ(100, Reserve.RESERVE));
        var z = new MPZ(15);
        z.realloc2(10000);
        assertEquals(new MPZ(15), z);
    }

    @Test
    void test_assignment() {
        var z = new MPZ();
        assertEquals(new MPZ(15), z.setValue(new MPZ(15)));
        assertEquals(new MPZ(2), z.setValue(2));
        assertEquals(new MPZ(-3), z.setValueSigned(-3));
        assertEquals(new MPZ(5), z.setValue(5.2));
        assertEquals(new MPZ(-26), z.setValue("-1A", 16));
        assertEquals(new MPZ(-12), z.setValue("-12"));
        var z2 = new MPZ(2).swap(z);
        assertEquals(new MPZ(-12), z2);
        assertEquals(new MPZ(2), z);
    }

    @Test
    void test_initandassignment() {
        assertEquals(new MPZ(15), new MPZ(new MPZ(15)));
        assertEquals(new MPZ(15), new MPZ(15, Unsigned.UNSIGNED));
        assertEquals(new MPZ(15), new MPZ(15.2));
        assertEquals(new MPZ(15), new MPZ("15"));
    }

    @Test
    void test_conversion() {
        assertEquals(4l, new MPZ(-4).getUi());
        assertEquals(-4l, new MPZ(-4).getSi());
        assertEquals(-4.0, new MPZ(-4).getD());
        assertEquals(new Pair<>(-0.5, 3l), new MPZ(-4).getD2Exp());
        assertEquals("125", new MPZ(125).getStr(10));
    }

    @Test
    void test_arithmetic() {
        assertEquals(new MPZ(15), new MPZ(8).add(new MPZ(7)));
        assertEquals(new MPZ(15), new MPZ(8).add(7));
        assertEquals(new MPZ(1), new MPZ(8).sub(new MPZ(7)));
        assertEquals(new MPZ(1), new MPZ(8).sub(7));
        assertEquals(new MPZ(-1), new MPZ(8).subReverse(7));
        assertEquals(new MPZ(56), new MPZ(8).mul(new MPZ(7)));
        assertEquals(new MPZ(56), new MPZ(8).mul(7));
        assertEquals(new MPZ(-56), new MPZ(8).mulSigned(-7));
        assertEquals(new MPZ(14), new MPZ(2).addmul(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(14), new MPZ(2).addmul(new MPZ(4), 3));
        assertEquals(new MPZ(-10), new MPZ(2).submul(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(-10), new MPZ(2).submul(new MPZ(4), 3));
        assertEquals(new MPZ(48), new MPZ(3).mul2Exp(4));
        assertEquals(new MPZ(-5), new MPZ(5).neg());
        assertEquals(new MPZ(5), new MPZ(-5).abs());
    }

    @Test
    void test_division() {
        assertEquals(new MPZ(4), new MPZ(15).cdivq(new MPZ(4)));
        assertEquals(new MPZ(-1), new MPZ(15).cdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(4), new MPZ(-1)), new MPZ(15).cdivqr(new MPZ(4)));
        assertEquals(1, new MPZ(15).cdiv(4));
        assertEquals(new MPZ(4), new MPZ(15).cdivq2Exp(2));
        assertEquals(new MPZ(-1), new MPZ(15).cdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).fdivq(new MPZ(4)));
        assertEquals(new MPZ(3), new MPZ(15).fdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(3), new MPZ(3)), new MPZ(15).fdivqr(new MPZ(4)));
        assertEquals(3, new MPZ(15).fdiv(4));
        assertEquals(new MPZ(3), new MPZ(15).fdivq2Exp(2));
        assertEquals(new MPZ(3), new MPZ(15).fdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).tdivq(new MPZ(4)));
        assertEquals(new MPZ(3), new MPZ(15).tdivr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(3), new MPZ(3)), new MPZ(15).tdivqr(new MPZ(4)));
        assertEquals(new Pair<>(new MPZ(-3), new MPZ(-3)), new MPZ(-15).tdivqr(new MPZ(4)));
        assertEquals(3, new MPZ(15).tdiv(4));
        assertEquals(new MPZ(3), new MPZ(15).tdivq2Exp(2));
        assertEquals(new MPZ(3), new MPZ(15).tdivr2Exp(2));

        assertEquals(new MPZ(3), new MPZ(15).mod(new MPZ(6)));
        assertEquals(new MPZ(3), new MPZ(-15).mod(new MPZ(-6)));
        assertEquals(new MPZ(4), new MPZ(12).divexact(new MPZ(3)));
        assertEquals(new MPZ(4), new MPZ(12).divexact(3));
        assertTrue(new MPZ(15).isDivisible(new MPZ(3)));
        assertTrue(new MPZ(15).isDivisible(3));
        assertFalse(new MPZ(15).isDivisible2Exp(3));

        assertTrue(new MPZ(15).isCongruent(new MPZ(3), new MPZ(4)));
        assertTrue(new MPZ(15).isCongruent(3, 4));
        assertFalse(new MPZ(15).isCongruent2Exp(new MPZ(1), 3));
    }

    @Test
    void test_exponentiation() {
        assertEquals(new MPZ(1), new MPZ(2).powm(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(1), new MPZ(2).powm(4, new MPZ(3)));
        assertEquals(new MPZ(1), new MPZ(2).powmSec(new MPZ(4), new MPZ(3)));
        assertEquals(new MPZ(16), new MPZ(2).pow(4));
        assertEquals(new MPZ(16), MPZ.pow(2, 4));
    }

    @Test
    void test_roots() {
        assertEquals(new MPZ(2), new MPZ(17).root(4));
        assertEquals(new Pair<>(new MPZ(2), new MPZ(1)), new MPZ(17).rootrem(4));
        assertEquals(new MPZ(8), new MPZ(65).sqrt());
        assertEquals(new Pair<>(new MPZ(8), new MPZ(1)), new MPZ(65).sqrtrem());
        assertTrue(new MPZ(8).isPerfectPower());
        assertFalse(new MPZ(8).isPerfectSquare());
        assertTrue(new MPZ(16).isPerfectSquare());
    }

    @Test
    void test_numbertheory() {
        assertEquals(PrimalityStatus.PRIME, new MPZ(17).isProbabPrime(15));
        assertEquals(new MPZ(19), new MPZ(17).nextprime());
        assertEquals(new MPZ(6), new MPZ(30).gcd(new MPZ(24)));
        assertEquals(6, new MPZ(30).gcd(24));
        assertEquals(new Triplet<>(new MPZ(6), new MPZ(1), new MPZ(-1)), new MPZ(30).gcdext(new MPZ(24)));
        assertEquals(new MPZ(120), new MPZ(30).lcm(new MPZ(24)));
        assertEquals(new MPZ(120), new MPZ(30).lcm(24));
        assertEquals(Optional.of(new MPZ(3)), new MPZ(5).invert(new MPZ(7)));
        assertEquals(Optional.empty(), new MPZ(5).invert(new MPZ(5)));
        assertEquals(-1, new MPZ(5).jacobi(new MPZ(3)));
        assertEquals(0, new MPZ(9).legendre(new MPZ(3)));
        assertEquals(1, new MPZ(5).kronecker(new MPZ(4)));
        assertEquals(-1, new MPZ(27).kronecker(28));
        assertEquals(-1, new MPZ(27).kronecker(28, Unsigned.UNSIGNED));
        assertEquals(1, new MPZ(27).kroneckerReverse(28));
        assertEquals(1, new MPZ(27).kroneckerReverse(28, Unsigned.UNSIGNED));
        assertEquals(new Pair<>(new MPZ(3), 2l), new MPZ(12).remove(new MPZ(2)));
        assertEquals(new MPZ(40320), MPZ.fac(8));
        assertEquals(new MPZ(945), MPZ.dfac(9));
        assertEquals(new MPZ(28), MPZ.mfac(7, 3));
        assertEquals(new MPZ(210), MPZ.primorial(8));
        assertEquals(new MPZ(21), new MPZ(7).bin(2));
        assertEquals(new MPZ(21), MPZ.bin(7, 2));
        assertEquals(new MPZ(34), MPZ.fib(9));
        assertEquals(new Pair<>(new MPZ(34), new MPZ(21)), MPZ.fib2(9));
        assertEquals(new MPZ(18), MPZ.lucnum(6));
        assertEquals(new Pair<>(new MPZ(18), new MPZ(11)), MPZ.lucnum2(6));
    }

    @Test
    void test_randomstate() {
        var a = new RandomState();
        assertDoesNotThrow(() -> new RandomState());
        assertDoesNotThrow(() -> RandomState.create());
        assertDoesNotThrow(() -> RandomState.mt());
        assertDoesNotThrow(() -> RandomState.lc(10));
        assertDoesNotThrow(() -> new RandomState(a));
        assertDoesNotThrow(() -> RandomState.valueOf(a));
        assertThrows(IllegalArgumentException.class, () -> RandomState.lc(200));
    }

    @Test
    void test_comparison() {
        var a = new MPZ(10);
        var b = new MPZ(2);
        assertTrue(a.compareTo(b) > 0);
        assertEquals(0, a.compareTo(10.0));
        assertTrue(a.compareTo(-1l) > 0);
        assertTrue(a.compareTo(-1l, Unsigned.UNSIGNED) < 0);

        assertTrue(a.compareAbsTo(b) > 0);
        assertEquals(0, a.compareAbsTo(-10.0));
        assertTrue(a.compareAbsTo(-1l) > 0);
        assertTrue(a.compareAbsTo(-1l, Unsigned.UNSIGNED) < 0);
    }

    @Test
    void test_bitmanipulation() {
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
    void test_random() {
        var s = new RandomState();
        var a = MPZ.urandomb(s, 2);
        assertTrue(a.compareTo(0) >= 0);
        assertTrue(a.compareTo(3) <= 0);
        var b = MPZ.urandomm(s, new MPZ(10));
        assertTrue(b.compareTo(0) >= 0);
        assertTrue(b.compareTo(10) <= 0);
        var c = MPZ.rrandomb(s, 2);
        assertTrue(c.compareTo(0) >= 0);
        assertTrue(c.compareTo(3) <= 0);
        MPZ.random(10);
        MPZ.random2(10);
    }

    @Test
    void test_importexport() {
        var a = new MPZ("124485");
        var buffer = a.export(1, 1, 0, 0);
        var b = new MPZ();
        b.importAssign(1, 1, 0, 0, buffer);
        assertEquals(a, b);
    }

    @Test
    void test_miscellaneous() {
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

}
