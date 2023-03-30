package org.move.lang.core.psi

import com.intellij.psi.*
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.util.Query
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.completion.BUILTIN_ITEM_PRIORITY
import org.move.lang.core.completion.LOCAL_ITEM_PRIORITY
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.findLastChildByType
import org.move.lang.core.types.ItemFQName

interface MvNamedElement : MvElement,
                           PsiNamedElement,
                           NavigatablePsiElement {

    val nameElement: PsiElement? get() = this.findLastChildByType(IDENTIFIER)
}

interface MvMandatoryNamedElement : MvNamedElement {
    val identifier: PsiElement

    override val nameElement: PsiElement get() = identifier

    override fun getName(): String
}

interface MvQualifiedNamedElement : MvNamedElement {
    val fqName: ItemFQName
}

data class FqPath(val address: String, val module: String, val item: String?) {
    override fun toString(): String {
        return if (item == null) {
            "$address::$module"
        } else {
            "$address::$module::$item"
        }
    }
}

//val MvFQNamedElement.fqPath: FqPath?
//    get() {
//        return when (this) {
//            is MvModule -> {
//                val address = this.addressRef()?.text ?: return null
//                val moduleName = this.name ?: return null
//                FqPath(address, moduleName, null)
//            }
//
//            else -> {
//                val module = this.containingModule ?: return null
//                val address = module.addressRef()?.text ?: return null
//                val moduleName = module.name ?: return null
//                val elementName = this.name ?: return null
//                FqPath(address, moduleName, elementName)
//            }
//        }
//    }

val MvNamedElement.completionPriority
    get() = when {
        this is MvFunction && this.name in BUILTIN_FUNCTIONS -> BUILTIN_ITEM_PRIORITY
        else -> LOCAL_ITEM_PRIORITY
    }

fun MvNamedElement.searchReferences(): Query<PsiReference> =
    ReferencesSearch.search(
        this,
        PsiSearchHelper.getInstance(this.project).getUseScope(this)
    )

fun MvNamedElement.rename(newName: String) {
    val usageInfos = RenameUtil.findUsages(
        this,
        newName,
        false,
        false,
        emptyMap()
    )
    RenameUtil.doRename(
        this,
        newName,
        usageInfos,
        this.project,
        RefactoringElementListener.DEAF
    )
    PsiDocumentManager.getInstance(project).commitAllDocuments()
}
