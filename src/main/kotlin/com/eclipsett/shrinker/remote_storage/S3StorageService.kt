package com.eclipsett.shrinker.remote_storage

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime

@Service
class S3StorageService(
    private val fileStorageProperties: FileStorageProperties,
    private val s3: S3Client, private val s3Presigner: S3Presigner
) {

    private val log = LoggerFactory.getLogger(S3StorageService::class.java)

    @Throws(IOException::class)
    fun uploadToS3(objectKey: String, filePath: String) {
        val file = File(filePath)
        try {
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket)
                    .key(objectKey)
                    .build(),
                RequestBody.fromFile(file)
            )
            log.info("Upload file to S3. objectId: {}", objectKey)
            File(file.parent).deleteRecursively()
        } catch (e: SdkException) {
            log.error("Failed to upload file to S3 {}", e.message)
        }
    }

    fun deleteFile(objectId: String) {
        try {
            val request = DeleteObjectRequest.builder()
                .bucket(fileStorageProperties.uploadBucket)
                .key(objectId)
                .build()
            s3.deleteObject(request)
        } catch (e: S3Exception) {
            log.info("deleteFile error: ${e.message}")
        }
    }

    fun getPresignedUrl(objectName: String): PresignedRequest? {
        return try {
            val expiration = Duration.ofMinutes(240)

            val getObjectRequest = GetObjectRequest.builder()
                .bucket(fileStorageProperties.uploadBucket)
                .key(objectName)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(5))
                .build()

            val presignedRequest = s3Presigner.presignGetObject(presignRequest)

            log.info("Generated presign url")
            PresignedRequest(
                presignedRequest.url().toString(),
                LocalDateTime.now().plus(expiration).toString()
            )
        } catch (e: S3Exception) {
            log.info("getPresignedUrl error: ${e.message}")
            null
        }
    }

    companion object {
        data class PresignedRequest(val presignedUrl: String, val expirationDate: String, )
    }

}