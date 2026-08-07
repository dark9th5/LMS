package com.lmspilot.configuration.api;

import com.lmspilot.support.api.ApiException;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClient;

import java.util.UUID;
@Service
public class BrandingFileClient{
    private final RestClient client;
    private final String token;
    public BrandingFileClient(RestClient.Builder b,@Value("${file-storage-service.url:http://localhost:8089}")String url,@Value("${lmspilot.internal-token}")String t){
        client=b.baseUrl(url).build();
        token=t;
    }
    public BrandingFileMetadata metadata(UUID id){
        var m=client.get().uri("/internal/v1/files/{id}",id).header("X-Service-Token",token).retrieve().body(BrandingFileMetadata.class);
        if(m==null)throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"FILE_SERVICE_UNAVAILABLE","Không đọc được thông tin tệp thương hiệu");
        return m;
    }

}
