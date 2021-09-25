package net.derrops.task

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import net.derrops.task.info.PublishInfo
import net.derrops.task.info.UpdateCodeInfo
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeResponse


class UpdateFunctionCodeTask extends DefaultTask {

    @Input
    String lambda

    @InputFile
    File describeCode

    @InputFile
    File lambdaPublishInfoFile

    @OutputFile
    File updateFunctionCodeResponseFile

//    @OutputFile
//    File updateCodeInfo

    UpdateFunctionCodeTask(){
//        outputs.upToDateWhen {
//            boolean updateToDate = updateFunctionCodeResponseFile.exists() && describeCode.exists() && updateFunctionCodeResponseFile.text == describeCode.text
//            return updateToDate
//        }
    }

    @TaskAction
    void updateFunctionCode() {

        def jsonSlurper = new JsonSlurper()
        PublishInfo lambdaPublishInfo = jsonSlurper.parse(lambdaPublishInfoFile) as net.derrops.task.info.PublishInfo

        LambdaClient client = LambdaClient.builder().build()

        UpdateFunctionCodeRequest updateFunctionCodeRequest = UpdateFunctionCodeRequest.builder()
                .functionName(lambda)
                .s3Key(lambdaPublishInfo.publishedS3Key)
                .s3Bucket(lambdaPublishInfo.bucket)
                .build()

        UpdateFunctionCodeResponse updateFunctionCodeResponse = client.updateFunctionCode(updateFunctionCodeRequest)

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(lambda)
                .build()

        GetFunctionResponse response = client.getFunction(getFunctionRequest)

        GetFunctionResponse enrichedResponse = response.toBuilder()
                .code(response.code().toBuilder().location(
                        response.code().location().split("\\?")[0] // only include the repository code
                ).build())
                .build()

        // update the describe task as well
        //updateFunctionCodeResponseFile.text = enrichedResponse.code().toString()
        //describeCode.text =                   enrichedResponse.code().toString()

        updateFunctionCodeResponseFile.text = createCodeInfo(lambdaPublishInfo, updateFunctionCodeResponse)
    }


    String createCodeInfo(PublishInfo publishInfo, UpdateFunctionCodeResponse updateFunctionCodeResponse) {

        UpdateCodeInfo updateCodeInfo = new UpdateCodeInfo()

        use(InvokerHelper) {
            updateCodeInfo.setProperties(publishInfo.properties)
        }

        updateCodeInfo.codeSha256 = updateFunctionCodeResponse.codeSha256()
        updateCodeInfo.lastModified = updateFunctionCodeResponse.lastModified()

        def generator = new JsonGenerator.Options()
                .excludeNulls()
                .build()

        return generator.toJson(updateCodeInfo)
    }
}
