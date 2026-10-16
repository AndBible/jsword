/*

Copyright (c) 2001, Dr Martin Porter
Copyright (c) 2002, Richard Boulton
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
    * notice, this list of conditions and the following disclaimer in the
    * documentation and/or other materials provided with the distribution.
    * Neither the name of the copyright holders nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.crosswire.jsword.index.analysis.snowball

/**
 * Lucene 3.6.2 `org.tartarus.snowball.SnowballProgram` (Snowball SVN rev 502 as modified by Lucene), ported to Kotlin.
 *
 * Differences from the Java original: the reflective `Among` condition is a lambda, the buffer growth uses plain
 * doubling instead of `ArrayUtil.oversize` (capacity never affects results; only [limit] does), and a program is
 * driven through [SnowballStemmer.stem] with a `String`. The `char` grouping tables are [IntArray]s of bytes.
 *
 * Not thread-safe: every program keeps the word it is working on in its fields.
 */
internal abstract class SnowballProgram internal constructor() {
    private var current: CharArray = CharArray(8)

    protected var cursor: Int = 0
    protected var limit: Int = 0
    protected var limit_backward: Int = 0
    protected var bra: Int = 0
    protected var ket: Int = 0

    /** Runs the stemmer on the current string; the result is read with [getCurrent]. */
    abstract fun stem(): Boolean

    init {
        setCurrent("")
    }

    fun setCurrent(value: String) {
        current = value.toCharArray()
        cursor = 0
        limit = value.length
        limit_backward = 0
        bra = cursor
        ket = limit
    }

    fun getCurrent(): String = current.concatToString(0, limit)

    protected fun copy_from(other: SnowballProgram) {
        current = other.current
        cursor = other.cursor
        limit = other.limit
        limit_backward = other.limit_backward
        bra = other.bra
        ket = other.ket
    }

    protected fun in_grouping(s: IntArray, min: Int, max: Int): Boolean {
        if (cursor >= limit) return false
        var ch = current[cursor].code
        if (ch > max || ch < min) return false
        ch -= min
        if ((s[ch shr 3] and (0x1 shl (ch and 0x7))) == 0) return false
        cursor++
        return true
    }

    protected fun in_grouping_b(s: IntArray, min: Int, max: Int): Boolean {
        if (cursor <= limit_backward) return false
        var ch = current[cursor - 1].code
        if (ch > max || ch < min) return false
        ch -= min
        if ((s[ch shr 3] and (0x1 shl (ch and 0x7))) == 0) return false
        cursor--
        return true
    }

    protected fun out_grouping(s: IntArray, min: Int, max: Int): Boolean {
        if (cursor >= limit) return false
        var ch = current[cursor].code
        if (ch > max || ch < min) {
            cursor++
            return true
        }
        ch -= min
        if ((s[ch shr 3] and (0x1 shl (ch and 0x7))) == 0) {
            cursor++
            return true
        }
        return false
    }

    protected fun out_grouping_b(s: IntArray, min: Int, max: Int): Boolean {
        if (cursor <= limit_backward) return false
        var ch = current[cursor - 1].code
        if (ch > max || ch < min) {
            cursor--
            return true
        }
        ch -= min
        if ((s[ch shr 3] and (0x1 shl (ch and 0x7))) == 0) {
            cursor--
            return true
        }
        return false
    }

    protected fun in_range(min: Int, max: Int): Boolean {
        if (cursor >= limit) return false
        val ch = current[cursor].code
        if (ch > max || ch < min) return false
        cursor++
        return true
    }

    protected fun in_range_b(min: Int, max: Int): Boolean {
        if (cursor <= limit_backward) return false
        val ch = current[cursor - 1].code
        if (ch > max || ch < min) return false
        cursor--
        return true
    }

    protected fun out_range(min: Int, max: Int): Boolean {
        if (cursor >= limit) return false
        val ch = current[cursor].code
        if (!(ch > max || ch < min)) return false
        cursor++
        return true
    }

    protected fun out_range_b(min: Int, max: Int): Boolean {
        if (cursor <= limit_backward) return false
        val ch = current[cursor - 1].code
        if (!(ch > max || ch < min)) return false
        cursor--
        return true
    }

    protected fun eq_s(s_size: Int, s: CharSequence): Boolean {
        if (limit - cursor < s_size) return false
        for (i in 0 until s_size) {
            if (current[cursor + i] != s[i]) return false
        }
        cursor += s_size
        return true
    }

    protected fun eq_s_b(s_size: Int, s: CharSequence): Boolean {
        if (cursor - limit_backward < s_size) return false
        for (i in 0 until s_size) {
            if (current[cursor - s_size + i] != s[i]) return false
        }
        cursor -= s_size
        return true
    }

