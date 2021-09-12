package net.derrops.extension

import groovy.transform.ToString

@ToString
class LambdaLayer {

    /**
     * The prefix of the layer names. Layers will be named as ${name}-${layerNumber}
     */
    String name

    /**
     * The bucket used to store the layer archives
     */
    String bucket

    /**
     * The runtime of the function
     */
    String runtime = "java11"

    /**
     * Any dependencies to exclude
     */
    List<String> excludes = new ArrayList<>()
}
