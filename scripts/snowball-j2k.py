#!/usr/bin/env python3
"""Deterministic Java -> Kotlin conversion of the Lucene 3.6.2 Snowball-generated stemmers.

Usage: snowball-j2k.py <lucene-analyzers-3.6.2-sources.jar-extract-dir> <out-dir> <bsd-header-file>

The generated Java has a tiny, regular shape (see the line patterns below); anything outside it aborts the
conversion instead of guessing. Committed output lives in
`src/commonMain/kotlin/org/crosswire/jsword/index/analysis/snowball/`; the script is kept so the conversion can be
audited and repeated, not because the sources change (Lucene 3.6.2 is frozen).
"""
import re
import sys
from pathlib import Path

LANGS = ["Danish", "Dutch", "Finnish", "French", "Italian", "Norwegian", "Portuguese", "Russian", "Spanish", "Swedish"]


def convert(name, java, header):
    out = [header, "", "package org.crosswire.jsword.index.analysis.snowball", "",
           "/**",
           f" * Lucene 3.6.2 `org.tartarus.snowball.ext.{name}Stemmer` (Snowball-generated), converted to Kotlin mechanically",
           " * by `jsword/scripts/snowball-j2k.py`. Behaviour is that of the Java original; compare with the baseline via",
           " * the vocabulary parity test.",
           " */",
           f"internal class {name}Stemmer : SnowballStemmer() {{"]
    N_HEAD = len(out)
    conditional = set()
    stack = []  # 'blk' or ['sw', case_open]
    in_among = False
    lines = java.split("\n")
    body_started = False
    for raw in lines:
        s = raw.strip()
        if not body_started:
            if s.startswith("public class"):
                body_started = True
            continue
        if not s:
            continue
        if s == "}" and not stack and not in_among:
            continue  # end of class
        no_str = re.sub(r'"(\\.|[^"\\])*"', '""', s)
        # --- among tables
        m = re.match(r"private (?:static final )?Among (\w+)\[\] = \{$", s)
        if m:
            out.append(f"private val {m.group(1)}: Array<Among> = arrayOf(")
            in_among = True
            continue
        if in_among:
            if s == "};":
                out.append(")")
                in_among = False
                continue
            m = re.match(r'new Among \( ("(?:\\.|[^"\\])*"), (-?\d+), (-?\d+), "(\w*)", this\)(,?)$', s)
            if not m:
                raise SystemExit(f"{name}: unexpected among line: {s}")
            lit, sub, res, meth, comma = m.groups()
            # Lucene 3.6.2 resolves the condition with getDeclaredMethod and invokes it from SnowballProgram; the method is
            # private in the stemmer, so Method.invoke throws IllegalAccessException, which find_among swallows as
            # "condition false". The baseline therefore never takes a conditional entry (Finnish "den"/"seen"/"tten"/
            # "siin" with r_VI/r_LONG); reproduced on purpose, so the lambda is `{ false }` (verified against the probe).
            lam = f" {{ false /* {meth}: inaccessible in Lucene 3.6.2 */ }}" if meth else ""
            if meth:
                conditional.add(meth)
            out.append(f"Among({lit}, {sub}, {res}){lam}{comma}")
            continue
        # --- fields
        m = re.match(r"private static final char (\w+)\[\] = \{([\d, ]*)\};$", s)
        if m:
            out.append(f"private val {m.group(1)} = intArrayOf({m.group(2).strip()})")
            continue
        m = re.match(r"private int (\w+);$", s)
        if m:
            out.append(f"private var {m.group(1)} = 0"); continue
        m = re.match(r"private boolean (\w+);$", s)
        if m:
            out.append(f"private var {m.group(1)} = false"); continue
        m = re.match(r"private StringBuilder (\w+) = new StringBuilder\(\);$", s)
        if m:
            out.append(f"private var {m.group(1)} = StringBuilder()"); continue
        # --- methods
        m = re.match(rf"private void copy_from\({name}Stemmer other\) \{{$", s)
        if m:
            out.append(f"private fun copy_from(other: {name}Stemmer) {{"); stack.append("blk"); continue
        m = re.match(r"private boolean (r_\w+)\(\) \{$", s)
        if m:
            out.append(f"private fun {m.group(1)}(): Boolean {{"); stack.append("blk"); continue
        if s == "public boolean stem() {":
            out.append("override fun stem(): Boolean {"); stack.append("blk"); continue
        # --- statements
        if s.startswith("//"):
            out.append(s); continue
        m = re.match(r"int (\w+)(?: = (.+))?;$", s)
        if m:
            out.append(f"var {m.group(1)} = {m.group(2) or 0}"); continue
        if no_str.startswith("switch(among_var) {"):
            out.append("when (among_var) {"); stack.append(["sw", False]); continue
        m = re.match(r"case (\d+):$", s)
        if m:
            top = stack[-1] if stack else None
            if not (isinstance(top, list) and top[0] == "sw"):
                raise SystemExit(f"{name}: case outside switch")
            if top[1]:
                out.append("}")
            out.append(f"{m.group(1)} -> {{")
            top[1] = True
            continue
        if s == "break;":
            top = stack[-1] if stack else None
            if isinstance(top, list) and top[0] == "sw":
                continue  # end of a when branch
            raise SystemExit(f"{name}: unlabeled break outside switch top level")
        m = re.match(r"break (\w+);$", s)
        if m:
            out.append(f"break@{m.group(1)}"); continue
        m = re.match(r"continue (\w+);$", s)
        if m:
            out.append(f"continue@{m.group(1)}"); continue
        m = re.match(r"(\w+): do \{$", s)
        if m:
            out.append(f"{m.group(1)}@ do {{"); stack.append("blk"); continue
        m = re.match(r"(\w+): while\(true\)$", s)
        if m:
            out.append(f"{m.group(1)}@ while (true)"); continue
        if s == "} while (false);":
            stack.pop(); out.append("} while (false)"); continue
        if s == "{":
            prev = next((o for o in reversed(out) if not o.startswith("//")), "")
            if re.match(r"(if \(|\w+@ while \(true\)$|else)", prev):
                stack.append("blk"); out.append("{")
            else:
                # A bare Java block is a scope; Kotlin would read `{` as a lambda, so it becomes a one-shot loop.
                stack.append("bare"); out.append("do {")
            continue
        if s == "}":
            top = stack.pop()
            if isinstance(top, list):
                if top[1]:
                    out.append("}")
            out.append("} while (false)" if top == "bare" else "}")
            continue
        # strip trailing ';' (also on 'a; b;' lines), then turn remaining simple statements
        stmt = s
        if "StringBuilder" in stmt:
            raise SystemExit(f"{name}: unexpected StringBuilder use: {stmt}")
        stmt = stmt.replace("; ", "\n")
        stmt = re.sub(r";\s*$", "", stmt)
        out.append(stmt)
    if conditional:
        out[N_HEAD - 3] = (" * the vocabulary parity test.\n *\n"
                           " * Reproduced Lucene bug: the `among` entries guarded by " + ", ".join(sorted(conditional)) + " are never taken,\n"
                           " * because Lucene 3.6.2 calls those private methods reflectively and the resulting\n"
                           " * `IllegalAccessException` is read as \"condition false\". The conditions are `{ false }` here for that reason.")
    # re-indent from brace structure
    res = []
    depth = 1
    for l in out[N_HEAD:]:
        l = l.strip()
        if not l:
            continue
        if l.startswith("}") or l.startswith(")"):
            depth -= 1
        res.append("    " * depth + l)
        if l.endswith("{") or l.endswith("(") or l.endswith("{"):
            depth += 1
    assert depth == 1, (name, depth)
    return "\n".join(out[:N_HEAD] + res + ["}", ""])


def main():
    src, dst, header = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]).read_text()
    dst.mkdir(parents=True, exist_ok=True)
    for lang in LANGS:
        java = (src / "org/tartarus/snowball/ext" / f"{lang}Stemmer.java").read_text()
        (dst / f"{lang}Stemmer.kt").write_text(convert(lang, java, header))


main()
