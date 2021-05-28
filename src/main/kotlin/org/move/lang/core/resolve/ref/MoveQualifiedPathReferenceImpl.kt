package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.MatchingProcessor
import org.move.lang.core.resolve.resolveItem
import org.move.lang.core.resolve.resolveModuleRefIntoQual

class MoveQualPathReferenceImpl<T : MoveQualPathReferenceElement>(
    qualPathRefElement: T,
    private val namespace: Namespace,
) : MoveReferenceBase<T>(qualPathRefElement) {

    override fun resolve(): MoveNamedElement? {
        val moduleRef = element.qualPath.moduleRef
        val qualModuleRef =
            if (moduleRef == null) {
                val resolved = resolveItem(element, namespace)
                if (resolved !is MoveItemImport) {
                    return resolved
                }
                resolved.parentImport().fullyQualifiedModuleRef
            } else {
                resolveModuleRefIntoQual(moduleRef) ?: return null
            }
        val refName = element.referenceName ?: return null
        return resolveQualifiedPath(qualModuleRef, refName, setOf(namespace))
    }
}

fun processModuleItems(
    module: MoveModuleDef,
    visibilities: Set<Visibility>,
    namespaces: Set<Namespace>,
    processor: MatchingProcessor,
): Boolean {
    for (namespace in namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                listOf(
                    visibilities.flatMap { module.functionSignatures(it) },
                    module.structSignatures(),
                    module.consts(),
                ).flatten()
            )
            Namespace.TYPE -> processor.matchAll(module.structSignatures())
            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            else -> false
        }
        if (found) return true
    }
    return false
}

fun resolveQualifiedPath(
    qualModuleRef: MoveFullyQualifiedModuleRef,
    refName: String,
    ns: Set<Namespace>,
): MoveNamedElement? {
    val module = (qualModuleRef.reference?.resolve() as? MoveModuleDef) ?: return null
    var resolved: MoveNamedElement? = null

    val vs = Visibility.buildSetOfVisibilities(qualModuleRef)
    processModuleItems(module, vs, ns) {
        if (it.name == refName && it.element != null) {
            resolved = it.element
            return@processModuleItems true
        }
        return@processModuleItems false
    }
    return resolved
}
