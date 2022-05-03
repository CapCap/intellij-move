package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.FQModule
import org.move.lang.moveProject
import javax.swing.Icon

fun List<MvAttr>.findSingleItemAttr(name: String): MvAttr? =
    this.find {
        it.attrItemList.size == 1
                && it.attrItemList.first().identifier.text == name
    }

val MvModule.isTestOnly: Boolean get() = this.attrList.findSingleItemAttr("test_only") != null

fun MvModule.address(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

fun MvModule.fqModule(): FQModule? {
    val address = this.address()?.toAddress() ?: return null
//    val address = this.containingAddress ?: return null
    val name = this.name ?: return null
    return FQModule(address, name)
}

val MvModule.fqName: String?
    get() {
        val address = this.address()?.text?.let { "$it::" } ?: ""
        val module = this.name ?: return null
        return address + module
    }

val MvModule.friendModules: Set<FQModule>
    get() {
        val block = this.moduleBlock ?: return emptySet()
        val moduleRefs = block.friendStmtList.mapNotNull { it.fqModuleRef }

        val friends = mutableSetOf<FQModule>()
        for (moduleRef in moduleRefs) {
            val proj = moduleRef.moveProject ?: continue
            val address = moduleRef.addressRef.toAddress(proj) ?: continue
            val identifier = moduleRef.identifier?.text ?: continue
            friends.add(FQModule(address, identifier))
        }
        return friends
    }

fun MvModule.allFunctions() = moduleBlock?.functionList.orEmpty()

fun MvModule.builtinFunctions(): List<MvFunction> {
    return listOf(
        builtinFunction(
            """
            /// Removes `T` from address and returns it. 
            /// Aborts if address does not hold a `T`.
            native fun move_from<T: key>(addr: address): T acquires T;
            """, project
        ),
        builtinFunction(
            """
            /// Publishes `T` under `signer.address`. 
            /// Aborts if `signer.address` already holds a `T`.
            native fun move_to<T: key>(acc: &signer, res: T);
            """, project
        ),
        builtinFunction("native fun borrow_global<T: key>(addr: address): &T acquires T;", project),
        builtinFunction(
            "native fun borrow_global_mut<T: key>(addr: address): &mut T acquires T;",
            project
        ),
        builtinFunction(
            """
            /// Returns `true` if a `T` is stored under address
            native fun exists<T: key>(addr: address): bool;
            """, project
        ),
        builtinFunction("native fun freeze<S>(mut_ref: &mut S): &S;", project),
    )
}

fun MvModule.functions(visibility: Visibility): List<MvFunction> =
    when (visibility) {
        is Visibility.Public ->
            allFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC }
        is Visibility.PublicScript ->
            allFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC_SCRIPT }
        is Visibility.PublicFriend -> {
            if (visibility.currentModule in this.friendModules) {
                allFunctions().filter { it.visibility == FunctionVisibility.PUBLIC_FRIEND }
            } else {
                emptyList()
            }
        }
        is Visibility.Internal -> allFunctions()
    }

fun builtinFunction(text: String, project: Project): MvFunction {
    val trimmedText = text.trimIndent()
    val function = project.psiFactory.function(trimmedText, moduleName = "builtin_functions")
    (function as MvFunctionMixin).builtIn = true
    return function
}

fun builtinSpecFunction(text: String, project: Project): MvSpecFunction {
    val trimmedText = text.trimIndent()
    return project.psiFactory.specFunction(trimmedText, moduleName = "builtin_spec_functions")
}

fun MvModule.structs(): List<MvStruct> = moduleBlock?.structList.orEmpty()

fun MvModule.schemas(): List<MvSchema> = moduleBlock?.schemaList.orEmpty()

fun MvModule.builtinSpecFunctions(): List<MvSpecFunction> {
    return CachedValuesManager.getCachedValue(this) {
        val funcs = listOf(
            builtinSpecFunction("spec native fun max_u8(): num;", project),
            builtinSpecFunction("spec native fun max_u64(): num;", project),
            builtinSpecFunction("spec native fun max_u128(): num;", project),
            builtinSpecFunction("spec native fun global<T: key>(addr: address): T;", project),
            builtinSpecFunction("spec native fun old<T>(_: T): T;", project),
            builtinSpecFunction(
                "spec native fun update_field<S, F, V>(s: S, fname: F, val: V): S;",
                project
            ),
            builtinSpecFunction("spec native fun TRACE<T>(_: T): T;", project),
            // vector functions
            builtinSpecFunction("spec native fun len<T>(_: vector<T>): num;", project),
            builtinSpecFunction(
                "spec native fun concat<T>(v1: vector<T>, v2: vector<T>): vector<T>;",
                project
            ),
            builtinSpecFunction("spec native fun contains<T>(v: vector<T>, e: T): bool;", project),
            builtinSpecFunction("spec native fun index_of<T>(_: vector<T>, _: T): num;", project),
            builtinSpecFunction("spec native fun range<T>(_: vector<T>): range;", project),
            builtinSpecFunction("spec native fun in_range<T>(_: vector<T>, _: num): bool;", project),
        )
        CachedValueProvider.Result(funcs, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

fun MvModule.specFunctions(): List<MvSpecFunction> = moduleBlock?.specFunctionList.orEmpty()

fun MvModule.constBindings(): List<MvBindingPat> =
    moduleBlock?.constList.orEmpty().mapNotNull { it.bindingPat }

fun MvModule.moduleSpecs() =
    this.moduleBlock?.childrenOfType<MvModuleSpec>().orEmpty()

abstract class MvModuleMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                              MvModule {
    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
        val locationString = this.address()?.toAddress()?.text() ?: ""
        return PresentationData(
            name,
            locationString,
            MoveIcons.MODULE,
            null
        )
    }

//    override val useStmts: List<MvUseStmt>
//        get() =
//            moduleBlock?.useStmtList.orEmpty()
}
