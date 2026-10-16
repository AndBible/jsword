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
 * Lucene 3.6.2 `org.tartarus.snowball.ext.DanishStemmer` (Snowball-generated), converted to Kotlin mechanically
 * by `jsword/scripts/snowball-j2k.py`. Behaviour is that of the Java original; compare with the baseline via
 * the vocabulary parity test.
 */
internal class DanishStemmer : SnowballStemmer() {
    private val a_0: Array<Among> = arrayOf(
        Among("hed", -1, 1),
        Among("ethed", 0, 1),
        Among("ered", -1, 1),
        Among("e", -1, 1),
        Among("erede", 3, 1),
        Among("ende", 3, 1),
        Among("erende", 5, 1),
        Among("ene", 3, 1),
        Among("erne", 3, 1),
        Among("ere", 3, 1),
        Among("en", -1, 1),
        Among("heden", 10, 1),
        Among("eren", 10, 1),
        Among("er", -1, 1),
        Among("heder", 13, 1),
        Among("erer", 13, 1),
        Among("s", -1, 2),
        Among("heds", 16, 1),
        Among("es", 16, 1),
        Among("endes", 18, 1),
        Among("erendes", 19, 1),
        Among("enes", 18, 1),
        Among("ernes", 18, 1),
        Among("eres", 18, 1),
        Among("ens", 16, 1),
        Among("hedens", 24, 1),
        Among("erens", 24, 1),
        Among("ers", 16, 1),
        Among("ets", 16, 1),
        Among("erets", 28, 1),
        Among("et", -1, 1),
        Among("eret", 30, 1)
    )
    private val a_1: Array<Among> = arrayOf(
        Among("gd", -1, -1),
        Among("dt", -1, -1),
        Among("gt", -1, -1),
        Among("kt", -1, -1)
    )
    private val a_2: Array<Among> = arrayOf(
        Among("ig", -1, 1),
        Among("lig", 0, 1),
        Among("elig", 1, 1),
        Among("els", -1, 1),
        Among("l\u00F8st", -1, 2)
    )
    private val g_v = intArrayOf(17, 65, 16, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 0, 128)
    private val g_s_ending = intArrayOf(239, 254, 42, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16)
    private var I_x = 0
    private var I_p1 = 0
    private var S_ch = StringBuilder()
    private fun copy_from(other: DanishStemmer) {
        I_x = other.I_x
        I_p1 = other.I_p1
        S_ch = other.S_ch
        super.copy_from(other)
    }
    private fun r_mark_regions(): Boolean {
        var v_1 = 0
        var v_2 = 0
        // (, line 29
        I_p1 = limit
        // test, line 33
        v_1 = cursor
        // (, line 33
        // hop, line 33
        do {
            var c = cursor + 3
            if (0 > c || c > limit)
            {
                return false
            }
            cursor = c
        } while (false)
        // setmark x, line 33
        I_x = cursor
        cursor = v_1
        // goto, line 34
        golab0@ while (true)
        {
            v_2 = cursor
            lab1@ do {
                if (!(in_grouping(g_v, 97, 248)))
                {
                    break@lab1
                }
                cursor = v_2
                break@golab0
            } while (false)
            cursor = v_2
            if (cursor >= limit)
            {
                return false
            }
            cursor++
        }
        // gopast, line 34
        golab2@ while (true)
        {
            lab3@ do {
                if (!(out_grouping(g_v, 97, 248)))
                {
                    break@lab3
                }
                break@golab2
            } while (false)
            if (cursor >= limit)
            {
                return false
            }
            cursor++
        }
        // setmark p1, line 34
        I_p1 = cursor
        // try, line 35
        lab4@ do {
            // (, line 35
            if (!(I_p1 < I_x))
            {
                break@lab4
            }
            I_p1 = I_x
        } while (false)
        return true
    }
    private fun r_main_suffix(): Boolean {
        var among_var = 0
        var v_1 = 0
        var v_2 = 0
        // (, line 40
        // setlimit, line 41
        v_1 = limit - cursor
        // tomark, line 41
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_2 = limit_backward
        limit_backward = cursor
        cursor = limit - v_1
        // (, line 41
        // [, line 41
        ket = cursor
        // substring, line 41
        among_var = find_among_b(a_0, 32)
        if (among_var == 0)
        {
            limit_backward = v_2
            return false
        }
        // ], line 41
        bra = cursor
        limit_backward = v_2
        when (among_var) {
            0 -> {
                return false
            }
            1 -> {
                // (, line 48
                // delete, line 48
                slice_del()
            }
            2 -> {
                // (, line 50
                if (!(in_grouping_b(g_s_ending, 97, 229)))
                {
                    return false
                }
                // delete, line 50
                slice_del()
            }
        }
        return true
    }
    private fun r_consonant_pair(): Boolean {
        var v_1 = 0
        var v_2 = 0
        var v_3 = 0
        // (, line 54
        // test, line 55
        v_1 = limit - cursor
        // (, line 55
        // setlimit, line 56
        v_2 = limit - cursor
        // tomark, line 56
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_3 = limit_backward
        limit_backward = cursor
        cursor = limit - v_2
        // (, line 56
        // [, line 56
        ket = cursor
        // substring, line 56
        if (find_among_b(a_1, 4) == 0)
        {
            limit_backward = v_3
            return false
        }
        // ], line 56
        bra = cursor
        limit_backward = v_3
        cursor = limit - v_1
        // next, line 62
        if (cursor <= limit_backward)
        {
            return false
        }
        cursor--
        // ], line 62
        bra = cursor
        // delete, line 62
        slice_del()
        return true
    }
    private fun r_other_suffix(): Boolean {
        var among_var = 0
        var v_1 = 0
        var v_2 = 0
        var v_3 = 0
        var v_4 = 0
        // (, line 65
        // do, line 66
        v_1 = limit - cursor
        lab0@ do {
            // (, line 66
            // [, line 66
            ket = cursor
            // literal, line 66
            if (!(eq_s_b(2, "st")))
            {
                break@lab0
            }
            // ], line 66
            bra = cursor
            // literal, line 66
            if (!(eq_s_b(2, "ig")))
            {
                break@lab0
            }
            // delete, line 66
            slice_del()
        } while (false)
        cursor = limit - v_1
        // setlimit, line 67
        v_2 = limit - cursor
        // tomark, line 67
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_3 = limit_backward
        limit_backward = cursor
        cursor = limit - v_2
        // (, line 67
        // [, line 67
        ket = cursor
        // substring, line 67
        among_var = find_among_b(a_2, 5)
        if (among_var == 0)
        {
            limit_backward = v_3
            return false
        }
        // ], line 67
        bra = cursor
        limit_backward = v_3
        when (among_var) {
            0 -> {
                return false
            }
            1 -> {
                // (, line 70
                // delete, line 70
                slice_del()
                // do, line 70
                v_4 = limit - cursor
                lab1@ do {
                    // call consonant_pair, line 70
                    if (!r_consonant_pair())
                    {
                        break@lab1
                    }
                } while (false)
                cursor = limit - v_4
            }
            2 -> {
                // (, line 72
                // <-, line 72
                slice_from("l\u00F8s")
            }
        }
        return true
    }
    private fun r_undouble(): Boolean {
        var v_1 = 0
        var v_2 = 0
        // (, line 75
        // setlimit, line 76
        v_1 = limit - cursor
        // tomark, line 76
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_2 = limit_backward
        limit_backward = cursor
        cursor = limit - v_1
        // (, line 76
        // [, line 76
        ket = cursor
        if (!(out_grouping_b(g_v, 97, 248)))
        {
            limit_backward = v_2
            return false
        }
        // ], line 76
        bra = cursor
        // -> ch, line 76
        S_ch = slice_to(S_ch)
        limit_backward = v_2
        // name ch, line 77
        if (!(eq_v_b(S_ch)))
        {
            return false
        }
        // delete, line 78
        slice_del()
        return true
    }
    override fun stem(): Boolean {
        var v_1 = 0
        var v_2 = 0
        var v_3 = 0
        var v_4 = 0
        var v_5 = 0
        // (, line 82
        // do, line 84
        v_1 = cursor
        lab0@ do {
            // call mark_regions, line 84
            if (!r_mark_regions())
            {
                break@lab0
            }
        } while (false)
        cursor = v_1
        // backwards, line 85
        limit_backward = cursor
cursor = limit
        // (, line 85
        // do, line 86
        v_2 = limit - cursor
        lab1@ do {
            // call main_suffix, line 86
            if (!r_main_suffix())
            {
                break@lab1
            }
        } while (false)
        cursor = limit - v_2
        // do, line 87
        v_3 = limit - cursor
        lab2@ do {
            // call consonant_pair, line 87
            if (!r_consonant_pair())
            {
                break@lab2
            }
        } while (false)
        cursor = limit - v_3
        // do, line 88
        v_4 = limit - cursor
        lab3@ do {
            // call other_suffix, line 88
            if (!r_other_suffix())
            {
                break@lab3
            }
        } while (false)
        cursor = limit - v_4
        // do, line 89
        v_5 = limit - cursor
        lab4@ do {
            // call undouble, line 89
            if (!r_undouble())
            {
                break@lab4
            }
        } while (false)
        cursor = limit - v_5
        cursor = limit_backward
           return true
    }
}
