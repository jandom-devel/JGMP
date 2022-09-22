package it.unich.jgmp;

import java.lang.ref.Cleaner;

import it.unich.jgmp.nativelib.LibGMP;

/**
 * Class collecting global variables and some static methods which do no fit
 * in more specific classes.
 */
public class GMP {
    /**
     * Cleaner used by the JGMP library.
     */
    static final Cleaner cleaner = Cleaner.create();

    /**
     * Returns the version of the native GMP library.
     */
    public static String getNativeVersion() {
        return LibGMP.__gmp_version;
    }
}
