# KSword parity oracle

`jsword-baseline.jar` is the JVM jar of the pruned, pre-conversion JSword: the **KSword baseline**.
Parity tests (`./gradlew parityTest`) load it in an isolated class loader and compare it with the
current code.

| Item | Value |
|---|---|
| Baseline commit | `50b241187ceb289495e16c1c94a59699159481e7` |
| Convenience tag | `ksword-base` (may be missing; the commit is authoritative) |

Rebuild: `oracle/build-oracle.sh` (builds the commit above via `git archive`, writes the jar and stamps
`Baseline-Commit` into its manifest). The jar is immutable; rebuild only to recover it.
