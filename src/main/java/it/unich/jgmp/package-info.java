/**
 * This package contains all the high-level classes of JGMP.
 *
 * <p>
 * Here are some guidelines we have followed in the developing the API of JGMP.
 * This rules may be overriden in particular cases.
 * </p>
 *
 * <h2>Naming conventions</h2>
 * <p>
 * In developing the interface of JGMP classes, we tried to adhere to Java
 * naming conventions, while keeping methods discoverable by people who already
 * know the C GMP library. For each GMP function, a <em>base name</em> is
 * determined by the original name as follows:
 * <ul>
 * <li>the prefix ({@code mpz_}, {@code gmp_} or other) and the components
 * {@code _si}, {@code _d} and {@code _str} are removed, if this does not clause
 * prototype clashes;
 * <li>the component {@code _ui} is kept to help the user distinguish unsigned
 * long parameters;
 * <li>the postfix {@code _p}, which sometimes marks functions returning
 * booleans, is either removed or replaced by the prefix {@code is} when it
 * makes sense;
 * <li>the rest of the name is transformed to camel case, by converting to
 * uppercase the first letter after each underscore;
 * <li>name which could cause conflicts with Java reserwed words are modified:
 * for example, {@code import} and {@code export} become {@code bufferImport}
 * and {@code bufferExport} respectively.
 * </ul>
 * The base name is then used to give a name to the corresponding JVM method,
 * following different rules for different classes.
 *
 * <h2>Type mapping</h2>
 * <p>
 * The types of the formal parameters and return value of a GMP function are
 * mapped to the types of the JGMP method as follows:
 * <ul>
 * <li>{@code int} and {@code long}) Generally mapped to the respective Java
 * types. This may cause truncation when the native {@code long} is only 32 bit.
 * If an {@code int} is used to represent a boolean, then the {@code boolean}
 * type is used in JGMP.
 * <li>{@code unsigned long}, {@code size_t}, {@code mp_bitcnt},
 * {@code mp_size_t} and {@code mp_exp_t}) Mapped to {@code long}. This may
 * cause truncation when the native size of these types is only 32 bit. Noote
 * that{@code unsigned long}, {@code size_t} and {@code mp_bitcnt} are natively
 * unsigned. Handle with care.
 * <li>{@code mpz_t}) Mapped to {@code MPZ}.
 * <li>{@code mpq_t}) Mapped to {@code MPQ}.
 * <li>{@code mpf_t}) Mapped to {@code MPF}.
 * <li>{@code gmp_randstate_t}) Mapped to {@code RandState}.
 * <li>{@code const char*}) Mapped to {@code String}.
 * <li>{@code char*}) All functions requiring a non-const {@code char*} may be
 * called with a {@code null} pointer. Since this is much easier to handle, we
 * choose to always follow this pattern. Therefore, non-constant {@code char*}
 * are always removed from the input parameters. When {@code char*} is used a
 * return value, it is mapped to a {@code String}.
 * <li>{@code void}) If a method would return {@code void}, we prefer to return
 * {@code this} instead, in order to ease chaining method calls;
 * </ul>
 */
package it.unich.jgmp;