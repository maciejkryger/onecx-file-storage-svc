package org.tkit.onecx.file.storage.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.file.storage.AbstractTest;
import org.tkit.onecx.file.storage.rs.external.v1.services.S3APIService;

import gen.org.tkit.onecx.file.storage.rs.external.v1.model.FileDownloadRequestDTOV1;
import gen.org.tkit.onecx.file.storage.rs.external.v1.model.PresignedUrlResponseDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@QuarkusTest
class FileStorageRestControllerV1Test extends AbstractTest {

    @InjectMock
    S3APIService s3APIService;

    String token;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.reset(s3APIService);
        token = keycloakClient.getAccessToken(ADMIN);
    }

    @Test
    void downloadFileTest() {
        byte[] fileContent = "onecx content".getBytes();
        GetObjectResponse metadata = GetObjectResponse.builder()
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength((long) fileContent.length)
                .build();

        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                metadata,
                AbortableInputStream.create(new ByteArrayInputStream(fileContent)));

        when(s3APIService.downloadFile(anyString(), anyString(), anyString()))
                .thenReturn(responseInputStream);

        byte[] response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "fileName": "test-file.txt",
                          "productName": "product1",
                          "applicationId": "app1"
                        }
                        """)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/file/download")
                .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .extract()
                .asByteArray();

        assertThat(response).isEqualTo(fileContent);
    }

    @Test
    void downloadFileNotFoundTest() {
        when(s3APIService.downloadFile(anyString(), anyString(), anyString()))
                .thenThrow(NoSuchKeyException.builder()
                        .message("The specified key does not exist.")
                        .build());

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "fileName": "missing-file.txt",
                          "productName": "product1",
                          "applicationId": "app1"
                        }
                        """)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/file/download")
                .then()
                .statusCode(404);
    }

    @Test
    void downloadFileGeneralExceptionTest() {
        when(s3APIService.downloadFile(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected S3 error"));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "fileName": "error-file.txt",
                          "productName": "product1",
                          "applicationId": "app1"
                        }
                        """)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/file/download")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadFileTest() throws Exception {
        byte[] fileContent = "onecx file content".getBytes();

        doNothing().when(s3APIService).uploadFile(anyString(), any(), anyString(), anyString());

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .multiPart("applicationId", "app1")
                .multiPart("productName", "product1")
                .multiPart("fileName", "my-file.txt")
                .multiPart("file", fileContent)
                .when()
                .post("/v1/file-storage/file/upload")
                .then()
                .statusCode(201);

    }

    @Test
    void uploadFileBadRequestTest() throws Exception {
        byte[] fileContent = "onecx file content".getBytes();

        doThrow(new RuntimeException("Failed to upload file"))
                .when(s3APIService).uploadFile(anyString(), any(), anyString(), anyString());

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .multiPart("applicationId", "app1")
                .multiPart("productName", "product1")
                .multiPart("fileName", "my-file.txt")
                .multiPart("file", fileContent)
                .when()
                .post("/v1/file-storage/file/upload")
                .then()
                .statusCode(400);
    }

    @Test
    void deleteFileTest() {
        FileDownloadRequestDTOV1 request = new FileDownloadRequestDTOV1();
        request.setFileName("test-file.txt");
        request.setProductName("product1");
        request.setApplicationId("app1");

        doNothing().when(s3APIService).deleteFile(anyString(), anyString(), anyString());

        given().contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/file/delete")
                .then()
                .statusCode(200);

    }

    @Test
    void getPresignedUploadUrlTest() {
        PresignedUrlResponseDTOV1 presignedUrlResponseDTOV1 = new PresignedUrlResponseDTOV1();
        presignedUrlResponseDTOV1.setUrl("https://presigned-upload-url.com");

        when(s3APIService.getPresignedUploadUrl(anyString(), anyString(), anyString()))
                .thenReturn(presignedUrlResponseDTOV1);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                        "fileName": "test-file.txt",
                        "productName": "product1",
                        "applicationId": "app1"
                        }
                        """)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/presigned/upload")
                .then()
                .statusCode(200);
    }

    @Test
    void getPresignedDownloadUrlTest() {
        PresignedUrlResponseDTOV1 presignedUrlResponseDTOV1 = new PresignedUrlResponseDTOV1();
        presignedUrlResponseDTOV1.setUrl("https://presigned-download-url.com");

        when(s3APIService.getPresignedDownloadUrl(anyString(), anyString(), anyString()))
                .thenReturn(presignedUrlResponseDTOV1);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                        "fileName": "test-file.txt",
                        "productName": "product1",
                        "applicationId": "app1"
                        }
                        """)
                .when()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post("/v1/file-storage/presigned/download")
                .then()
                .statusCode(200);
    }

}
