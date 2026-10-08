package org.crosswire.ksword.parity

import java.io.Serializable
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * The Java-visible shape of a class as a set of canonical lines (KSword K1 spec §4.1). Two classes with
 * equal sets are interchangeable for Java callers and Java/Kotlin subclasses. Only public and protected
 * members are listed, plus the serialization members of `Serializable` classes regardless of visibility.
 */
object ApiShape {
    private val SERIAL_METHODS = setOf("readObject", "writeObject", "readResolve", "writeReplace", "readObjectNoData")

    private fun visible(mods: Int) = Modifier.isPublic(mods) || Modifier.isProtected(mods)

    private fun vis(mods: Int) = if (Modifier.isPublic(mods)) "public" else if (Modifier.isProtected(mods)) "protected" else "private-or-package"

    /** True for a class Java code outside its package can see: public/protected all the way out, not synthetic, anonymous or local. */
    fun isCompared(c: Class<*>): Boolean {
        if (c.isSynthetic || c.isAnonymousClass || c.isLocalClass) return false
        var k: Class<*>? = c
        while (k != null) {
            if (!visible(k.modifiers)) return false
            k = k.declaringClass
        }
        return true
    }

    private fun kind(c: Class<*>) = when {
        c.isAnnotation -> "annotation"
        c.isInterface -> "interface"
        c.isEnum -> "enum"
        else -> "class"
    }

    private fun params(e: Executable) = e.parameterTypes.joinToString(",") { it.typeName }

    private fun throwsOf(e: Executable) = e.exceptionTypes.map { it.name }.sorted().joinToString(",")

    fun of(c: Class<*>): Set<String> {
        val out = sortedSetOf<String>()
        val classFinal = Modifier.isFinal(c.modifiers) && !c.isEnum
        val mods = buildList {
            add(vis(c.modifiers))
            if (!c.isInterface && !c.isEnum && Modifier.isAbstract(c.modifiers)) add("abstract")
            if (classFinal) add("final")
            if (c.declaringClass != null && Modifier.isStatic(c.modifiers)) add("static")
        }.joinToString(" ")
        // Kotlin adds marker interfaces to implementers of mapped collection types; no Java-visible members.
        val supers = c.interfaces.map { it.name }.filter { !it.startsWith("kotlin.jvm.internal.markers.") }.sorted().joinToString(",")
        out += "C ${kind(c)} $mods extends ${c.superclass?.name} implements [$supers]"

        val serializable = Serializable::class.java.isAssignableFrom(c)
        for (f in c.declaredFields) {
            if (f.isSynthetic) continue
            val m = f.modifiers
            // Enums serialize by constant name; their fields are never part of the serialized form.
            val serialField = serializable && !c.isEnum && (f.name == "serialVersionUID" ||
                (!Modifier.isStatic(m) && !Modifier.isTransient(m)))
            if (!visible(m) && !serialField) continue
            val flags = listOfNotNull(vis(m), "static".takeIf { Modifier.isStatic(m) },
                "final".takeIf { Modifier.isFinal(m) }, "transient".takeIf { Modifier.isTransient(m) }).joinToString(" ")
            out += "F $flags ${f.name}: ${f.type.typeName}${constantValue(f)}"
        }
        for (k in c.declaredConstructors) {
            if (k.isSynthetic || !visible(k.modifiers)) continue
            out += "K ${vis(k.modifiers)}${if (k.isVarArgs) " varargs" else ""} (${params(k)}) throws [${throwsOf(k)}]"
        }
        for (m in c.declaredMethods) {
            if (m.isSynthetic || m.isBridge) continue
            val mm = m.modifiers
            val serialMethod = serializable && m.name in SERIAL_METHODS
            if (!visible(mm) && !serialMethod) continue
            val static = Modifier.isStatic(mm)
            // Enum methods: Java leaves them non-final, Kotlin makes them final; only constant bodies could override, so ignore.
            val final = !static && !c.isEnum && (Modifier.isFinal(mm) || classFinal)
            val flags = listOfNotNull(vis(mm), "static".takeIf { static }, "final".takeIf { final },
                "abstract".takeIf { Modifier.isAbstract(mm) }, "default".takeIf { m.isDefault },
                "varargs".takeIf { m.isVarArgs }).joinToString(" ")
            out += "M $flags ${m.name}(${params(m)}): ${m.returnType.typeName} throws [${throwsOf(m)}]"
        }
        return out
    }

