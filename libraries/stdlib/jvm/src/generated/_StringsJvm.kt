/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

//
// NOTE: THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//


/**
 * Returns a character at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this char sequence.
 * 
 * @sample samples.collections.Collections.Elements.elementAt
 */
@kotlin.internal.InlineOnly
public actual inline fun CharSequence.elementAt(index: Int): Char {
    return get(index)
}

/**
 * Returns a new [SortedSet][java.util.SortedSet] of all characters.
 */
public fun CharSequence.toSortedSet(): java.util.SortedSet<Char> {
    return toCollection(java.util.TreeSet<Char>())
}

/**
 * Returns the sum of all values produced by [selector] function applied to each character in the char sequence.
 */
@SinceKotlin("1.4")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("sumOfBigDecimal")
@kotlin.internal.InlineOnly
public inline fun CharSequence.sumOf(selector: (Char) -> java.math.BigDecimal): java.math.BigDecimal {
    var sum: java.math.BigDecimal = 0.toBigDecimal()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each character in the char sequence.
 */
@SinceKotlin("1.4")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("sumOfBigInteger")
@kotlin.internal.InlineOnly
public inline fun CharSequence.sumOf(selector: (Char) -> java.math.BigInteger): java.math.BigInteger {
    var sum: java.math.BigInteger = 0.toBigInteger()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

