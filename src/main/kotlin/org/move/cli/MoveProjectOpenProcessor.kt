package org.move.cli

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.runconfig.makeDefaultRunConfiguration
import org.move.ide.MoveIcons
import org.move.ide.newProject.openFile
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.contentRoots
import org.move.openapiext.fullyRefreshDirectory

class MoveProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName() = "Move"
    override fun getIcon() = MoveIcons.MOVE

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, MoveConstants.MANIFEST_FILE)
                || file.isDirectory && file.findChild(MoveConstants.MANIFEST_FILE) != null

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean,
    ): Project? {
        val platformOpenProcessor = PlatformProjectOpenProcessor.getInstance()
        return platformOpenProcessor.doOpenProject(
            virtualFile,
            projectToClose,
            forceOpenInNewFrame
        )?.also {
            StartupManager.getInstance(it).runWhenProjectIsInitialized {
                it.moveProjects.refreshAllProjects()

                // create default build configuration
                val runManager = RunManager.getInstance(it)
                if (runManager.allConfigurationsList.isEmpty()) {
                    it.makeDefaultRunConfiguration()
                }

                val packageRoot = it.contentRoots.firstOrNull()
                if (packageRoot != null) {
                    val manifest = packageRoot.findChild(MoveConstants.MANIFEST_FILE)
                    if (manifest != null) {
                        it.openFile(manifest)
                    }
                    updateAllNotifications(it)
                }
            }
        }
    }
}
