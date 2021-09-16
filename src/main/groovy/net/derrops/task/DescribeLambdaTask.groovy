package net.derrops.task

import groovy.json.JsonGenerator
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.LambdaResponse


class DescribeLambdaTask extends DefaultTask {

    @Input
    String lambda

    @OutputFile
    File describeFunction

    DescribeLambdaTask() {
        outputs.upToDateWhen {false}
    }

    @TaskAction
    void describeFunction() {

        LambdaClient client = LambdaClient.builder().build()

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(lambda)
                .build()

        GetFunctionResponse response = client.getFunction(getFunctionRequest)

        def generator = new JsonGenerator.Options()
                .build()


         LambdaResponse updatedResponse = response.toBuilder()
            .configuration(response.configuration().toBuilder().revisionId(null)
                    .lastModified(null)
                    .build())
            .code(response.code().toBuilder().location(null).build())
            .build()


        describeFunction.text = generator.toJson(updatedResponse.toString())

    }

}
