package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.*

data class EqualityConstraint(var ty1: Ty, var ty2: Ty) : TypeFoldable<EqualityConstraint> {
    init {
        // always sort TyUnknown to the right
        if (ty1 is TyUnknown && ty2 !is TyUnknown) {
            ty1 = ty2
            ty2 = TyUnknown
        }
    }

    override fun innerFoldWith(folder: TypeFolder): EqualityConstraint =
        EqualityConstraint(ty1.foldWith(folder), ty2.foldWith(folder))

    override fun visitWith(visitor: TypeVisitor): Boolean = innerVisitWith(visitor)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        ty1.visitWith(visitor) || ty2.visitWith(visitor)

    override fun toString(): String =
        "$ty1 == $ty2"
}

class ConstraintSolver(val ctx: InferenceContext) {
    private val constraints = mutableListOf<EqualityConstraint>()

    fun registerConstraint(constraint: EqualityConstraint) {
        constraints.add(constraint)
    }

    fun processConstraints(): Boolean {
        var solvable = true
        while (constraints.isNotEmpty()) {
            val constraint = constraints.removeFirst()
            val isSuccessful = processConstraint(constraint)
            if (!isSuccessful) {
                solvable = false
            }
        }
        return solvable
    }

    private fun processConstraint(rawConstraint: EqualityConstraint): Boolean {
        val constraint = rawConstraint.foldTyInferWith(ctx::resolveTyInfer)
        var ty1 = constraint.ty1
        var ty2 = constraint.ty2
        if (ctx.msl) {
            ty1 = ty1.mslTy()
            ty2 = ty2.mslTy()
        }
        when {
            ty1 is TyInfer.IntVar && ty2 is TyInfer.IntVar -> {
                ctx.intUnificationTable.unifyVarVar(ty1, ty2)
            }
            ty1 is TyInfer.IntVar && ty2 is TyInteger -> {
                ctx.intUnificationTable.unifyVarValue(ty1, ty2)
            }
            ty2 is TyInfer.IntVar && ty1 is TyInteger -> {
                ctx.intUnificationTable.unifyVarValue(ty2, ty1)
            }

            ty1 is TyInfer.TyVar && ty2 is TyInfer.TyVar -> {
                if ((ty1.abilities() - ty2.abilities()).isNotEmpty()) return false
                ctx.unificationTable.unifyVarVar(ty1, ty2)
            }

            ty1 is TyInfer.TyVar && ty2 !is TyInfer.TyVar -> {
                if ((ty1.abilities() - ty2.abilities()).isNotEmpty()) return false
                ctx.unificationTable.unifyVarValue(ty1, ty2)
            }

            ty2 is TyInfer.TyVar /* && ty1 !is TyInfer.TyVar */ -> {
                if ((ty2.abilities() - ty1.abilities()).isNotEmpty()) return false
                ctx.unificationTable.unifyVarValue(ty2, ty1)
            }

            else -> {
                when {
                    ty1 is TyVector && ty2 is TyVector -> {
                        constraints.add(0, EqualityConstraint(ty1.item, ty2.item))
                    }
                    ty1 is TyVector && ty2 is TyUnknown -> {
                        constraints.add(0, EqualityConstraint(ty1.item, TyUnknown))
                    }

                    ty1 is TyReference && ty2 is TyReference -> {
                        constraints.add(0, EqualityConstraint(ty1.referenced, ty2.referenced))
                    }
                    ty1 is TyReference && ty2 is TyUnknown -> {
                        constraints.add(0, EqualityConstraint(ty1.referenced, TyUnknown))
                    }

                    ty1 is TyStruct && ty2 is TyStruct && ty1.item == ty2.item -> {
                        if (ty1.typeArgs.size != ty2.typeArgs.size) return false
                        val cs =
                            ty1.typeArgs.zip(ty2.typeArgs)
                                .map { (t1, t2) -> EqualityConstraint(t1, t2) }
                        constraints.addAll(0, cs)
                    }
                    ty1 is TyStruct && ty2 is TyUnknown -> {
                        val cs = ty1.typeArgs.map { arg -> EqualityConstraint(arg, TyUnknown) }
                        constraints.addAll(0, cs)
                    }

                    else -> {
                        // if types are not compatible, constraints are unsolvable
                        if (!isCompatible(ty1, ty2)) {
                            return false
                        }
                        // no variables to solve
                        if (
                            !(ty1.visitWith { it is TyInfer.TyVar })
                            && !(ty2.visitWith { it is TyInfer.TyVar })
                        ) {
                            return true
                        }
                        // TODO: add
//                        error("Constraint $ty1 == $ty2 is not supported")
                    }
                }
            }
        }
        return true
    }
}
