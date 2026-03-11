package org.tkit.onecx.file.storage.rs.external.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import io.quarkus.test.Mock;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.tkit.onecx.file.storage.AbstractTest;
import org.tkit.onecx.file.storage.rs.external.v1.mappers.PresginedUrlMapper;
import org.tkit.quarkus.context.ApplicationContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@QuarkusTest
class S3APIServiceTest extends AbstractTest {

    @Inject
    S3APIService s3APIService;

    @InjectMock
    S3Client s3Client;

    @InjectMock
    S3Presigner presigner;

    @InjectMock
    PresginedUrlMapper mapper;


    @BeforeEach
    void setUp() {
        ApplicationContext.start();
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());
    }

    @Test
    void uploadFileTest() throws Exception {
        String fileId = "my-file.txt";
        String productName = "product1";
        String applicationId = "app1";
        InputStream data = new ByteArrayInputStream("onecx content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() -> s3APIService.uploadFile(fileId, data, productName, applicationId));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFileExceptionTest() {
        String fileId = "my-file.txt";
        String productName = "product1";
        String applicationId = "app1";
        InputStream data = new ByteArrayInputStream("onecx content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        assertThrows(Exception.class,
                () -> s3APIService.uploadFile(fileId, data, productName, applicationId));
    }

    void downloadFileTest() {

    }

    void deleteFileTest() {

    }

    void getPresignedDownloadUrlTest() {

    }

    void getPresignedUploadUrlTest() {

    }

}