/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.experimental.inv

private fun decodeFromUtf8(bytes: ByteArray) = String(bytes)
internal fun encodeToUtf8(str: String) = str.toByteArray()

internal fun CPointer<ByteVar>.toKStringFromUtf8Impl(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(nativeBytes.pointed, bytes, length)
    return decodeFromUtf8(bytes)
}

fun bitsToFloat(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)
fun bitsToDouble(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

// TODO: the functions below should eventually be intrinsified

inline fun <reified R : Number> Byte.signExtend(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Short.signExtend(): R = when (R::class.java) {
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Int.signExtend(): R = when (R::class.java) {
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Long.signExtend(): R = when (R::class.java) {
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Number.invalidSignExtension(): R {
    throw Error("unable to sign extend ${this.javaClass.simpleName} \"${this}\" to ${R::class.java.simpleName}")
}

inline fun <reified R : Number> Byte.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Short.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Int.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Long.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Number.invalidNarrowing(): R {
    throw Error("unable to narrow ${this.javaClass.simpleName} \"${this}\" to ${R::class.java.simpleName}")
}

// This is J2K of ClassLoader::initializePath.
private fun initializePath(): Array<String> {
    val ldpath = System.getProperty("java.library.path", "")
    val ps = File.pathSeparator
    val ldlen = ldpath.length
    var i: Int
    var j: Int
    var n: Int
    // Count the separators in the path
    i = ldpath.indexOf(ps)
    n = 0
    while (i >= 0) {
        n++
        i = ldpath.indexOf(ps, i + 1)
    }

    // allocate the array of paths - n :'s = n + 1 path elements
    val paths = Array(n + 1) { "" }

    // Fill the array with paths from the ldpath
    i = 0
    n = i
    j = ldpath.indexOf(ps)
    while (j >= 0) {
        if (j - i > 0) {
            paths[n++] = ldpath.substring(i, j)
        } else if (j - i == 0) {
            paths[n++] = "."
        }
        i = j + 1
        j = ldpath.indexOf(ps, i)
    }
    paths[n] = ldpath.substring(i, ldlen)
    return paths
}

private fun tryLoadKonanLibrary(libraryDir: String, fullLibraryName: String): Boolean {
    if (!Files.exists(Paths.get(libraryDir, fullLibraryName))) return false

//    val dir = if (fullLibraryName.contains("orgjetbrainskotlinbackendkonanenvstubs"))
//        return true
//        //libraryDir
//    else {
//        val tempDir = Files.createTempDirectory(null).toAbsolutePath().toString()
//        Files.copy(Paths.get(libraryDir, fullLibraryName), Paths.get(tempDir, fullLibraryName), StandardCopyOption.REPLACE_EXISTING)
//        // TODO: Does not work on Windows. May be use FILE_FLAG_DELETE_ON_CLOSE?
//        File(tempDir).deleteOnExit()
//        File("$tempDir/$fullLibraryName").deleteOnExit()
//        tempDir
//    }
    val dir = Files.createTempDirectory(null).toAbsolutePath().toString()
    Files.copy(Paths.get(libraryDir, fullLibraryName), Paths.get(dir, fullLibraryName), StandardCopyOption.REPLACE_EXISTING)
    // TODO: Does not work on Windows. May be use FILE_FLAG_DELETE_ON_CLOSE?
    File(dir).deleteOnExit()
    File("$dir/$fullLibraryName").deleteOnExit()
    println("QZZ: $dir/$fullLibraryName")
//    val libFile = File("$libraryDir/$fullLibraryName")
//    val bytes = libFile.readBytes()
//    bytes[0] = bytes[0].inv()
//    val dir = try {
//        libFile.writeBytes(bytes)
//        bytes[0] = bytes[0].inv()
//        libFile.writeBytes(bytes)
//        libraryDir
//    } catch (e: IOException) {
//        val tempDir = Files.createTempDirectory(null).toAbsolutePath().toString()
//        Files.copy(Paths.get(libraryDir, fullLibraryName), Paths.get(tempDir, fullLibraryName), StandardCopyOption.REPLACE_EXISTING)
//        // TODO: Does not work on Windows. May be use FILE_FLAG_DELETE_ON_CLOSE?
//        File(tempDir).deleteOnExit()
//        File("$tempDir/$fullLibraryName").deleteOnExit()
//        tempDir
//    }

//    val dir = if (File("$libraryDir/$fullLibraryName").canWrite())
//        libraryDir
//    else {
//    }
//    val dir = try {
//        System.getSecurityManager().checkWrite("$libraryDir/$fullLibraryName")
//        libraryDir
//    } catch (e: SecurityException) {
//    }

    try {
        System.load("$dir/$fullLibraryName")
    } catch (e: UnsatisfiedLinkError) {
        if (fullLibraryName.endsWith(".dylib") && e.message?.contains("library load disallowed by system policy") == true) {
            throw UnsatisfiedLinkError("""
                    |Library $libraryDir/$fullLibraryName can't be loaded.
                    |${'\t'}This can happen because library file is marked as untrusted (e.g because it was downloaded from browser).
                    |${'\t'}You can trust libraries in distribution by running
                    |${'\t'}${'\t'}xattr -d com.apple.quarantine '$libraryDir'/*
                    |${'\t'}command in terminal
                    |${'\t'}Original exception message:
                    |${'\t'}${e.message}
                    """.trimMargin())
        }
        throw e
    }

    return true
}

fun loadKonanLibrary(name: String) {
    val fullLibraryName = System.mapLibraryName(name)
    val paths = initializePath()
    for (dir in paths) {
        if (tryLoadKonanLibrary(dir, fullLibraryName)) return
    }
    val defaultNativeLibsDir = "${KonanHomeProvider.determineKonanHome()}/konan/nativelib"
    if (tryLoadKonanLibrary(defaultNativeLibsDir, fullLibraryName))
        return
    error("Lib $fullLibraryName is not found in $defaultNativeLibsDir and ${paths.joinToString { it }}")
}