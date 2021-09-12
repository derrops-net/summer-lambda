package net.derrops.extension

import groovy.transform.ToString
import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator


@ToString
class SummerLambdaPluginExtension {

    Project project

    SummerLambdaPluginExtension(Instantiator instantiator,
                                Project project) {
        this.project = project
        this.function = new LambdaFunction()
        this.layer = new LambdaLayer()
    }

    /**
     * The AWS Account Id
     * Default: If not set this will be calculated based on the caller identity.
     */
    String accountId

    /**
     * The AWS Region
     * Default: If not set this will be calculated based on the caller identity.
     */
    String region

    /**
     * Settings for the Lambda Function
     */
    LambdaFunction function

    /**
     * Settings for the layer builds
     */
    LambdaLayer layer


    /**
     * The bucket to use for both layers and code.
     */
    String bucket

    String getBucket() {
        return bucket
    }

    void setBucket(String bucket) {
        this.bucket = bucket
        this.function.bucket = bucket
        this.layer.bucket = bucket
    }

    LambdaFunction function(Closure closure) {
        project.configure(function, closure)
        function.bucket = bucket
        return function
    }

    LambdaLayer layer(Closure closure) {
        project.configure(layer, closure)
        layer.bucket = bucket
        return layer
    }


}
