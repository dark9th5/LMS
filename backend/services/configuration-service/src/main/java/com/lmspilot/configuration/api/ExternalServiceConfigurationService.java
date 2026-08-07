package com.lmspilot.configuration.api;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.configuration.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.net.*;

import java.time.Instant;

import java.util.*;

import javax.net.ssl.*;
@Service
public class ExternalServiceConfigurationService{
    private final ExternalServiceConfigRepository repo;
    private final ObjectMapper mapper;
    private final ConfigurationSecretCipher cipher;
    public ExternalServiceConfigurationService(ExternalServiceConfigRepository r,ObjectMapper m,ConfigurationSecretCipher c){
        repo=r;
        mapper=m;
        cipher=c;
    }
    @Transactional(readOnly=true)
    public List<ExternalServiceResponse>list(){
        return repo.findAllByOrderByServiceTypeAscConfigKeyAsc().stream().map(this::response).toList();
    }
    @Transactional
    public ExternalServiceResponse save(UUID id,ExternalServiceRequest i){
        validateConfig(i.serviceType(),i.config());
        String key=i.configKey().trim().toLowerCase();
        ExternalServiceConfigEntity e=id!=null?repo.findById(id).orElseThrow(this::notFound):repo.findByServiceTypeAndConfigKey(i.serviceType(),key);
        if(e==null)e=new ExternalServiceConfigEntity(i.serviceType(),key);
        e.setServiceType(i.serviceType());
        e.setConfigKey(key);
        e.setEnabled(i.enabled());
        e.setConfigJson(write(i.config()));
        if(i.secret()!=null){
            e.setEncryptedSecret(i.secret().isEmpty()?null:cipher.encrypt(i.secret()));
            e.setSecretKeyVersion(e.getEncryptedSecret()==null?null:1);
        }
        e.setHealthStatus(ExternalServiceHealth.UNKNOWN);
        e.setLastError(null);
        e.setLastCheckedAt(null);
        e.setUpdatedBy(CurrentUser.id());
        e.setUpdatedAt(Instant.now());
        return response(repo.save(e));
    }
    @Transactional
    public ExternalServiceResponse test(UUID id){
        var e=repo.findById(id).orElseThrow(this::notFound);
        Map<String,Object>cfg=read(e.getConfigJson());
        String secret=e.getEncryptedSecret()==null?null:cipher.decrypt(e.getEncryptedSecret());
        try{
            e.setHealthStatus(health(e.getServiceType(),cfg,secret));
            e.setLastError(null);
        }
        catch(Exception x){
            e.setHealthStatus(x instanceof ApiException||x instanceof IllegalArgumentException?ExternalServiceHealth.MISCONFIGURED:ExternalServiceHealth.UNREACHABLE);
            e.setLastError(x.getMessage()==null?null:x.getMessage().substring(0,Math.min(1000,x.getMessage().length())));
        }
        e.setLastCheckedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return response(e);
    }
    private String text(Map<String,Object>c,String k,String label){
        Object v=c.get(k);
        if(v==null||v.toString().trim().isBlank())throw new ApiException(HttpStatus.BAD_REQUEST,"EXTERNAL_SERVICE_CONFIG_INVALID","Thiếu "+label);
        return v.toString().trim();
    }
    private int port(Map<String,Object>c,int fallback){
        String r=String.valueOf(c.getOrDefault("port","")).trim();
        if(r.isBlank())return fallback;
        try{
            int p=Integer.parseInt(r);
            if(p<1||p>65535)throw new Exception();
            return p;
        }
        catch(Exception x){
            throw new ApiException(HttpStatus.BAD_REQUEST,"EXTERNAL_SERVICE_CONFIG_INVALID","Cổng kết nối phải nằm trong khoảng 1-65535");
        }

    }
    private URI endpoint(Map<String,Object>c){
        Object raw=c.containsKey("endpoint")?c.get("endpoint"):c.get("baseUrl");
        try{
            URI u=URI.create(raw==null?"":raw.toString().trim());
            if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getHost().isBlank())throw new Exception();
            return u;
        }
        catch(Exception x){
            throw new ApiException(HttpStatus.BAD_REQUEST,"EXTERNAL_SERVICE_CONFIG_INVALID","Endpoint phải là URL HTTP/HTTPS đầy đủ");
        }

    }
    private void validateConfig(ExternalServiceType t,Map<String,Object>c){
        switch(t){
            case REDIS-> {
                text(c,"host","máy chủ Redis");
                port(c,6379);
            }
            case SMTP->{
                text(c,"host","máy chủ SMTP");
                port(c,587);
                text(c,"fromEmail","email gửi");
            }
            case AI_PROVIDER->{
                endpoint(c);
                text(c,"model","model AI");
            }
            case OBJECT_STORAGE->{
                endpoint(c);
                text(c,"bucket","bucket");
                text(c,"region","region");
                text(c,"accessKey","access key");
            }
            case DOCUMENT_EDITOR->{
                endpoint(c);
                URI u=URI.create(text(c,"callbackUrl","callback URL"));
                if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null)throw new ApiException(HttpStatus.BAD_REQUEST,"EXTERNAL_SERVICE_CONFIG_INVALID","Callback URL phải là URL HTTP/HTTPS đầy đủ");
            }
            case VIDEO_CONFERENCE->endpoint(c);
        }

    }
    private ExternalServiceHealth health(ExternalServiceType t,Map<String,Object>c,String secret)throws Exception{
        if(t==ExternalServiceType.REDIS||t==ExternalServiceType.SMTP){
            String host=text(c,"host",t==ExternalServiceType.REDIS?"máy chủ Redis":"máy chủ SMTP");
            int p=port(c,t==ExternalServiceType.REDIS?6379:587);
            boolean tls=t==ExternalServiceType.REDIS?Boolean.parseBoolean(String.valueOf(c.get("tls"))):"TLS".equalsIgnoreCase(String.valueOf(c.get("security")));
            Socket s=tls?SSLSocketFactory.getDefault().createSocket():new Socket();
            try(s){
                s.connect(new InetSocketAddress(host,p),3000);
                if(s instanceof SSLSocket ssl)ssl.startHandshake();
            }
            return ExternalServiceHealth.HEALTHY;
        }
        String base=endpoint(c).toString().replaceAll("/+$","");
        String target=switch(t){
            case DOCUMENT_EDITOR->base+"/healthcheck";
            case AI_PROVIDER->base+"/models";
            case OBJECT_STORAGE->base+"/"+text(c,"bucket","bucket");
            default->base;
        }
        ;
        HttpURLConnection con=(HttpURLConnection)URI.create(target).toURL().openConnection();
        con.setConnectTimeout(4000);
        con.setReadTimeout(4000);
        con.setRequestMethod(t==ExternalServiceType.OBJECT_STORAGE?"HEAD":"GET");
        if(t==ExternalServiceType.AI_PROVIDER&&secret!=null&&!secret.isBlank())con.setRequestProperty("Authorization","Bearer "+secret);
        int status=con.getResponseCode();
        return status>=200&&status<=499?ExternalServiceHealth.HEALTHY:ExternalServiceHealth.DEGRADED;
    }
    private ExternalServiceResponse response(ExternalServiceConfigEntity e){
        return new ExternalServiceResponse(e.getId(),e.getServiceType(),e.getConfigKey(),e.isEnabled(),read(e.getConfigJson()),e.getEncryptedSecret()!=null,e.getHealthStatus(),e.getLastCheckedAt(),e.getLastError(),e.getUpdatedAt());
    }
    private Map<String,Object>read(String s){
        try{
            return mapper.readValue(s,new TypeReference<>(){
            }
            );
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }

    }
    private String write(Object o){
        try{
            return mapper.writeValueAsString(o);
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }

    }
    private ApiException notFound(){
        return new ApiException(HttpStatus.NOT_FOUND,"EXTERNAL_SERVICE_NOT_FOUND","Không tìm thấy cấu hình dịch vụ");
    }

}
