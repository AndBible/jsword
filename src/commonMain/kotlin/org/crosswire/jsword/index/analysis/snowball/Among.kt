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
 * One entry of a Snowball `among` table (Lucene 3.6.2 `org.tartarus.snowball.Among`, ported to Kotlin).
 *
 * The Java original resolved the optional condition by reflection (method name plus target object). Here it is the
 * lambda [condition]; `null` means no condition. Lucene 3.6.2's reflective call always fails (the target methods are
 * private, so `Method.invoke` throws `IllegalAccessException`, which `find_among` treats as "condition false"); the
 * generated stemmers reproduce that with `{ false }`, which is what the baseline index was built with.
 */
class Among(
    s: String,
    /** Index of the longest matching proper prefix-suffix entry, or -1. */
    val substring_i: Int,
    /** Result of the lookup. */
    val result: Int,
    /** Condition to run when the entry matches; a false result makes the search continue with [substring_i]. */
    val condition: ((SnowballProgram) -> Boolean)? = null,
) {
    val s_size: Int = s.length
    val s: CharArray = s.toCharArray()
}
