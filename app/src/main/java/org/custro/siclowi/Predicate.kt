/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.custro.siclowi

/**
 * A Predicate can determine a true or false value for any input of its
 * parameterized type. For example, a `RegexPredicate` might implement
 * `Predicate<String>`, and return true for any String that matches its
 * given regular expression.
 *
 * Implementors of Predicate which may cause side effects upon evaluation are
 * strongly encouraged to state this fact clearly in their API documentation.
 */
interface Predicate<T> {
    fun apply(t: T): Boolean

    companion object {
        /**
         * An implementation of the predicate interface that always returns true.
         */
        @JvmField
        val TRUE: Predicate<*> = object : Predicate<Any> {
            override fun apply(t: Any): Boolean = true
        }

        /**
         * An implementation of the predicate interface that always returns false.
         */
        @JvmField
        val FALSE: Predicate<*> = object : Predicate<Any> {
            override fun apply(t: Any): Boolean = false
        }
    }
}