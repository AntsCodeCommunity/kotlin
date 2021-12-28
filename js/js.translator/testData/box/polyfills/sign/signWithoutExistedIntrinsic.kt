// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
if (!isLegacyBackend()) {
    Math.sign = undefined;
}

// FILE: main.kt
import kotlin.math.sign

fun box(): String {
    val result = 44.0.sign

    assertEquals(result, 1)
    assertEquals(js("Math.sign.called"), js("undefined"))

    return "OK"
}
