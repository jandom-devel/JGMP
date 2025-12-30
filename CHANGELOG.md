# Changelog
All notable changes to this project will be documented in this file.

## [1.0.2]
### Changed
- Updated Maven plugins and library dependencies.
- Updated Maven project for new Java compilers.

### Fixed
- `NullPointerException` when converting `MPZ(0)` to `BigInteger` (#5).
- `MPQ.hashCode` uses only the numerator.
- Javadoc for `PrimalityStatus`.

## [1.0.1] - 2023-05-23
### Changed
- Updated Maven plugins and library dependencies.
- Refreshed minimum GMP version.

### Fixed
- Return value of `mp?_sgn`.
- `MPF.getD2Exp` test for old versions of GMP.

## [1.0] - 2023-04-06
- First GA release of JGMP

[1.0.2]: https://github.com/amato-gianluca/JGMP/compare/jgmp-1.0.1...HEAD
[1.0.1]: https://github.com/amato-gianluca/JGMP/compare/jgmp-1.0...jgmp-1.0.1
[1.0]: https://github.com/amato-gianluca/JGMP/compare/jgmp-0.2...jgmp-1.0
