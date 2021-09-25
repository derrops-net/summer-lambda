package net.derrops.task

import net.derrops.util.GenericUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse


class DescribeLambdaTask extends DefaultTask {

    @Input
    String lambda

    @OutputFile
    File describeCode

    @OutputFile
    File describeConfiguration

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

        GetFunctionResponse updatedResponse = response.toBuilder()
            .configuration(response.configuration().toBuilder()
                    .revisionId(null)
                    .lastModified(null)
            .build())
            .code(response.code().toBuilder().location(
                    response.code().location().split("\\?")[0] // only include the repository code
            ).build())
            .build()


        if (!describeCode.exists() || describeCode.exists() && describeCode.text != updatedResponse.code().toString()) {
            logger.info("code has changed")
            describeCode.text = updatedResponse.code().toString()
        }else{
            println "!!!NO CODE CHANGE!!!!!"
        }

        if (!describeConfiguration.exists() || describeConfiguration.exists() && describeConfiguration.text != updatedResponse.configuration().toString()) {
            logger.info("config has changed\n\n\n")
            println describeConfiguration.text
            println "--"
            println updatedResponse.configuration().toString()
            describeConfiguration.text = updatedResponse.configuration().toString()
        }else{
            println "!!!NO CONFIG CHANGE!!!!!"
        }

    }

}
