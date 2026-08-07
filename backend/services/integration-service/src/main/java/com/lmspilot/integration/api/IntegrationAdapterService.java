package com.lmspilot.integration.api;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.integration.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.LicenseGuard;

import java.net.*;

import java.nio.file.*;

import java.time.Instant;

import java.util.*;

import javax.net.ssl.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClient;
@Service
public class IntegrationAdapterService{
    private final IntegrationAdapterRepository repo;
    private final ObjectMapper mapper;
    private final LicenseGuard license;
    public IntegrationAdapterService(IntegrationAdapterRepository r,ObjectMapper m,LicenseGuard l){
        repo=r;
        mapper=m;
        license=l;
    }
    @Transactional(readOnly=true)
    public List<AdapterResponse> list(){
        return repo.findAll().stream().map(this::response).toList();
    }
    @Transactional
    public AdapterResponse save(UUID id,AdapterRequest in){
        license.requireFeature(featureFor(in.type()));
        validateEndpoint(in.endpoint());
        IntegrationAdapterEntity e=id==null?new IntegrationAdapterEntity():repo.findById(id).orElseThrow(this::notFound);
        IntegrationAdapterEntity duplicate=repo.findByCode(in.code().trim());
        if(duplicate!=null&&!duplicate.getId().equals(e.getId()))throw new ApiException(HttpStatus.CONFLICT,"ADAPTER_CODE_EXISTS","Mã adapter đã tồn tại");
        e.setCode(in.code().trim());
        e.setName(in.name().trim());
        e.setType(in.type());
        e.setEndpoint(in.endpoint().trim());
        try{
            e.setMappingJson(mapper.writeValueAsString(in.mapping()));
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }
        e.setSecretReference(in.secretReference().trim());
        e.setStatus(in.status());
        e.setUpdatedAt(Instant.now());
        return response(repo.save(e));
    }
    @Transactional
    public AdapterResponse test(UUID id){
        IntegrationAdapterEntity e=repo.findById(id).orElseThrow(this::notFound);
        license.requireFeature(featureFor(e.getType()),false);
        String result;
        try{
            result="Kết nối thành công: "+testEndpoint(e.getEndpoint());
        }
        catch(Exception x){
            String m=x.getMessage()==null?x.getClass().getSimpleName():x.getMessage();
            result="Kết nối thất bại: "+m.substring(0,Math.min(1000,m.length()));
        }
        e.setLastTestedAt(Instant.now());
        e.setLastTestResult(result);
        if(result.startsWith("Kết nối thất bại"))e.setStatus(AdapterStatus.ERROR);
        return response(e);
    }
    private String testEndpoint(String value)throws Exception{
        URI uri=validatedUri(value);
        return switch(uri.getScheme().toLowerCase()){
            case "http","https"->{
                var response=RestClient.create().get().uri(uri).retrieve().toBodilessEntity();
                yield "HTTP "+response.getStatusCode().value();
            }
            case "ldap","smtp"->probeSocket(uri,false);
            case "ldaps","smtps"->probeSocket(uri,true);
            case "file"->{
                Path path=Paths.get(uri).normalize();
                if(!path.isAbsolute())throw new IllegalArgumentException("Đường dẫn file phải là đường dẫn tuyệt đối");
                if(!Files.exists(path))throw new IllegalArgumentException("Đường dẫn không tồn tại: "+path);
                yield Files.isDirectory(path)?"Thư mục có thể truy cập: "+path:"Tệp có thể truy cập: "+path;
            }
            default->throw new IllegalStateException("Giao thức endpoint chưa được hỗ trợ");
        }
        ;
    }
    private String probeSocket(URI uri,boolean secure)throws Exception{
        String host=uri.getHost();
        if(host==null||host.isBlank())throw new IllegalArgumentException("Endpoint thiếu host");
        int port=uri.getPort()>0?uri.getPort():switch(uri.getScheme().toLowerCase()){
            case"ldap"->389;
            case"ldaps"->636;
            case"smtp"->25;
            case"smtps"->465;
            default->throw new IllegalStateException("Endpoint thiếu port");
        }
        ;
        try(Socket socket=secure?SSLSocketFactory.getDefault().createSocket():new Socket()){
            socket.connect(new InetSocketAddress(host,port),3000);
            socket.setSoTimeout(3000);
            if(secure&&socket instanceof SSLSocket ssl)ssl.startHandshake();
        }
        return(secure?"TLS ":"TCP ")+host+":"+port;
    }
    private void validateEndpoint(String v){
        validatedUri(v);
    }
    private URI validatedUri(String value){
        URI uri;
        try{
            uri=URI.create(value.trim());
        }
        catch(Exception e){
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ENDPOINT","Endpoint không hợp lệ");
        }
        String scheme=uri.getScheme()==null?null:uri.getScheme().toLowerCase();
        if(!Set.of("http","https","ldap","ldaps","smtp","smtps","file").contains(scheme))throw new ApiException(HttpStatus.BAD_REQUEST,"UNSUPPORTED_ENDPOINT","Giao thức endpoint chưa được hỗ trợ");
        if(!"file".equals(scheme)&&(uri.getHost()==null||uri.getHost().isBlank()))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ENDPOINT","Endpoint phải có host");
        if(uri.getUserInfo()!=null&&!uri.getUserInfo().isBlank())throw new ApiException(HttpStatus.BAD_REQUEST,"CREDENTIALS_NOT_ALLOWED","Không đặt thông tin xác thực trực tiếp trong endpoint");
        return uri;
    }
    private String featureFor(AdapterType t){
        return t==AdapterType.LDAP||t==AdapterType.ACTIVE_DIRECTORY?"LDAP":"INTEGRATIONS";
    }
    private ApiException notFound(){
        return new ApiException(HttpStatus.NOT_FOUND,"ADAPTER_NOT_FOUND","Không tìm thấy adapter");
    }
    private AdapterResponse response(IntegrationAdapterEntity e){
        Map<String,String> mapping;
        try{
            mapping=mapper.readValue(e.getMappingJson(),new TypeReference<>(){
            }
            );
        }
        catch(Exception x){
            throw new IllegalStateException(x);
        }
        return new AdapterResponse(e.getId(),e.getCode(),e.getName(),e.getType(),e.getEndpoint(),mapping,!e.getSecretReference().isBlank(),e.getStatus(),e.getLastTestedAt(),e.getLastTestResult());
    }

}
