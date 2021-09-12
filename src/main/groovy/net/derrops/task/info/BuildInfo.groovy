package net.derrops.task.info

import groovy.transform.ToString


/**
 * Info .
 */
@ToString
class BuildInfo {

    String awsAccountId
    String awsRegion

    String projectArtifact
    String projectName
    String projectGroup
    String projectVersion

    String  gitCommitDate
    String  gitCommitAuthor
    String  gitCommitHash
    String  gitCommitMessage
    boolean gitWorkingTreeDirty

    String buildDate
    String buildUser
    String buildGradleVersion
    String buildJDK
    String buildChecksum
    String buildArtifact
}
