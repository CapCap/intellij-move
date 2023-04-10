package org.move.ide.hints.type

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class MvInlayTypeHintsProvider : InlayHintsProvider<MvInlayTypeHintsProvider.Settings> {
    override val key: SettingsKey<Settings> get() = KEY

    override val name: String get() = "Type hints"

    override val previewText: String
        get() = """
            module 0x1::M {
                struct Foo<T1, T2, T3> { x: T1, y: T2, z: T3 }
                fun main() {
                    let foo = Foo { x: 1, y: b"abc", z: true };
                }
            }
            """.trimIndent()

    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object :
        ImmediateConfigurable {

        override val cases: List<ImmediateConfigurable.Case>
            get() = listOf(
                ImmediateConfigurable.Case("Show for variables", "variables", settings::showForVariables),
            )

        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        val project = file.project
        return object : FactoryInlayHintsCollector(editor) {

            val typeHintsFactory = MvTypeHintsPresentationFactory(factory)

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (project.service<DumbService>().isDumb) return true

                when {
                    settings.showForVariables && element is MvLetStmt -> {
                        val pat = element.pat ?: return true
                        if (element.typeAnnotation != null) return true
                        presentTypeForPat(pat)
                    }
                }
                return true
            }

            private fun presentTypeForPat(pat: MvPat) {
                val msl = pat.isMsl()
//                val inferenceCtx = pat.inferenceContext(msl)
                val inference = pat.inference(msl) ?: return
                for (bindingPat in pat.descendantsOfType<MvBindingPat>()) {
                    if (bindingPat.identifier.text.startsWith("_")) continue

                    val ty = inference.getPatType(bindingPat)
//                    val bindingTy = inferenceCtx.getBindingPatTy(bindingPat)
//                    val bindingTy = inference.getPatType()
                    if (ty is TyUnknown) continue

                    presentTypeForBinding(bindingPat, ty)
                }
            }

            private fun presentTypeForBinding(
                binding: MvBindingPat,
                bindingTy: Ty
            ) {
                val presentation = typeHintsFactory.typeHint(bindingTy)
                sink.addInlineElement(
                    binding.endOffset,
                    false,
                    presentation,
                    false
                )
            }
        }
    }

    data class Settings(
        var showForVariables: Boolean = true,
    )

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("move.type.hints")
    }
}
