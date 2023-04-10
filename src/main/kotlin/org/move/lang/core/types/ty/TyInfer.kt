package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.*

sealed class TyInfer : Ty {
    // Note these classes must NOT be `data` classes and must provide equality by identity
    class TyVar(
        val origin: TyTypeParameter? = null,
        override var parent: NodeOrValue = VarValue(null, 0)
    ) : TyInfer(), Node {

        override fun abilities(): Set<Ability> = origin?.abilities() ?: Ability.none()
    }

    class IntVar(
        override var parent: NodeOrValue = VarValue(null, 0)
    ) : TyInfer(), Node {

        override fun abilities(): Set<Ability> = Ability.all()
    }

    override fun toString(): String = tyToString(this)
}
