package net.derrops

import net.derrops.task.DescribeLambdaTask
import net.derrops.util.GenericUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.impldep.org.eclipse.jgit.annotations.NonNull
import org.gradle.internal.reflect.Instantiator
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse

import javax.inject.Inject

class SummerLambdaPlugin implements Plugin<Project> {

    @NonNull
    final Instantiator instantiator;

    static def logger = LoggerFactory.getLogger(this.class)

    public static final String DERROPS = "derrops"

    public static final List<List<String>> ALPHABET = ['a*', 'b*','c*', 'd*', 'e*', 'f*','g*','h*','i*', 'j*','k*','l*','m*','n*','o*', 'p*','q*','r*','s*', 't*','u*','v*','w*','x*','y*','z*']

    public static final int NO_LAYERS = 5

    public static final List<List<String>> LAYER_GROUPS = GenericUtils.split(ALPHABET, NO_LAYERS)
    public static final List<Integer> LAYERS = 1..NO_LAYERS

    @Inject
    SummerLambdaPlugin(@NonNull Instantiator instantiator) {
        this.instantiator = instantiator
    }

    @Override
    void apply(Project project) {

        File buildDirectory = new File(project.buildDir, DERROPS)

        net.derrops.extension.SummerLambdaPluginExtension extension = project.getExtensions().create("summerLambda", net.derrops.extension.SummerLambdaPluginExtension.class, instantiator, project)



        StsClient stsClient = StsClient.builder()
                .build()

        GetCallerIdentityResponse callerIdentity = stsClient.getCallerIdentity()

        setDefaultValuesOnExtension(project, extension, callerIdentity)


        def lambdaArchive = project.tasks.register("lambdaArchive", Zip.class) { lambdaArchive ->

            def sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
            lambdaArchive.from(sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME).output)

            lambdaArchive.archiveFileName = "${extension.function.name}-${project.version}-lambda.zip"

            lambdaArchive.destinationDirectory = buildDirectory
            lambdaArchive.preserveFileTimestamps = false
            lambdaArchive.reproducibleFileOrder = true

            dependsOn(project.tasks.findByName("classes"))
            project.tasks.findByName("assemble").dependsOn("lambdaArchive")

        }


        def lambdaArchiveInfo = project.tasks.register("lambdaArchiveInfo", net.derrops.task.BuildInfoTask.class) { lambdaArchiveInfo ->
            def lambdaArchiveTask = project.tasks.findByName("lambdaArchive")
            dependsOn lambdaArchiveTask
            artifact = lambdaArchiveTask.outputs.files.singleFile
            buildInfoFile = new File(project.buildDir, "derrops/" + artifact.name + ".build-info.json")
        }

        LAYERS.collect{

            project.tasks.register("layerArchive-${it}", Zip.class) { layerArchive ->

                def emptyFileDir = "${project.buildDir}/derrops/empty"

                doFirst {
                    new File(emptyFileDir).mkdirs()
                    new File(emptyFileDir, "empty.txt").text = "test"
                }

                List<String> includes = []
                includes.addAll(LAYER_GROUPS.get(it - 1))
                includes.add("empty.txt")


                layerArchive
                        .from(project.configurations.compileClasspath)
                        .into('java/lib')
                        .exclude(extension.layer.excludes)
                        .include(includes)
                        .from(emptyFileDir)



                layerArchive.archiveFileName = "${extension.layer.name}-layer-${it}.zip"
                layerArchive.destinationDirectory = buildDirectory //new File(buildDirectory, "derrops")
                layerArchive.preserveFileTimestamps = false
                layerArchive.reproducibleFileOrder = true

                dependsOn(project.tasks.findByName("classes"))
                project.tasks.findByName("assemble").dependsOn(layerArchive)

            }

            def layerArchiveInfo = project.tasks.register("layerArchiveInfo-${it}", net.derrops.task.BuildInfoTask.class) { layerArchiveInfo ->
                def layerArchiveTask = project.tasks.findByName("layerArchive-${it}")
                dependsOn layerArchiveTask
                artifact = layerArchiveTask.outputs.files.singleFile
                buildInfoFile = new File(project.buildDir, "derrops/" + artifact.name + ".build-info.json")
            }

            def publishLayer = project.tasks.register("publishLayer-${it}", net.derrops.task.PublishToS3Task.class) { publishLayer ->

                publishLayer.dependsOn(project.tasks.findByName("layerArchiveInfo-${it}"))

                def layerArchiveFile = project.tasks.findByName("layerArchive-${it}").outputs.files.singleFile
                def layerArchiveInfoFile = project.tasks.findByName("layerArchiveInfo-${it}").outputs.files.singleFile

                publishLayer.buildInfoFile = layerArchiveInfoFile
                publishLayer.prefix = "${project.group}/${project.name}/layer-${it}"
                publishLayer.uploadFile = layerArchiveFile
                publishLayer.bucket = extension.layer.bucket

                publishLayer.publishInfoFile = new File(project.buildDir, "derrops/" + layerArchiveFile.name + ".publish-info.json")

            }

            def publishLambdaLayerVersion = project.tasks.register("publishLambdaLayerVersion-${it}", net.derrops.task.PublishLambdaLayerVersionTask) { publishLambdaLayerVersion ->
                publishLambdaLayerVersion.dependsOn("publishLayer-${it}")

                publishLambdaLayerVersion.layerInfoFile = project.tasks.findByName("publishLayer-${it}").outputs.files.singleFile
                publishLambdaLayerVersion.layerName = "${extension.layer.name}-${it}"
                publishLambdaLayerVersion.publishLayerInfoFile = new File(project.buildDir, "derrops/" + "${publishLambdaLayerVersion.layerName}.lambda-version-info.json")
                publishLambdaLayerVersion.layerNumber = it
                publishLambdaLayerVersion.runtime = extension.layer.runtime
            }

        }

