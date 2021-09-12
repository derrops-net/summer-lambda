package net.derrops.task

import net.derrops.task.info.LayerVersionInfo
import net.derrops.task.info.PublishInfo
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.InvokerHelper
import net.derrops.util.FilePublisher
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetLayerVersionByArnRequest
import software.amazon.awssdk.services.lambda.model.GetLayerVersionByArnResponse
import software.amazon.awssdk.services.lambda.model.LayerVersionContentInput
import software.amazon.awssdk.services.lambda.model.PublishLayerVersionRequest
import software.amazon.awssdk.services.lambda.model.PublishLayerVersionResponse

class PublishLambdaLayerVersionTask extends DefaultTask {

    @Input
    String runtime

    @Input
    Integer layerNumber

    @Input
    String layerName

    @InputFile
    File layerInfoFile

    @OutputFile
    File publishLayerInfoFile

    PublishLambdaLayerVersionTask() {
//        outputs.upToDateWhen { false }
    }

    LayerVersionInfo createLayerVersionInfo(PublishInfo publishInfo, PublishLayerVersionResponse response) {

        LayerVersionInfo layerVersionInfo = new LayerVersionInfo()
        use(InvokerHelper) {
            layerVersionInfo.setProperties(publishInfo.properties)
        }

        layerVersionInfo.layerName = layerName
        layerVersionInfo.layerVersion = response.version()
        layerVersionInfo.layerArn = response.layerArn()
        layerVersionInfo.layerVersionArn = response.layerVersionArn()
        layerVersionInfo.layerCreatedDate = response.createdDate()
        layerVersionInfo.layerDescription = response.description()
        layerVersionInfo.layerCompatibleRuntimes = response.compatibleRuntimesAsStrings()
        layerVersionInfo.layerNumber = layerNumber

        return layerVersionInfo
    }

    PublishLayerVersionResponse publishLayer(PublishInfo publishInfo){
        LayerVersionContentInput layerVersionContentInput = LayerVersionContentInput.builder()
                .s3Bucket(publishInfo.bucket)
                .s3Key(publishInfo.publishedS3Key)
                .build()

        PublishLayerVersionRequest request = PublishLayerVersionRequest.builder()
                .layerName(layerName)
                .description(description(publishInfo))
                .content(layerVersionContentInput)
                .compatibleRuntimesWithStrings(runtime)
                .build()

        LambdaClient client = LambdaClient.builder().build()

        return client.publishLayerVersion(request)
    }

    @TaskAction
    void publish() {

        def jsonSlurper = new JsonSlurper()
        PublishInfo publishInfo = jsonSlurper.parse(layerInfoFile) as PublishInfo

        String publishLayerKey = "${publishInfo.publishedPrefix}/${publishLayerInfoFile.name}"



        LayerVersionInfo layerVersionInfo = null
        logger.info("Determining if a new layer needs to be created")
        if (FilePublisher.keyExistsInBucket(publishInfo.bucket, publishLayerKey)) {

            logger.info("publishLayerKey=${publishLayerKey} exists in bucket=${publishInfo.bucket}. Checking if this is for the corrrect build.")

            FilePublisher.download(publishInfo.bucket, publishLayerKey, publishLayerInfoFile)

            try {
                layerVersionInfo = jsonSlurper.parse(publishLayerInfoFile) as LayerVersionInfo

                if (publishInfo.buildChecksum == layerVersionInfo.buildChecksum) {

                    GetLayerVersionByArnRequest request = GetLayerVersionByArnRequest.builder()
                            .arn(layerVersionInfo.layerVersionArn)
                            .build()

                    LambdaClient client = LambdaClient.builder().build()
                    GetLayerVersionByArnResponse getLayerVersionByArnResponse = client.getLayerVersionByArn(request)
                    logger.warn("Layer Version already exists LayerVersionArn=${getLayerVersionByArnResponse.getValueForField("LayerVersionArn", String.class).get()}. Skipping Layer Creation")

                }
            } catch (Exception ex) {
                logger.warn(ex.getStackTrace().toString())
                logger.warn("Could not load previous Lambda Layer. New Layer will be created.")
                layerVersionInfo = null
            }
        }

        if (layerVersionInfo == null) {
            logger.info("New Lambda Layer is needed, Creating now.")
            PublishLayerVersionResponse response = publishLayer(publishInfo)
            layerVersionInfo = createLayerVersionInfo(publishInfo, response)
            def generator = new JsonGenerator.Options()
                    .excludeNulls()
                    .build()
            publishLayerInfoFile.text = generator.toJson(layerVersionInfo)
            layerVersionInfo.layerVersionInfoS3Key = publishLayerKey
            FilePublisher.upload(layerVersionInfo.bucket, layerVersionInfo.layerVersionInfoS3Key, publishLayerInfoFile)
        }
        logger.info("Layer Version now at LayerVersionArn=${layerVersionInfo.layerVersionArn}")
    }

    static String description(PublishInfo pi) {
        return "s3=${pi.getPublishedS3Key()}"
    }

}