    protected fun eq_v(s: CharSequence): Boolean = eq_s(s.length, s)

    protected fun eq_v_b(s: CharSequence): Boolean = eq_s_b(s.length, s)

    protected fun find_among(v: Array<Among>, v_size: Int): Int {
        var i = 0
        var j = v_size
        val c = cursor
        val l = limit
        var common_i = 0
        var common_j = 0
        var first_key_inspected = false

        while (true) {
            val k = i + ((j - i) shr 1)
            var diff = 0
            var common = if (common_i < common_j) common_i else common_j // smaller
            val w = v[k]
            var i2 = common
            while (i2 < w.s_size) {
                if (c + common == l) {
                    diff = -1
                    break
                }
                diff = current[c + common] - w.s[i2]
                if (diff != 0) break
                common++
                i2++
            }
            if (diff < 0) {
                j = k
                common_j = common
            } else {
                i = k
                common_i = common
            }
            if (j - i <= 1) {
                if (i > 0) break // v->s has been inspected
                if (j == i) break // only one item in v
                // Go round once more to get v->s inspected.
                if (first_key_inspected) break
                first_key_inspected = true
            }
        }
        while (true) {
            val w = v[i]
            if (common_i >= w.s_size) {
                cursor = c + w.s_size
                val condition = w.condition ?: return w.result
                val res = condition(this)
                cursor = c + w.s_size
                if (res) return w.result
            }
            i = w.substring_i
            if (i < 0) return 0
        }
    }

    /** [find_among] for backwards processing. */
    protected fun find_among_b(v: Array<Among>, v_size: Int): Int {
        var i = 0
        var j = v_size
        val c = cursor
        val lb = limit_backward
        var common_i = 0
        var common_j = 0
        var first_key_inspected = false

        while (true) {
            val k = i + ((j - i) shr 1)
            var diff = 0
            var common = if (common_i < common_j) common_i else common_j
            val w = v[k]
            var i2 = w.s_size - 1 - common
            while (i2 >= 0) {
                if (c - common == lb) {
                    diff = -1
                    break
                }
                diff = current[c - 1 - common] - w.s[i2]
                if (diff != 0) break
                common++
                i2--
            }
            if (diff < 0) {
                j = k
                common_j = common
            } else {
                i = k
                common_i = common
            }
            if (j - i <= 1) {
                if (i > 0) break
                if (j == i) break
                if (first_key_inspected) break
                first_key_inspected = true
            }
        }
        while (true) {
            val w = v[i]
            if (common_i >= w.s_size) {
                cursor = c - w.s_size
                val condition = w.condition ?: return w.result
                val res = condition(this)
                cursor = c - w.s_size
                if (res) return w.result
            }
            i = w.substring_i
            if (i < 0) return 0
        }
    }

    /** Replaces the chars between [c_bra] and [c_ket] by [s]; returns the length change. */
    protected fun replace_s(c_bra: Int, c_ket: Int, s: CharSequence): Int {
        val adjustment = s.length - (c_ket - c_bra)
        val newLength = limit + adjustment
        if (newLength > current.size) {
            val newBuffer = CharArray(maxOf(newLength, current.size * 2))
            current.copyInto(newBuffer, 0, 0, limit)
            current = newBuffer
        }
        if (adjustment != 0 && c_ket < limit) {
            current.copyInto(current, c_bra + s.length, c_ket, limit)
        }
        for (i in 0 until s.length) current[c_bra + i] = s[i]
        limit += adjustment
        if (cursor >= c_ket) cursor += adjustment else if (cursor > c_bra) cursor = c_bra
        return adjustment
    }

    protected fun slice_check() {
        // The Java original only prints "faulty slice operation" here; a generated stemmer never triggers it.
    }

    protected fun slice_from(s: CharSequence) {
        slice_check()
        replace_s(bra, ket, s)
    }

    protected fun slice_del() {
        slice_from("")
    }

    protected fun insert(c_bra: Int, c_ket: Int, s: CharSequence) {
        val adjustment = replace_s(c_bra, c_ket, s)
        if (c_bra <= bra) bra += adjustment
        if (c_bra <= ket) ket += adjustment
    }

    /** Copies the slice between [bra] and [ket] into [s]. */
    protected fun slice_to(s: StringBuilder): StringBuilder {
        slice_check()
        s.clear()
        s.appendRange(current, bra, ket)
        return s
    }

    protected fun assign_to(s: StringBuilder): StringBuilder {
        s.clear()
        s.appendRange(current, 0, limit)
        return s
    }
}
