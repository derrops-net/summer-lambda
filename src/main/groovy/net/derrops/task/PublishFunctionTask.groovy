package net.derrops.task

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import net.derrops.task.info.LayerVersionInfo
import net.derrops.task.info.PublishFunctionInfo
import net.derrops.task.info.PublishInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest
import software.amazon.awssdk.services.lambda.model.FunctionCode
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest


class PublishFunctionTask extends DefaultTask {

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
    File lambdaPublishInfoFile

    @InputFile
    File describeFunction

    @InputFiles
    List<File> layerVersionInfoFiles

    @OutputFile
    File deployOutput


    @TaskAction
    void makeFunction() {


        def jsonSlurper = new JsonSlurper()
        PublishInfo lambdaPublishInfo = jsonSlurper.parse(lambdaPublishInfoFile) as PublishInfo
        List<LayerVersionInfo> layerPublishInfo = layerVersionInfoFiles.collect{jsonSlurper.parse(it) as LayerVersionInfo}


        LambdaClient client = LambdaClient.builder().build()


        boolean codeUpToDate = false // TODO - do a better check by putting function deploy info into s3
        boolean functionExists = false

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(lambda)
                .build()

        try {


            GetFunctionResponse response = client.getFunction(getFunctionRequest)

            functionExists = true
            codeUpToDate = response.configuration().description() == lambdaPublishInfo.publishedS3Key
//            layersUpToDate = response.configuration().layers().size() == layerVersionInfoFiles.size() &&
//                    response.configuration().layers().every{ l ->
//                layerPublishInfo.any{f -> f.layerVersionArn == l.arn() }
//            }

        } catch(Exception ex) {
            logger.info("An exception occurred when looking for the function. We will try and create it now")
            functionExists = false
        }

        PublishFunctionInfo publishFunctionInfo = new PublishFunctionInfo()



        if (!functionExists) {
            logger.info("Creating Function as it does not exist")

            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .functionName(lambda)
                    .code(
                            FunctionCode.builder()
                                    .s3Key(lambdaPublishInfo.publishedS3Key)
                                    .s3Bucket(lambdaPublishInfo.bucket)
                                    .build()
                    )
                    .layers(layerPublishInfo.collect{it.layerVersionArn})
                    .role(role)
                    .runtime(runtime)
                    .timeout(lambdaTimeout)
                    .handler(handler)
                    .description(lambdaPublishInfo.publishedS3Key)
                    .build()

            def response = client.createFunction(request)
            logger.info("Function Created ${response.functionArn()}")

            publishFunctionInfo.functionArn = response.functionArn()
            publishFunctionInfo.functionName = response.functionName()
            publishFunctionInfo.codeSha256 = response.codeSha256()
            publishFunctionInfo.layerArns = response.layers().collect {it.arn()}

            // return as we do not need to update it
            return
        } else {
            logger.info("Function exists")
        }

        logger.info("Updating Function Layers and Configuration")
        UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest = UpdateFunctionConfigurationRequest.builder()
                .functionName(lambda)
                .layers(layerPublishInfo.collect{it.layerVersionArn})
                .role(role)
                .runtime(runtime)
                .timeout(lambdaTimeout)
                .handler(handler)
                .description(lambdaPublishInfo.publishedS3Key)
                .build()

        client.updateFunctionConfiguration(updateFunctionConfigurationRequest)

        if (!codeUpToDate) {
            logger.info("Updating Function Code")
            UpdateFunctionCodeRequest updateFunctionCodeRequest = UpdateFunctionCodeRequest.builder()
                    .functionName(lambda)
                    .s3Key(lambdaPublishInfo.publishedS3Key)
                    .s3Bucket(lambdaPublishInfo.bucket)
                    .build()
            client.updateFunctionCode(updateFunctionCodeRequest)
        } else {
            logger.info("Function Code up to Date based on description")
        }



        def generator = new JsonGenerator.Options()
                .build()

        GetFunctionResponse describeReponse = client.getFunction(getFunctionRequest)
        deployOutput.text = generator.toJson(describeReponse.toString())

    }


}
