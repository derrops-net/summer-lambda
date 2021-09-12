package net.derrops.task


import com.google.common.hash.Hashing
import com.google.common.io.Files
import groovy.json.JsonGenerator
import net.derrops.util.DateUtil
import net.derrops.util.GitUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm

@CacheableTask
class BuildInfoTask extends DefaultTask {

    @InputFile
    File artifact

    @OutputFile
    File buildInfoFile

    @TaskAction
    void buildInfo() {

        net.derrops.task.info.BuildInfo buildInfo = new net.derrops.task.info.BuildInfo()
        buildInfo.with {

            awsAccountId = project.extensions.findByType(net.derrops.extension.SummerLambdaPluginExtension.class).accountId
            awsRegion = project.extensions.findByType(net.derrops.extension.SummerLambdaPluginExtension.class).region

            projectArtifact = artifact.name
            projectName = project.name.toString()
            projectGroup = project.group.toString()
            projectVersion = project.version.toString()

            // git stuff
            gitCommitHash = GitUtils.gitCommitHash()
            gitCommitAuthor = GitUtils.gitCommitAuthor()
            gitCommitDate = GitUtils.gitCommitDate()
            gitCommitMessage = GitUtils.gitCommitMessage()
            gitWorkingTreeDirty = GitUtils.gitWorkingTreeDirty()
            

            // build info
            buildDate = DateUtil.format(new Date(artifact.lastModified()))
            buildUser = System.getProperty("user.name")
            buildGradleVersion = project.gradle.gradleVersion
            buildJDK = Jvm.current().getJavaVersion().toString()
            buildChecksum = Base64.getEncoder().encodeToString(
                    Files.asByteSource(artifact).hash(Hashing.sha256()).toString().getBytes()
            )
            buildArtifact = artifact.name
        }

        def generator = new JsonGenerator.Options()
                .excludeNulls()
                .build()

        buildInfoFile.text = generator.toJson(buildInfo)

    }

}
