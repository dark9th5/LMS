package com.lmspilot.reporting.api;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClient;

import java.util.*;
@Service
public class EnrollmentScopeClient{
    private final RestClient c;
    private final String token;
    public EnrollmentScopeClient(RestClient.Builder b,@Value("${enrollment-service.url:http://localhost:8084}")String url,@Value("${lmspilot.internal-token}")String t){
        c=b.baseUrl(url).build();
        token=t;
    }
    public Set<UUID>assignedDeliveryIds(UUID u){
        try{
            String[]a=c.get().uri("/internal/v1/course-access/instructors/{userId}/delivery-ids",u).header("X-Service-Token",token).retrieve().body(String[].class);
            Set<UUID>r=new HashSet<>();
            if(a!=null)for(String s:a)r.add(UUID.fromString(s));
            return r;
        }
        catch(Exception e){
            return Set.of();
        }

    }

}
