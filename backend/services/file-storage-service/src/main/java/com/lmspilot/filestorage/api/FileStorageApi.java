package com.lmspilot.filestorage.api;

import com.lmspilot.filestorage.domain.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.CurrentUser;

import jakarta.annotation.PostConstruct;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.nio.file.*;

import java.security.*;

import java.time.*;

import java.util.*;

import java.util.zip.*;

import javax.xml.parsers.*;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.*;

import org.springframework.http.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import org.w3c.dom.*;
record StoredFileResponse(UUID id,String originalName,String contentType,long sizeBytes,String sha256,String purpose,StoredFileStatus status,Instant createdAt){
}
record DocumentPreviewResponse(UUID fileId,String originalName,List<String> paragraphs){
}
record InternalStoredFileResponse(UUID id,UUID ownerId,String originalName,String contentType,long sizeBytes,String sha256,String purpose,StoredFileStatus status,Instant createdAt){
}
record GrantFileAccessRequest(UUID fileId,UUID userId,String source,Instant expiresAt){
}
@Service
@Transactional
class FileStorageService {
    private final StoredFileRepository repo;
    private final FileAccessGrantRepository grants;
    private final FileVersionRepository versions;
    private final Path root;
    private final long max;
    private final Set<String> allowed;
    private static final Set<String> BLOCKED=Set.of("exe","dll","bat","cmd","ps1","sh","jar","msi","com","scr","js","html","htm","svg");
    public FileStorageService(StoredFileRepository r,FileAccessGrantRepository g,FileVersionRepository v,@Value("${storage.root:./data/files}")String root,@Value("${storage.max-size-bytes:209715200}")long max,@Value("${storage.allowed-content-types:application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,video/mp4,audio/mpeg,image/png,image/jpeg,image/webp,text/plain,text/csv}")String types){
        repo=r;
        grants=g;
        versions=v;
        this.root=Paths.get(root).toAbsolutePath().normalize();
        this.max=max;
        allowed=new HashSet<>(Arrays.asList(types.toLowerCase(Locale.ROOT).split(",")));
    }
    @PostConstruct
    public void init()throws IOException{
        Files.createDirectories(root);
        if(max<=0)throw new IllegalStateException("storage.max-size-bytes must be positive");
    }
    StoredFileResponse store(MultipartFile file,String purpose){
        if(file==null||file.isEmpty())bad("FILE_EMPTY","Tệp tải lên đang trống");
        if(file.getSize()>max)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"FILE_TOO_LARGE","Tệp vượt quá dung lượng cho phép");
        String name=safe(file.getOriginalFilename());
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
        if(ext.isBlank()||BLOCKED.contains(ext))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"FILE_TYPE_BLOCKED","Loại tệp bị chặn");
        String content=file.getContentType()==null?"application/octet-stream":file.getContentType().toLowerCase(Locale.ROOT);
        if(!allowed.contains(content))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"FILE_TYPE_NOT_ALLOWED","Loại nội dung không được cho phép");
        String pur=(purpose==null?"GENERAL":purpose.trim().toUpperCase(Locale.ROOT));
        if(pur.length()>50)bad("PURPOSE_INVALID","Mục đích tệp quá dài");
        UUID id=UUID.randomUUID();
        String key=id.toString().substring(0,2)+"/"+id;
        Path target=path(key);
        try{
            Files.createDirectories(target.getParent());
            byte[] bytes=file.getBytes();
            Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            StoredFileEntity e=new StoredFileEntity();
            e.id=id;
            e.ownerId=user();
            e.originalName=name;
            e.storageKey=key;
            e.contentType=content;
            e.sizeBytes=bytes.length;
            e.sha256=sha(bytes);
            e.purpose=pur;
            repo.save(e);
            FileVersionEntity ver=new FileVersionEntity();
            ver.fileId=e.id;
            ver.storageKey=key;
            ver.mediaType=content;
            ver.sizeBytes=e.sizeBytes;
            ver.sha256=e.sha256;
            ver.createdBy=e.ownerId;
            versions.save(ver);
            return view(e);
        }
        catch(IOException x){
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"FILE_STORE_FAILED","Không thể lưu tệp");
        }

    }
    List<StoredFileResponse> list(){
        return repo.findAllByOwnerIdOrderByCreatedAtDesc(user()).stream().filter(e->e.status!=StoredFileStatus.DELETED).map(this::view).toList();
    }
    StoredFileResponse metadata(UUID id){
        return view(readable(id));
    }
    InternalStoredFileResponse internalMetadata(UUID id){
        StoredFileEntity e=require(id);
        return new InternalStoredFileResponse(e.id,e.ownerId,e.originalName,e.contentType,e.sizeBytes,e.sha256,e.purpose,e.status,e.createdAt);
    }
    ResponseEntity<Resource> content(UUID id,String range){
        StoredFileEntity e=readable(id);
        return resource(e);
    }
    ResponseEntity<Resource> internalContent(UUID id){
        return resource(require(id));
    }
    DocumentPreviewResponse preview(UUID id){
        StoredFileEntity e=readable(id);
        if(!e.contentType.contains("wordprocessingml"))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"DOCX_REQUIRED","Tệp không phải DOCX");
        List<String> p=new ArrayList<>();
        try(ZipFile z=new ZipFile(path(e.storageKey).toFile())){
            ZipEntry entry=z.getEntry("word/document.xml");
            if(entry==null)return new DocumentPreviewResponse(id,e.originalName,List.of());
            DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            f.setFeature("http://xml.org/sax/features/external-general-entities",false);
            Document d=f.newDocumentBuilder().parse(z.getInputStream(entry));
            NodeList nodes=d.getElementsByTagNameNS("*","p");
            for(int i=0;
            i<nodes.getLength();
            i++){
                String s=nodes.item(i).getTextContent().trim();
                if(!s.isBlank())p.add(s);
            }

        }
        catch(Exception x){
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCX_PREVIEW_FAILED","Không thể đọc nội dung DOCX");
        }
        return new DocumentPreviewResponse(id,e.originalName,p);
    }
    void delete(UUID id){
        StoredFileEntity e=readable(id);
        e.status=StoredFileStatus.DELETED;
        e.deletedAt=Instant.now();
        repo.save(e);
    }
    void grant(GrantFileAccessRequest i){
        FileAccessGrantEntity g=grants.findByFileIdAndUserId(i.fileId(),i.userId()).orElseGet(FileAccessGrantEntity::new);
        g.fileId=i.fileId();
        g.userId=i.userId();
        g.source=i.source()==null?"INTERNAL":i.source();
        g.expiresAt=i.expiresAt()==null?Instant.now().plus(Duration.ofDays(30)):i.expiresAt();
        g.updatedAt=Instant.now();
        grants.save(g);
    }
    StoredFileEntity require(UUID id){
        return repo.findById(id).filter(e->e.status==StoredFileStatus.AVAILABLE).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"FILE_NOT_FOUND","Không tìm thấy tệp"));
    }
    Path filePath(StoredFileEntity e){
        return path(e.storageKey);
    }
    private StoredFileEntity readable(UUID id){
        StoredFileEntity e=require(id);
        UUID u=user();
        if(!e.ownerId.equals(u)){
            boolean ok=grants.findByFileIdAndUserId(id,u).filter(g->g.expiresAt.isAfter(Instant.now())).isPresent();
            if(!ok)throw new ApiException(HttpStatus.FORBIDDEN,"FILE_READ_FORBIDDEN","Không có quyền đọc tệp");
        }
        return e;
    }
    private ResponseEntity<Resource> resource(StoredFileEntity e){
        FileSystemResource r=new FileSystemResource(path(e.storageKey));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(e.contentType)).contentLength(e.sizeBytes).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(e.originalName,StandardCharsets.UTF_8).build().toString()).body(r);
    }
    private Path path(String key){
        Path p=root.resolve(key).normalize();
        if(!p.startsWith(root))bad("INVALID_PATH","Đường dẫn không hợp lệ");
        return p;
    }
    private String safe(String n){
        String s=n==null?"upload.bin":Paths.get(n).getFileName().toString();
        return s.replaceAll("[\\r\\n\\0]","_").substring(0,Math.min(240,s.length()));
    }
    private String sha(byte[] b){
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }
    private StoredFileResponse view(StoredFileEntity e){
        return new StoredFileResponse(e.id,e.originalName,e.contentType,e.sizeBytes,e.sha256,e.purpose,e.status,e.createdAt);
    }
    private UUID user(){
        try{
            return CurrentUser.id();
        }
        catch(Exception e){
            return new UUID(0,1);
        }

    }
    private void bad(String c,String m){
        throw new ApiException(HttpStatus.BAD_REQUEST,c,m);
    }

}
@RestController
@RequestMapping("/api/v1/files")
public class FileStorageApi {
    private final FileStorageService service;
    public FileStorageApi(FileStorageService s){
        service=s;
    }
    @GetMapping
    public List<StoredFileResponse> list(){
        return service.list();
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StoredFileResponse upload(@RequestPart("file")MultipartFile f,@RequestParam(defaultValue="GENERAL")String purpose){
        return service.store(f,purpose);
    }
    @GetMapping("/{id}")
    public StoredFileResponse metadata(@PathVariable UUID id){
        return service.metadata(id);
    }
    @GetMapping("/{id}/docx-preview")
    public DocumentPreviewResponse preview(@PathVariable UUID id){
        return service.preview(id);
    }
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID id,@RequestHeader(value="Range",required=false)String range){
        return service.content(id,range);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id){
        service.delete(id);
    }

}
@RestController
@RequestMapping("/internal/v1/files")
class InternalFileController {
    private final FileStorageService service;
    private final com.lmspilot.support.security.InternalTokenAuthorizer token;
    InternalFileController(FileStorageService s,com.lmspilot.support.security.InternalTokenAuthorizer t){
        service=s;
        token=t;
    }
    @GetMapping("/{id}/content") ResponseEntity<Resource> content(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false)String k){
        token.require(k);
        return service.internalContent(id);
    }
    @GetMapping("/{id}")
    InternalStoredFileResponse metadata(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false)String k){
        token.require(k);
        return service.internalMetadata(id);
    }
    @PostMapping("/access-grants")
    void grant(@RequestBody GrantFileAccessRequest i,@RequestHeader(value="X-Service-Token",required=false)String k){
        token.require(k);
        service.grant(i);
    }

}