        def publishLambda = project.tasks.register("publishLambda", net.derrops.task.PublishToS3Task.class) { publishLambda ->

            publishLambda.dependsOn(project.tasks.findByName("lambdaArchiveInfo"))
            publishLambda.bucket = extension.function.bucket

            def lambdaArchiveInfoFile = project.tasks.findByName("lambdaArchiveInfo").outputs.files.singleFile
            def lambdaArchiveFile = project.tasks.findByName("lambdaArchive").outputs.files.singleFile

            publishLambda.buildInfoFile = lambdaArchiveInfoFile

            publishLambda.prefix = "${project.group}/${project.name}/function/${project.version}"
            publishLambda.uploadFile = lambdaArchiveFile
            publishLambda.publishInfoFile = new File(project.buildDir, "derrops/" + lambdaArchive.name + ".publish-info.json")

        }


        def describeFunction = project.tasks.register("describeFunction", DescribeLambdaTask.class) { task ->
            task.lambda = extension.function.name
            task.describeFunction = new File(project.buildDir, "derrops/" + "function-info.json")
        }


        def publishFunction = project.tasks.register("publishFunction", net.derrops.task.PublishFunctionTask.class) { publishFunction ->

            List<Task> layerVersionTasks = LAYERS.collect{project.tasks.findByName("publishLambdaLayerVersion-${it}")}

            def publishLambdaTask = project.tasks.findByName("publishLambda")
            def describeFunctionTask = project.tasks.findByName("describeFunction")

            LAYERS.forEach{dependsOn(layerVersionTasks)}
            dependsOn(publishLambdaTask)
            dependsOn(describeFunctionTask)

            publishFunction.describeFunction = describeFunctionTask.outputs.files.singleFile
            publishFunction.lambda = extension.function.name
            publishFunction.layerVersionInfoFiles = layerVersionTasks.collect{it.outputs.files.singleFile}
            publishFunction.lambdaPublishInfoFile = publishLambdaTask.outputs.files.singleFile
            publishFunction.handler = extension.function.handler
            publishFunction.memory = extension.function.memory
            publishFunction.runtime = extension.function.runtime
            publishFunction.lambdaTimeout = extension.function.timeout
            publishFunction.role = extension.function.role
            publishFunction.deployOutput = new File(project.buildDir, "derrops/" + "lambda-deploy-info.json")

        }

    }

    /**
     * Set the default values for the extension
     * @param project
     * @param extension
     * @param callerIdentity
     */
    net.derrops.extension.SummerLambdaPluginExtension setDefaultValuesOnExtension(Project project, net.derrops.extension.SummerLambdaPluginExtension extension, GetCallerIdentityResponse callerIdentity) {

        extension.accountId = extension.accountId ?: callerIdentity.account()
        extension.region = extension.region ?: Region.AP_SOUTHEAST_2

        extension.function.bucket = extension.function.bucket ?: extension.bucket
        extension.layer.bucket = extension.layer.bucket ?: extension.bucket


        extension.function.name = extension.function.name ?: project.name
        extension.layer.name = extension.layer.name ?: project.name

        return extension
    }

}
