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

private val a_0: Array<Among> = arrayOf(
    Among("a", -1, 1),
    Among("e", -1, 1),
    Among("ede", 1, 1),
    Among("ande", 1, 1),
    Among("ende", 1, 1),
    Among("ane", 1, 1),
    Among("ene", 1, 1),
    Among("hetene", 6, 1),
    Among("erte", 1, 3),
    Among("en", -1, 1),
    Among("heten", 9, 1),
    Among("ar", -1, 1),
    Among("er", -1, 1),
    Among("heter", 12, 1),
    Among("s", -1, 2),
    Among("as", 14, 1),
    Among("es", 14, 1),
    Among("edes", 16, 1),
    Among("endes", 16, 1),
    Among("enes", 16, 1),
    Among("hetenes", 19, 1),
    Among("ens", 14, 1),
    Among("hetens", 21, 1),
    Among("ers", 14, 1),
    Among("ets", 14, 1),
    Among("et", -1, 1),
    Among("het", 25, 1),
    Among("ert", -1, 3),
    Among("ast", -1, 1)
)
private val a_1: Array<Among> = arrayOf(
    Among("dt", -1, -1),
    Among("vt", -1, -1)
)
private val a_2: Array<Among> = arrayOf(
    Among("leg", -1, 1),
    Among("eleg", 0, 1),
    Among("ig", -1, 1),
    Among("eig", 2, 1),
    Among("lig", 2, 1),
    Among("elig", 4, 1),
    Among("els", -1, 1),
    Among("lov", -1, 1),
    Among("elov", 7, 1),
    Among("slov", 7, 1),
    Among("hetslov", 9, 1)
)
private val g_v = intArrayOf(17, 65, 16, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 0, 128)
private val g_s_ending = intArrayOf(119, 125, 149, 1)

/**
 * Lucene 3.6.2 `org.tartarus.snowball.ext.NorwegianStemmer` (Snowball-generated), converted to Kotlin mechanically
 * by `jsword/scripts/snowball-j2k.py`. Behaviour is that of the Java original; compare with the baseline via
 * the vocabulary parity test.
 */
internal class NorwegianStemmer : SnowballStemmer() {
    private var I_x = 0
    private var I_p1 = 0
    private fun copy_from(other: NorwegianStemmer) {
        I_x = other.I_x
        I_p1 = other.I_p1
        super.copy_from(other)
    }
    private fun r_mark_regions(): Boolean {
        var v_1 = 0
        var v_2 = 0
        // (, line 26
        I_p1 = limit
        // test, line 30
        v_1 = cursor
        // (, line 30
        // hop, line 30
        do {
            var c = cursor + 3
            if (0 > c || c > limit)
            {
                return false
            }
            cursor = c
        } while (false)
        // setmark x, line 30
        I_x = cursor
        cursor = v_1
        // goto, line 31
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
        // gopast, line 31
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
        // setmark p1, line 31
        I_p1 = cursor
        // try, line 32
        lab4@ do {
            // (, line 32
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
        var v_3 = 0
        // (, line 37
        // setlimit, line 38
        v_1 = limit - cursor
        // tomark, line 38
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_2 = limit_backward
        limit_backward = cursor
        cursor = limit - v_1
        // (, line 38
        // [, line 38
        ket = cursor
        // substring, line 38
        among_var = find_among_b(a_0, 29)
        if (among_var == 0)
        {
            limit_backward = v_2
            return false
        }
        // ], line 38
        bra = cursor
        limit_backward = v_2
        when (among_var) {
            0 -> {
                return false
            }
            1 -> {
                // (, line 44
                // delete, line 44
                slice_del()
            }
            2 -> {
                // (, line 46
                // or, line 46
                lab0@ do {
                    v_3 = limit - cursor
                    lab1@ do {
                        if (!(in_grouping_b(g_s_ending, 98, 122)))
                        {
                            break@lab1
                        }
                        break@lab0
                    } while (false)
                    cursor = limit - v_3
                    // (, line 46
                    // literal, line 46
                    if (!(eq_s_b(1, "k")))
                    {
                        return false
                    }
                    if (!(out_grouping_b(g_v, 97, 248)))
                    {
                        return false
                    }
                } while (false)
                // delete, line 46
                slice_del()
            }
            3 -> {
                // (, line 48
                // <-, line 48
                slice_from("er")
            }
        }
        return true
    }
    private fun r_consonant_pair(): Boolean {
        var v_1 = 0
        var v_2 = 0
        var v_3 = 0
        // (, line 52
        // test, line 53
        v_1 = limit - cursor
        // (, line 53
        // setlimit, line 54
        v_2 = limit - cursor
        // tomark, line 54
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_3 = limit_backward
        limit_backward = cursor
        cursor = limit - v_2
        // (, line 54
        // [, line 54
        ket = cursor
        // substring, line 54
        if (find_among_b(a_1, 2) == 0)
        {
            limit_backward = v_3
            return false
        }
        // ], line 54
        bra = cursor
        limit_backward = v_3
        cursor = limit - v_1
        // next, line 59
        if (cursor <= limit_backward)
        {
            return false
        }
        cursor--
        // ], line 59
        bra = cursor
        // delete, line 59
        slice_del()
        return true
    }
    private fun r_other_suffix(): Boolean {
        var among_var = 0
        var v_1 = 0
        var v_2 = 0
        // (, line 62
        // setlimit, line 63
        v_1 = limit - cursor
        // tomark, line 63
        if (cursor < I_p1)
        {
            return false
        }
        cursor = I_p1
        v_2 = limit_backward
        limit_backward = cursor
        cursor = limit - v_1
        // (, line 63
        // [, line 63
        ket = cursor
        // substring, line 63
        among_var = find_among_b(a_2, 11)
        if (among_var == 0)
        {
            limit_backward = v_2
            return false
        }
        // ], line 63
        bra = cursor
        limit_backward = v_2
        when (among_var) {
            0 -> {
                return false
            }
            1 -> {
                // (, line 67
                // delete, line 67
                slice_del()
            }
        }
        return true
    }
    override fun stem(): Boolean {
        var v_1 = 0
        var v_2 = 0
        var v_3 = 0
        var v_4 = 0
        // (, line 72
        // do, line 74
        v_1 = cursor
        lab0@ do {
            // call mark_regions, line 74
            if (!r_mark_regions())
            {
                break@lab0
            }
        } while (false)
        cursor = v_1
        // backwards, line 75
        limit_backward = cursor
cursor = limit
        // (, line 75
        // do, line 76
        v_2 = limit - cursor
        lab1@ do {
            // call main_suffix, line 76
            if (!r_main_suffix())
            {
                break@lab1
            }
        } while (false)
        cursor = limit - v_2
        // do, line 77
        v_3 = limit - cursor
        lab2@ do {
            // call consonant_pair, line 77
            if (!r_consonant_pair())
            {
                break@lab2
            }
        } while (false)
        cursor = limit - v_3
        // do, line 78
        v_4 = limit - cursor
        lab3@ do {
            // call other_suffix, line 78
            if (!r_other_suffix())
            {
                break@lab3
            }
        } while (false)
        cursor = limit - v_4
        cursor = limit_backward
           return true
    }
}
