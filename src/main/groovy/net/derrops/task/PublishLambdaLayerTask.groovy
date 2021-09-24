package net.derrops.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.PublishVersionRequest

class PublishLambdaLayerTask extends DefaultTask {

    @Input
    String lambda

    @Input
    String codeSha256

    @OutputFile
    lambdaVersionInfo

    @TaskAction
    void publish() {

        LambdaClient client = LambdaClient.builder().build()

        client.publishVersion(
                PublishVersionRequest.builder()
                        .functionName(lambda)
                        .description("derrops was here")
                        .codeSha256(codeSha256)
                .build() as PublishVersionRequest
        )

    }

}
