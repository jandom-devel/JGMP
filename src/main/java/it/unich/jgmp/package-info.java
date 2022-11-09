/**
 * This package contains all the high-level classes of JGMP.
 *
 * <p>
 * Here are some guidelines we have followed in developing the API for the JGMP
 * library. We tried to adhere to Java conventions, while keeping methods
 * discoverable by people who already know the GMP library. These rules may be
 * overriden in specific cases.
 * </p>
 *
 * <h2>Naming conventions: the base name</h2>
 * <p>
 * For each GMP function, a <em>base name</em> is determined by the original
 * name as follows:
 * <ul>
 * <li>the prefix ({@code mpf_}, {@code mpq_}, {@code mpz_} or {@code gmp_}) is
 * removed; the same happens to the components {@code _si}, {@code _d} and
 * {@code _str}, if this does not clause signature clashes;
 * <li>the component {@code _ui} is kept to help the user distinguish unsigned
 * long parameters;
 * <li>the postfix {@code _p}, which sometimes marks functions returning
 * booleans, is either removed or replaced by the prefix {@code is} when this
 * makes sense;
 * <li>the rest of the name is transformed to camel case, by converting to
 * uppercase the first letter after each underscore;
 * <li>name which could cause conflicts with Java reserved words are modified:
 * for example, {@code import} and {@code export} become {@code bufferImport}
 * and {@code bufferExport} respectively.
 * </ul>
 * The base name is then used to give a name to the corresponding Java method,
 * according to the following rules.
 *
 * <h2>Naming conventions: general rules</h2>
 * <p>
 * Given a GMP function, the following rules dictate how to derive name and
 * signature for the corrisponding method in JGMP. Let
 * <code><em>mptype</em></code> be the family of the function, which may be
 * either {@code mpf}, {@code mpq} or {@code mpz}. The function becomes a class
 * of the corresponding {@link MPF}, {@link MPQ} or {@link MPZ} class. In
 * particular:
 * <ul>
 * <li>the function <code><em>mptype</em>_clear</code> is only used internally;
 * <li>the functions <code><em>mptype</em>_inits</code> and
 * <code><em>mptype</em>_clears</code> are only used internally and are not
 * exposed by the high-level classes;
 * <li>if {@code baseName} begins with {@code realloc2}, {@code set} or
 * {@code swap}, we create a method called {@code baseName} which calls the
 * original function, implicitly using {@code this} as the first
 * <code><em>mptype</em></code> parameter;
 * <li>if {@code baseName} begins with {@code init}, we create a side-effect
 * free static method (see later);
 * <li>for all the other functions:
 * <ul>
 * <li>if the function has at least a non constant
 * <code><em>mptype</em>_t</code> parameter, then we create a method
 * {@code baseNameAssign} which calls the original function, implicitly using
 * {@code this} as the first non-constant <code><em>mptype</em>_t</code>
 * parameter;
 * <li>we create e side-effect free method called {@code baseName}, with the
 * exception of a few cases where such as a method would not be particularly
 * useful.
 * </ul>
 * </ul>
 * In general, all the parameters which are not provided implicitly to the
 * original GMP function through {@code this} should be provided explicitly.
 * <p>
 * Other methods and constructors which conform to standard Java naming
 * conventions might be provided.
 *
 * <h2>Side-effect free methods</h2>
 * <p>
 * In order to simplify the development of code without side-effects, we enrich
 * the API provided by JGMP with side-effect free methods, which builds new
 * objects instead of modifying old ones.
 *
 * First of all, we distinguish between input and output parameters for the GMP
 * function. Some parameters may have both an input and an output nature. The
 * side-effect free method takes all input parameters in its signature, with the
 * exception of the first input <code><em>mptype</em>_t</code> parameter which
 * is mapped to {@code this}. If there are no input
 * <code><em>mptype</em>_t</code> parameters, the method will be static. The
 * method creates new objects for the output parameters, eventually cloning the
 * ones also used as an input. After calling the GMP functions, the return value
 * and all the output parameters are returned by the method, eventually packed
 * in an {@link org.javatuples.Tuple org.javatuples.Tuple}, from left to right
 * according to the function signature. Sometimes, when the first
 * <code><em>mptype</em>_t</code> input parameter comes after other input
 * parameters, this procedure may lead to a signature clash. In this case, the
 * name of the method is changed into {@code baseNameReverse}.
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
 * cause truncation when the native size of these types is only 32 bit. Note
 * that {@code unsigned long}, {@code size_t} and {@code mp_bitcnt} are natively
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
 * {@code this} instead, in order to ease chaining method calls.
 * </ul>
 *
 */
package it.unich.jgmp;