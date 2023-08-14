package org.move.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.move.ide.MoveIcons

object MoveFileType : LanguageFileType(MoveLanguage) {
    override fun getIcon() = MoveIcons.MOVE_LOGO
    override fun getName() = "Move"
    override fun getDefaultExtension() = "move"
    override fun getDescription() = "Move Language file"
}
