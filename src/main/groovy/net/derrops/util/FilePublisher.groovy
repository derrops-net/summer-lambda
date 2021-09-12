package net.derrops.util

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class FilePublisher {

    private static s3client = S3Client.builder().build()

    static def logger = LoggerFactory.getLogger(this.class)

    static boolean keyExistsInBucket(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(key).build()
            s3client.headObject(request)
            return true
        } catch (NoSuchKeyException e) {
            return false
        }
    }

    static void uploadButDoNotReplace(String bucket, String key, File fileToUpload) {

        if (keyExistsInBucket(bucket, key)) {
            logger.warn("SKIPPING S3 UPLOAD: file ${fileToUpload.name} already exists at: ${key}")
        } else {
            logger.info("UPLOADING TO S3: file ${fileToUpload.name} to ${key}")
            upload(bucket, key, fileToUpload)
        }
    }

    static boolean upload(String bucket, String key, File fileToUpload) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

        s3client.putObject(objectRequest, RequestBody.fromFile(fileToUpload))
        return true
    }

    static void download(String bucket, String key, File file) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        ResponseInputStream<GetObjectResponse> inputStream = s3client.getObject(getRequest)
        FileUtils.copyInputStreamToFile(inputStream, file);
    }
}