    /** Value of a static final primitive or String field; Java inlines these into callers. */
    private fun constantValue(f: Field): String {
        val m = f.modifiers
        if (!Modifier.isStatic(m) || !Modifier.isFinal(m)) return ""
        if (!f.type.isPrimitive && f.type != String::class.java) return ""
        return try {
            f.isAccessible = true
            " = " + f.get(null)
        } catch (e: Throwable) {
            " = !" + (generateSequence(e) { it.cause }.last()::class.java.name)
        }
    }

    /**
     * Additions that are never a Java-API change (spec §4.1): the companion field, enum `getEntries`, and
     * `internal` members, which Kotlin compiles to public members with a `$`-mangled name.
     * [line] is an `of`-format line ("F ..." or "M ...").
     */
    fun isKotlinArtefact(c: Class<*>, line: String): Boolean =
        (line.startsWith("F public static final Companion: ") && line.endsWith("\$Companion")) ||
            (c.isEnum && line.startsWith("M public static getEntries(): kotlin.enums.EnumEntries")) ||
            (line.startsWith("M ") && line.substringBefore('(').substringAfterLast(' ').contains('$'))

    private fun typeParams(tp: Array<out java.lang.reflect.TypeVariable<*>>) =
        if (tp.isEmpty()) "" else tp.joinToString(",", "<", "> ") { v -> v.name + " extends " + v.bounds.joinToString("&") { it.typeName } }

    /**
     * Generic signatures of the compared members; differences are reported, not failed (spec §4.1).
     * Uses the same normalisation as [of]: no `synchronized`, no `final` on static methods, Kotlin
     * artefacts (Companion, getEntries, `$`-mangled internals, marker interfaces) dropped.
     */
    fun genericOf(c: Class<*>): Set<String> {
        val out = sortedSetOf<String>()
        val supers = c.genericInterfaces.filter { !it.typeName.startsWith("kotlin.jvm.internal.markers.") }
        out += "C " + c.toGenericString().replace(" final ", " ") + " : " + c.genericSuperclass + " " + supers.joinToString(",")
        for (f in c.declaredFields) {
            if (f.isSynthetic || !visible(f.modifiers)) continue
            if (isKotlinArtefact(c, "F ${vis(f.modifiers)} static final ${f.name}: ${f.type.typeName}")) continue
            out += "F " + f.toGenericString()
        }
        for (k in c.declaredConstructors) {
            if (k.isSynthetic || !visible(k.modifiers)) continue
            out += "K " + k.toGenericString()
        }
        for (m in c.declaredMethods) {
            if (m.isSynthetic || m.isBridge || !visible(m.modifiers)) continue
            if (isKotlinArtefact(c, "M ${vis(m.modifiers)} static ${m.name}(): ${m.returnType.typeName}")) continue
            if (m.name.contains('$')) continue
            val mm = m.modifiers
            val static = Modifier.isStatic(mm)
            val final = !static && !c.isEnum && Modifier.isFinal(mm)
            val flags = listOfNotNull(vis(mm), "static".takeIf { static }, "final".takeIf { final },
                "abstract".takeIf { Modifier.isAbstract(mm) }).joinToString(" ")
            out += "M $flags ${typeParams(m.typeParameters)}${m.genericReturnType.typeName} ${m.name}" +
                "(${m.genericParameterTypes.joinToString(",") { it.typeName }}) throws [${m.genericExceptionTypes.joinToString(",") { it.typeName }}]"
        }
        return out
    }
}
