package it.unich.jgmp.nativejnr;

import java.lang.ref.Cleaner;
import java.util.HashMap;
import java.util.Map;

import jnr.ffi.LibraryLoader;
import jnr.ffi.LibraryOption;

public class GMP {
    /**
     * The undecorated name of the GMP library.
     */
    static final String LIBNAME = "gmp";

    public static LibGMP libGMP;

    static {
        Map<LibraryOption, Object> libraryOptions = new HashMap<>();
        libraryOptions.put(LibraryOption.LoadNow, true);
        libraryOptions.put(LibraryOption.IgnoreError, true);
        libGMP = LibraryLoader.loadLibrary(LibGMP.class, libraryOptions, LIBNAME);
    }

    /**
     * Cleaner used by the JGMP library.
     */
    static final Cleaner cleaner = Cleaner.create();

    /**
     * Returns the version of the native GMP library.
     */
   /*  public static String getNativeVersion() {
        return LibGMP.__gmp_version;
    } */
}
