package net.derrops.task


import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.InvokerHelper
import net.derrops.task.info.PublishInfo
import net.derrops.util.FilePublisher
import org.gradle.api.*
import org.gradle.api.tasks.*

class PublishToS3Task extends DefaultTask {

    @InputFile
    File uploadFile

    @InputFile
    File buildInfoFile

    @Input
    String bucket

    @Input
    String prefix

    @OutputFile
    File publishInfoFile


    PublishToS3Task() {
        //outputs.upToDateWhen { false } // do not allow this to be cached
    }

    PublishInfo writeInfo(net.derrops.task.info.BuildInfo buildInfo) {

        PublishInfo publishInfo = new PublishInfo()
        use(InvokerHelper) {
            publishInfo.setProperties(buildInfo.properties)
        }

        publishInfo.publishedS3Key = "${prefix}/${buildInfo.buildChecksum}/${uploadFile.name}"
        publishInfo.publishedPrefix = "${prefix}/${buildInfo.buildChecksum}"
        publishInfo.bucket = bucket

        def generator = new JsonGenerator.Options()
                .excludeNulls()
                .build()

        publishInfoFile.text = generator.toJson(publishInfo)

        return publishInfo
    }

    @TaskAction
    void publish() {
        def jsonSlurper = new JsonSlurper()
        net.derrops.task.info.BuildInfo buildInfo = jsonSlurper.parse(buildInfoFile) as net.derrops.task.info.BuildInfo

        FilePublisher.uploadButDoNotReplace(bucket, "${prefix}/${buildInfo.buildChecksum}/${uploadFile.name}", uploadFile)
        FilePublisher.upload(bucket, "${prefix}/${buildInfo.buildChecksum}/${buildInfoFile.name}", buildInfoFile) // this file is quick so do not worry

        def publishInfo = writeInfo(buildInfo)

    }
}