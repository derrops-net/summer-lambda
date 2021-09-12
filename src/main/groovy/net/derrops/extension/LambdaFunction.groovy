package net.derrops.extension

import groovy.transform.ToString

@ToString
class LambdaFunction {

    /**
     * The name of the lambda function which will be deployed.
     * Default: ${project.name}
     */
    String name

    /**
     * The runtime of the function
     */
    String runtime = "java11"

    /**
     * The IAM role of the function
     */
    String role

    /**
     * The memory of the Lambda Function
     */
    Integer memory = 128

    /**
     * The handler of the Lambda Function
     */
    String handler

    /**
     * The bucket used for the code
     */
    String bucket

}
