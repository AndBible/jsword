package org.crosswire.ksword.parity;

import java.util.List;
import java.util.function.BiFunction;

import org.crosswire.jsword.book.Books;

/**
 * The parity operation table. This one source file is compiled twice: against the frozen baseline jar
 * (and loaded into the oracle's isolated class loader) and against the current code (jvmTest). The same
 * source gives both sides identical canonical strings, and compiling it against the current code also
 * enforces that the Java-visible API is unchanged. Only JDK types cross the class-loader boundary.
 */
public final class ParityProbe implements BiFunction<String, List<String>, String> {
    @Override
    public String apply(String op, List<String> args) {
        switch (op) {
            case "self.codeSource":
                return Books.class.getProtectionDomain().getCodeSource().getLocation().toString();
            case "self.rootResource":
                return String.valueOf(Books.class.getClassLoader().getResource("BibleNames.properties"));
            default:
                throw new IllegalArgumentException("Unknown parity op: " + op);
        }
    }
}
