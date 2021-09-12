package net.derrops.task.info

import groovy.transform.ToString

@ToString(includeSuper = true)
class LayerVersionInfo extends PublishInfo {

    String layerName
    Integer layerNumber
    Long layerVersion
    String layerArn
    String layerVersionArn
    String layerCreatedDate
    String layerDescription
    String layerVersionInfoS3Key
    List<String> layerCompatibleRuntimes
    String layerSha


}
