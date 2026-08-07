package com.lmspilot.filestorage.api;

import com.lmspilot.filestorage.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import jakarta.validation.Valid;

import jakarta.validation.constraints.*;

import java.nio.file.*;

import java.security.*;

import java.time.*;

import java.util.*;

import org.springframework.http.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
record FileVersionResponse(UUID id,UUID fileId,int versionNumber,String mediaType,long sizeBytes,String sha256,FileVersionSource sourceType,UUID parentVersionId,String changeSummary,UUID createdBy,Instant createdAt){
}
record CreateEditSessionRequest(@NotNull FileEditorType editorType,@Min(5)
@Max(1440) Integer ttlMinutes){
}
record FileEditSessionResponse(UUID id,UUID fileId,UUID baseVersionId,FileEditorType editorType,String token,String editorUrl,Instant expiresAt,FileEditSessionStatus status){
}
record OnlyOfficeCallback(Integer status,String url,String key){
}
@Service
@Transactional
class FileEditingService {
    private final FileStorageService storage;
    private final StoredFileRepository files;
    private final FileVersionRepository versions;
    private final FileEditSessionRepository sessions;
    FileEditingService(FileStorageService s,StoredFileRepository f,FileVersionRepository v,FileEditSessionRepository e){
        storage=s;
        files=f;
        versions=v;
        sessions=e;
    }
    List<FileVersionResponse> listVersions(UUID id){
        storage.metadata(id);
        return versions.findAllByFileIdOrderByVersionNumberDesc(id).stream().map(this::view).toList();
    }
    FileEditSessionResponse createSession(UUID id,CreateEditSessionRequest i){
        StoredFileEntity f=storage.require(id);
        if(!f.ownerId.equals(user()))throw new ApiException(HttpStatus.FORBIDDEN,"FILE_EDIT_FORBIDDEN","Không có quyền sửa tệp");
        FileVersionEntity base=versions.findFirstByFileIdOrderByVersionNumberDesc(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"FILE_VERSION_NOT_FOUND","Không tìm thấy phiên bản"));
        String token=random();
        FileEditSessionEntity e=new FileEditSessionEntity();
        e.fileId=id;
        e.baseVersionId=base.id;
        e.editorType=i.editorType();
        e.userId=user();
        e.lockTokenHash=sha(token.getBytes());
        e.expiresAt=Instant.now().plus(Duration.ofMinutes(i.ttlMinutes()==null?60:i.ttlMinutes()));
        sessions.save(e);
        return new FileEditSessionResponse(e.id,e.fileId,e.baseVersionId,e.editorType,token,"/public/v1/file-edit/"+e.id+"/content?token="+token,e.expiresAt,e.status);
    }
    FileVersionResponse savePdf(UUID id,MultipartFile file,String summary){
        FileEditSessionEntity s=owned(id);
        if(s.editorType!=FileEditorType.PDF_ANNOTATOR)throw new ApiException(HttpStatus.CONFLICT,"EDITOR_TYPE_MISMATCH","Phiên không dùng trình chú thích PDF");
        if(!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType()))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"PDF_REQUIRED","Chỉ nhận PDF");
        try{
            return save(s,file.getBytes(),MediaType.APPLICATION_PDF_VALUE,FileVersionSource.PDF_ANNOTATION,summary);
        }
        catch(Exception e){
            if(e instanceof ApiException a)throw a;
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"FILE_EDIT_SAVE_FAILED","Không thể lưu phiên bản");
        }

    }
    void cancel(UUID id){
        FileEditSessionEntity s=owned(id);
        s.status=FileEditSessionStatus.CANCELLED;
        s.closedAt=Instant.now();
        sessions.save(s);
    }
    ResponseEntity<org.springframework.core.io.Resource> publicContent(UUID id,String token){
        FileEditSessionEntity s=token(id,token);
        return storage.internalContent(s.fileId);
    }
    Map<String,Object> callback(UUID id,String token,OnlyOfficeCallback p){
        FileEditSessionEntity s=token(id,token);
        if(p.status()!=null&&Set.of(2,6).contains(p.status())){
            s.status=FileEditSessionStatus.SAVED;
            s.closedAt=Instant.now();
            sessions.save(s);
        }
        return Map.of("error",0);
    }
    private FileVersionResponse save(FileEditSessionEntity s,byte[] bytes,String media,FileVersionSource source,String summary)throws Exception{
        StoredFileEntity f=storage.require(s.fileId);
        FileVersionEntity parent=versions.findFirstByFileIdOrderByVersionNumberDesc(s.fileId).orElseThrow();
        FileVersionEntity v=new FileVersionEntity();
        v.fileId=s.fileId;
        v.versionNumber=parent.versionNumber+1;
        v.storageKey="versions/"+s.fileId+"/"+v.id;
        v.mediaType=media;
        v.sizeBytes=bytes.length;
        v.sha256=sha(bytes);
        v.sourceType=source;
        v.parentVersionId=parent.id;
        v.changeSummary=summary;
        v.createdBy=s.userId;
        Path target=storage.filePath(newFile(v.storageKey));
        Files.createDirectories(target.getParent());
        Files.write(target,bytes);
        versions.save(v);
        f.storageKey=v.storageKey;
        f.contentType=media;
        f.sizeBytes=v.sizeBytes;
        f.sha256=v.sha256;
        files.save(f);
        s.status=FileEditSessionStatus.SAVED;
        s.closedAt=Instant.now();
        sessions.save(s);
        return view(v);
    }
    private StoredFileEntity newFile(String key){
        StoredFileEntity e=new StoredFileEntity();
        e.storageKey=key;
        return e;
    }
    private FileEditSessionEntity owned(UUID id){
        FileEditSessionEntity s=sessions.findById(id).orElseThrow(this::nf);
        if(!s.userId.equals(user()))throw new ApiException(HttpStatus.FORBIDDEN,"EDIT_SESSION_OWNER_MISMATCH","Phiên không thuộc người dùng");
        if(s.status!=FileEditSessionStatus.OPEN||!s.expiresAt.isAfter(Instant.now()))throw new ApiException(HttpStatus.GONE,"EDIT_SESSION_CLOSED","Phiên đã đóng");
        return s;
    }
    private FileEditSessionEntity token(UUID id,String t){
        FileEditSessionEntity s=sessions.findById(id).orElseThrow(this::nf);
        if(!MessageDigest.isEqual(s.lockTokenHash.getBytes(),sha(t.getBytes()).getBytes()))throw new ApiException(HttpStatus.UNAUTHORIZED,"EDIT_TOKEN_INVALID","Mã phiên không hợp lệ");
        if(s.status!=FileEditSessionStatus.OPEN||!s.expiresAt.isAfter(Instant.now()))throw new ApiException(HttpStatus.GONE,"EDIT_SESSION_CLOSED","Phiên đã đóng");
        return s;
    }
    private ApiException nf(){
        return new ApiException(HttpStatus.NOT_FOUND,"EDIT_SESSION_NOT_FOUND","Không tìm thấy phiên chỉnh sửa");
    }
    private String random(){
        return UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");
    }
    private String sha(byte[] b){
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }
    private FileVersionResponse view(FileVersionEntity v){
        return new FileVersionResponse(v.id,v.fileId,v.versionNumber,v.mediaType,v.sizeBytes,v.sha256,v.sourceType,v.parentVersionId,v.changeSummary,v.createdBy,v.createdAt);
    }
    private UUID user(){
        try{
            return CurrentUser.id();
        }
        catch(Exception e){
            return new UUID(0,1);
        }

    }

}
@RestController
@RequestMapping("/api/v1/files")
public class FileEditingApi {
    private final FileEditingService service;
    public FileEditingApi(FileEditingService s){
        service=s;
    }
    @GetMapping("/{id}/versions")
    public List<FileVersionResponse> versions(@PathVariable UUID id){
        return service.listVersions(id);
    }
    @PostMapping("/{id}/edit-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public FileEditSessionResponse create(@PathVariable UUID id,@Valid
    @RequestBody CreateEditSessionRequest i){
        return service.createSession(id,i);
    }
    @PostMapping(value="/edit-sessions/{id}/pdf",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileVersionResponse save(@PathVariable UUID id,@RequestPart("file")MultipartFile f,@RequestParam(required=false)String changeSummary){
        return service.savePdf(id,f,changeSummary);
    }
    @DeleteMapping("/edit-sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id){
        service.cancel(id);
    }

}
@RestController
@RequestMapping("/public/v1/file-edit")
class PublicFileEditingController {
    private final FileEditingService service;
    PublicFileEditingController(FileEditingService s){
        service=s;
    }
    @GetMapping("/{id}/content") ResponseEntity<org.springframework.core.io.Resource> content(@PathVariable UUID id,@RequestParam String token){
        return service.publicContent(id,token);
    }
    @PostMapping("/{id}/callback")
    Map<String,Object> callback(@PathVariable UUID id,@RequestParam String token,@RequestBody OnlyOfficeCallback p){
        return service.callback(id,token,p);
    }

}
