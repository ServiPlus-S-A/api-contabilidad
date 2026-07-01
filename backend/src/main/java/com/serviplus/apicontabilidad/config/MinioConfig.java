package com.serviplus.apicontabilidad.config;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {

    private final AppProperties appProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(appProperties.minio().endpoint())
                .credentials(appProperties.minio().accessKey(), appProperties.minio().secretKey())
                .build();
        ensureBucket(client, appProperties.minio().bucket());
        return client;
    }

    private void ensureBucket(MinioClient client, String bucket) {
        try {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket MinIO creado: {}", bucket);
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("BucketAlreadyOwnedByYou".equals(code) || "BucketAlreadyExists".equals(code)) {
                log.debug("Bucket MinIO '{}' ya existe", bucket);
            } else {
                log.warn("Error al crear bucket '{}': {}", bucket, e.getMessage(), e);
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/crear el bucket '{}': {}", bucket, e.getMessage(), e);
        }
    }
}
