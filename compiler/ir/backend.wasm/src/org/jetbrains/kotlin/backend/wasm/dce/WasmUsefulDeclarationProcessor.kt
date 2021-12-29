/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dce

import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmArrayAnnotation
import org.jetbrains.kotlin.ir.backend.js.dce.UsefulDeclarationProcessor
import org.jetbrains.kotlin.ir.backend.js.utils.erasedUpperBound
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass

internal class WasmUsefulDeclarationProcessor(override val context: WasmBackendContext, printReachabilityInfo: Boolean) :
    UsefulDeclarationProcessor(printReachabilityInfo) {

    override val bodyVisitor: BodyVisitorBase = object : BodyVisitorBase() {
        override fun <T> visitConst(expression: IrConst<T>) {
            if (expression.kind is IrConstKind.String) {
                context.wasmSymbols.stringGetLiteral.owner.enqueue("String literal intrinsic getter stringGetLiteral")
            }
        }

        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)

            when (expression.symbol) {
                context.wasmSymbols.boxIntrinsic -> {
                    val toType = expression.getTypeArgument(0)!!
                    val inlineClass = context.inlineClassesUtils.getInlinedClass(toType)!!
                    val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                    constructor.enqueue("constructor for boxIntrinsic")
                    return
                }
                context.wasmSymbols.wasmClassId,
                context.wasmSymbols.wasmInterfaceId,
                context.wasmSymbols.wasmRefCast -> {
                    expression.getTypeArgument(0)?.getClass()?.symbol?.owner?.enqueue("generic intrinsic ${expression.symbol.owner.name}")
                    return
                }
            }

            val isSuperCall = expression.superQualifierSymbol != null
            val function: IrFunction = expression.symbol.owner.realOverrideTarget
            if (function is IrSimpleFunction && function.isOverridable && !isSuperCall) {
                val klass = function.parentAsClass
                if (!klass.isInterface) {
                    context.wasmSymbols.getVirtualMethodId.owner.enqueue("getVirtualMethodId")
                    function.symbol.owner.enqueue("referenceFunctionType")
                } else {
                    klass.symbol.owner.enqueue("referenceInterfaceId")
                    context.wasmSymbols.getInterfaceImplId.owner.enqueue("getInterfaceImplId")
                    function.symbol.owner.enqueue("referenceInterfaceTable and referenceFunctionType")
                }
            }
        }
    }

    private fun IrType.needToEnqueueType(): Boolean = when (this) {
        context.builtIns.booleanType,
        context.builtIns.byteType,
        context.builtIns.shortType,
        context.builtIns.charType,
        context.builtIns.booleanType,
        context.builtIns.byteType,
        context.builtIns.shortType,
        context.builtIns.intType,
        context.builtIns.charType,
        context.builtIns.longType,
        context.builtIns.floatType,
        context.builtIns.doubleType,
        context.builtIns.nothingType,
        context.wasmSymbols.voidType -> false
        else -> this.erasedUpperBound?.isExternal != true &&
                context.inlineClassesUtils.getInlinedClass(this) == null &&
                !isBuiltInWasmRefType(this)
    }

    private fun IrType.enqueueType(info: String) {
        if (needToEnqueueType()) {
            (this.getRuntimeClass ?: context.wasmSymbols.any.owner).enqueue(info)
        }
    }

    override fun processClass(irClass: IrClass) {
        irClass.getWasmArrayAnnotation()?.type
            ?.enqueueType("array type for wasm array annotated")

        if (irClass.isInterface) {
            val metadata = InterfaceMetadata(irClass, context.irBuiltIns)
            for (method in metadata.methods) {
                method.function.enqueue("interface method")
            }
        } else {
            irClass.allFields(context.irBuiltIns).map {
                it.type.enqueueType("declaration fields types")
            }
        }
        super.processClass(irClass)
    }

    override fun isExported(declaration: IrDeclaration): Boolean =
        declaration.isJsExport() || declaration.isEffectivelyExternal()
}