/**
 * This package contains all the low-level classes of JGMP.
 *
 * All the code interfacing with the native C library is part of this package.
 * In case one wants to replace JNA with another library, the changes to the
 * JGMP would be almost entirely limited to this package.
 * <p>
 * The most important class is {@link LibGMP}, which containts the Java bindings
 * for the functions in the GMP C library. Other classes are Java proxies for the
 * parameter and return types used by these functions.
 */
package it.unich.jgmp.nativelib;
