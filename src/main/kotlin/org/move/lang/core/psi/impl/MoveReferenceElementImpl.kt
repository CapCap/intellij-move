package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvReferenceElement

abstract class MvReferenceElementImpl(node: ASTNode) : MvElementImpl(node),
                                                       MvReferenceElement {
    abstract override fun getReference(): MvPolyVariantReference
}
