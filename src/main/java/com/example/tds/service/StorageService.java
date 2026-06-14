package com.example.tds.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    public void uploadFile(String bucketName, String filename, MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()

            );
        }catch (Exception e){
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public void deleteBucket(String bucketName) {
        try {
            Iterable<Result<Item>> objects =
                    minioClient.listObjects(
                            ListObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .recursive(true)
                                    .build());

            for (Result<Item> result : objects) {
                Item item = result.get();

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build());
            }

            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete bucket", e);
        }
    }

    public void uploadFile(String bucketName, String filename, MultipartFile file, Boolean isPublic) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                if(isPublic) this.makeBucketPublic(bucketName);
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()

            );
        }catch (Exception e){
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public String getPublicUrl(String bucketName, String filename) {
        try {
            String policy = minioClient.getBucketPolicy(
                    GetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            boolean isPublic = policy.contains("\"Action\":[\"s3:GetObject\"]")
                    && policy.contains("\"Principal\":{\"AWS\":[\"*\"]}");

            if(isPublic){
                return minioUrl + "/" + bucketName + "/" + filename;
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(bucketName)
                            .object(filename)
                            .expiry(24 * 60 * 60)
                            .build()
            );
        }catch (Exception e){
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public void makeBucketPublic(String bucketName) throws Exception {
        try {
            String policy = """
                    {
                      "Version":"2012-10-17",
                      "Statement":[
                        {
                          "Effect":"Allow",
                          "Principal":{"AWS":["*"]},
                          "Action":["s3:GetObject"],
                          "Resource":["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        }catch (Exception e){
            throw new Exception("Error occurred: " + e.getMessage());
        }
    }
}
