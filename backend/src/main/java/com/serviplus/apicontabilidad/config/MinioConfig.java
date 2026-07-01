package com.serviplus.apicontabilidad.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
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
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO creado: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/crear el bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
