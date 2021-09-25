package net.derrops.task

import groovy.json.JsonSlurper
import net.derrops.task.info.LayerVersionInfo
import net.derrops.task.info.PublishInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse

class UpdateFunctionConfigurationTask extends DefaultTask {

    @Input
    String runtime

    @Input
    Integer lambdaTimeout

    @Input
    String role

    @Input
    String lambda

    @Input
    String handler

    @Input
    Integer memory

    @InputFile
    File describeConfiguration

    @InputFile
    File lambdaPublishInfoFile

    @OutputFile
    File updateFunctionConfigurationResponseFile

    @InputFiles
    List<File> layerVersionInfoFiles


    @TaskAction
    void updateFunctionConfiguration() {

        LambdaClient client = LambdaClient.builder().build()

        def jsonSlurper = new JsonSlurper()
        PublishInfo lambdaPublishInfo = jsonSlurper.parse(lambdaPublishInfoFile) as PublishInfo
        List<LayerVersionInfo> layerPublishInfo = layerVersionInfoFiles.collect{jsonSlurper.parse(it) as LayerVersionInfo}

        UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest = UpdateFunctionConfigurationRequest.builder()
                .functionName(lambda)
                .layers(layerPublishInfo.collect{it.layerVersionArn})
                .role(role)
                .runtime(runtime)
                .timeout(lambdaTimeout)
                .handler(handler)
                .description(lambdaPublishInfo.publishedS3Key)
                .build()

        UpdateFunctionConfigurationResponse updateFunctionConfigurationResponse = client.updateFunctionConfiguration(updateFunctionConfigurationRequest)

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(lambda)
                .build()

        GetFunctionResponse response = client.getFunction(getFunctionRequest)

        GetFunctionResponse enrichedResponse = response.toBuilder()
                .configuration(response.configuration().toBuilder()
                        .revisionId(null)
                        .lastModified(null)
                        .build())
                .build()

        // update the describe task as well
        updateFunctionConfigurationResponseFile.text = enrichedResponse.configuration().toString()
        describeConfiguration.text =                   enrichedResponse.configuration().toString()

    }


}
