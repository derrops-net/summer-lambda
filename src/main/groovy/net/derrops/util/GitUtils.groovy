package net.derrops.util

class GitUtils {

    static def gitCommitHash() {
        try {
            return 'git rev-parse --verify HEAD'.execute().text.trim()
        } catch (Exception ex) {
            return null
        }
    }

    static def gitCommitAuthor() {
        try {
            return "git log -1 --pretty=format:'%an'".execute().text.trim()
        } catch (Exception ex) {
            return null
        }
    }

    static def gitCommitDate(){
        try {
            return "git show -s --format=%ci HEAD".execute().text.trim()
        } catch (Exception ex) {
            return null
        }
    }

    static def gitCommitMessage() {
        try {
            return "git show -s --format=%s".execute().text.trim()
        } catch (Exception ex) {
            return null
        }
    }

    static def gitWorkingTreeDirty(){
        try {
            return "git diff --quiet || echo 'dirty'".execute().text.trim() == "dirty"
        } catch (Exception ex) {
            return null
        }
    }

}
