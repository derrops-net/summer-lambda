package net.derrops.task.info

import groovy.transform.ToString


@ToString(includeSuper = true)
class PublishInfo extends BuildInfo {

    String bucket
    String publishedPrefix
    String publishedS3Key

}
