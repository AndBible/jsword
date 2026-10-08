package org.crosswire.ksword.parity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;

import org.crosswire.common.crypt.Sapphire;
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
            case "sapphire.cipher": {
                // args: key (decoded with ISO-8859-1 exactly like Sapphire's callers do; "<null>" = null key), plaintext hex
                byte[] key = "<null>".equals(args.get(0)) ? null : args.get(0).getBytes(StandardCharsets.ISO_8859_1);
                Sapphire s = new Sapphire(key);
                byte[] in = hex(args.get(1));
                byte[] out = new byte[in.length];
                for (int i = 0; i < in.length; i++) {
                    out[i] = s.cipher(in[i]);
                }
                return hex(out);
            }
            case "sapphire.hash": {
                // args: key ("" = hash mode), data hex, hash length
                Sapphire s = new Sapphire(args.get(0).getBytes(StandardCharsets.ISO_8859_1));
                for (byte b : hex(args.get(1))) {
                    s.cipher(b);
                }
                byte[] hash = new byte[Integer.parseInt(args.get(2))];
                s.hashFinal(hash);
                return hex(hash);
            }
            default:
                throw new IllegalArgumentException("Unknown parity op: " + op);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
