package com.lmspilot.ai.api;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.node.*;

import com.lmspilot.ai.platform.*;

import static com.lmspilot.ai.platform.QuestionGenerationContracts.*;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.security.*;

import jakarta.validation.Valid;

import jakarta.validation.constraints.*;

import java.nio.charset.StandardCharsets;

import java.security.*;

import java.time.*;

import java.util.*;

import javax.crypto.*;

import javax.crypto.spec.*;

import org.apache.tika.Tika;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestClient;
record AiProviderRequest(@NotBlank
@Size(max=80)String code,AiProviderType providerType,@NotBlank
@Size(max=1000)String baseUrl,@NotBlank
@Size(max=240)String model,Boolean enabled,String apiKey,@Min(5)
@Max(3600)Integer requestTimeoutSeconds,@Min(1)Integer maxOutputTokens,Map<String,Object> config){
}
record AiProviderResponse(UUID id,String code,AiProviderType providerType,String baseUrl,String model,boolean enabled,boolean apiKeyConfigured,int requestTimeoutSeconds,Integer maxOutputTokens,Map<String,Object> config,Instant updatedAt){
}
record GenerateQuestionSetRequest(UUID courseId,UUID providerConfigId,@Size(max=50)
Set<UUID> documentFileIds,@Size(max=120000)String sourceText,String language,@Min(1)
@Max(100)Integer numberOfQuestions,Set<String> questionTypes,Map<String,Integer> difficultyDistribution){
}
record ReviewQuestionSetRequest(ReviewDecision decision,@Size(max=5000)String comments,@Size(max=500)
Set<String> selectedExternalIds,JsonNode questionSet){
}
record QuestionGenerationJobResponse(UUID id,UUID courseId,UUID requestedBy,UUID providerConfigId,Set<UUID> documentFileIds,Map<String,Object> options,QuestionGenerationStatus status,JsonNode questionSet,List<ValidationProblem> validationProblems,String errorMessage,Instant createdAt,Instant updatedAt,Instant completedAt){
}
record AiSourceFileMetadata(UUID id,UUID ownerId,String originalName,String contentType,String purpose,String status){
}
record CourseDocumentScope(UUID courseId,Set<UUID> fileIds){
}
@Service
class AiSecretCipher{
    private final SecretKeySpec key;
    private final SecureRandom random=new SecureRandom();
    AiSecretCipher(@Value("${ai.secret-key:lmspilot-development-ai-secret-change-me}")String secret){
        try{
            key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),"AES");
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }
    byte[] encrypt(String value){
        try{
            byte[]nonce=new byte[12];
            random.nextBytes(nonce);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            byte[]enc=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[]out=Arrays.copyOf(nonce,nonce.length+enc.length);
            System.arraycopy(enc,0,out,nonce.length,enc.length);
            return out;
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }
    String decrypt(byte[]value){
        try{
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(value,0,12)));
            return new String(c.doFinal(Arrays.copyOfRange(value,12,value.length)),StandardCharsets.UTF_8);
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }

}
@Service
@Transactional
class AiProviderConfigurationService{
    private final AiProviderConfigRepository repo;
    private final ObjectMapper mapper;
    private final AiSecretCipher cipher;
    AiProviderConfigurationService(AiProviderConfigRepository r,ObjectMapper m,AiSecretCipher c){
        repo=r;
        mapper=m;
        cipher=c;
    }
    List<AiProviderResponse> list(){
        return repo.findAllByOrderByCodeAsc().stream().map(this::view).toList();
    }
    AiProviderResponse save(UUID id,AiProviderRequest i){
        AiProviderConfigEntity e=id==null?repo.findByCodeIgnoreCase(i.code()).orElseGet(AiProviderConfigEntity::new):repo.findById(id).orElseThrow(this::nf);
        e.code=i.code().trim().toUpperCase(Locale.ROOT);
        e.providerType=i.providerType()==null?AiProviderType.LOCAL_OPENAI_COMPATIBLE:i.providerType();
        e.baseUrl=i.baseUrl().trim().replaceAll("/+$","");
        e.model=i.model().trim();
        e.enabled=Boolean.TRUE.equals(i.enabled());
        e.requestTimeoutSeconds=i.requestTimeoutSeconds()==null?120:i.requestTimeoutSeconds();
        e.maxOutputTokens=i.maxOutputTokens();
        try{
            e.configJson=mapper.writeValueAsString(i.config()==null?Map.of():i.config());
        }
        catch(Exception x){
            throw new ApiException(HttpStatus.BAD_REQUEST,"AI_CONFIG_INVALID","Cấu hình AI không hợp lệ");
        }
        if(i.apiKey()!=null){
            e.encryptedApiKey=i.apiKey().isEmpty()?null:cipher.encrypt(i.apiKey());
            e.secretKeyVersion=e.encryptedApiKey==null?null:1;
        }
        e.updatedBy=user();
        e.updatedAt=Instant.now();
        return view(repo.save(e));
    }
    String apiKey(AiProviderConfigEntity e){
        return e.encryptedApiKey==null?null:cipher.decrypt(e.encryptedApiKey);
    }
    private AiProviderResponse view(AiProviderConfigEntity e){
        Map<String,Object>config;
        try{
            config=mapper.readValue(e.configJson,new TypeReference<>(){
            }
            );
        }
        catch(Exception x){
            config=Map.of();
        }
        return new AiProviderResponse(e.id,e.code,e.providerType,e.baseUrl,e.model,e.enabled,e.encryptedApiKey!=null,e.requestTimeoutSeconds,e.maxOutputTokens,config,e.updatedAt);
    }
    private ApiException nf(){
        return new ApiException(HttpStatus.NOT_FOUND,"AI_PROVIDER_NOT_FOUND","Không tìm thấy cấu hình AI");
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
@Service
class AiFileDocumentClient{
    private final RestClient client;
    private final String token;
    AiFileDocumentClient(RestClient.Builder b,@Value("${file-storage-service.url:http://localhost:8089}")String url,@Value("${lmspilot.internal-token}")String token){
        client=b.baseUrl(url).build();
        this.token=token;
    }
    byte[]content(UUID id){
        byte[]b=client.get().uri("/internal/v1/files/{id}/content",id).header("X-Service-Token",token).retrieve().body(byte[].class);
        if(b==null||b.length==0)throw new ApiException(HttpStatus.BAD_GATEWAY,"DOCUMENT_EMPTY","Tài liệu không có nội dung");
        return b;
    }
    AiSourceFileMetadata metadata(UUID id){
        AiSourceFileMetadata m=client.get().uri("/internal/v1/files/{id}",id).header("X-Service-Token",token).retrieve().body(AiSourceFileMetadata.class);
        if(m==null)throw new ApiException(HttpStatus.BAD_GATEWAY,"DOCUMENT_METADATA_MISSING","Không đọc được thông tin tài liệu");
        return m;
    }

}
@Service
class AssessmentQuestionImportClient{
    private final RestClient client;
    private final String token;
    AssessmentQuestionImportClient(RestClient.Builder b,@Value("${assessment-service.url:http://localhost:8086}")String url,@Value("${lmspilot.internal-token}")String token){
        client=b.baseUrl(url).build();
        this.token=token;
    }
    List<UUID> importQuestions(UUID owner,UUID course,JsonNode set){
        String[]r=client.post().uri("/internal/v1/questions/import-generated").header("X-Service-Token",token).body(Map.of("ownerId",owner,"courseId",course,"questionSet",set)).retrieve().body(String[].class);
        if(r==null)return List.of();
        return Arrays.stream(r).map(UUID::fromString).toList();
    }

}
@Service
@Transactional
class QuestionGenerationService{
    private final QuestionGenerationJobRepository jobs;
    private final QuestionGenerationReviewRepository reviews;
    private final AiProviderConfigRepository providers;
    private final AiProviderConfigurationService providerService;
    private final AiFileDocumentClient files;
    private final AssessmentQuestionImportClient importer;
    private final ObjectMapper mapper;
    private final Tika tika=new Tika();
    QuestionGenerationService(QuestionGenerationJobRepository j,QuestionGenerationReviewRepository r,AiProviderConfigRepository p,AiProviderConfigurationService ps,AiFileDocumentClient f,AssessmentQuestionImportClient i,ObjectMapper m){
        jobs=j;
        reviews=r;
        providers=p;
        providerService=ps;
        files=f;
        importer=i;
        mapper=m;
    }
    List<QuestionGenerationJobResponse> list(){
        return jobs.findAllByRequestedByOrderByCreatedAtDesc(user()).stream().map(this::view).toList();
    }
    QuestionGenerationJobResponse get(UUID id){
        return view(job(id));
    }
    QuestionGenerationJobResponse generate(GenerateQuestionSetRequest raw){
        GenerateQuestionSetRequest i=normalize(raw);
        if((i.documentFileIds()==null||i.documentFileIds().isEmpty())&&(i.sourceText()==null||i.sourceText().isBlank()))throw new ApiException(HttpStatus.BAD_REQUEST,"AI_SOURCE_REQUIRED","Cần ít nhất một tài liệu hoặc sourceText");
        DifficultyDistributionPolicy.normalize(i.difficultyDistribution());
        Set<String>types=i.questionTypes().stream().map(x->x.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        if(types.isEmpty()||!Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","TRUE_FALSE").containsAll(types))throw new ApiException(HttpStatus.BAD_REQUEST,"AI_QUESTION_TYPE_INVALID","Loại câu hỏi không hợp lệ");
        AiProviderConfigEntity provider=providers.findById(i.providerConfigId()).filter(x->x.enabled).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"AI_PROVIDER_DISABLED","Cấu hình AI chưa bật"));
        QuestionGenerationJobEntity job=new QuestionGenerationJobEntity();
        job.courseId=i.courseId();
        job.requestedBy=user();
        job.providerConfigId=provider.id;
        try{
            job.documentVersionIdsJson=mapper.writeValueAsString(i.documentFileIds());
            job.generationOptionsJson=mapper.writeValueAsString(Map.of("language",i.language(),"numberOfQuestions",i.numberOfQuestions(),"questionTypes",types,"difficultyDistribution",DifficultyDistributionPolicy.normalize(i.difficultyDistribution())));
        }
        catch(Exception ignored){
        }
        jobs.save(job);
        try{
            job.status=QuestionGenerationStatus.EXTRACTING;
            jobs.save(job);
            List<SourceChunk>chunks=extract(i.documentFileIds(),i.sourceText());
            job.status=QuestionGenerationStatus.GENERATING;
            jobs.save(job);
            JsonNode result=canonicalize(call(provider,i,chunks,List.of()),provider,i,chunks);
            job.status=QuestionGenerationStatus.VALIDATING;
            GenerateQuestionsCommand cmd=new GenerateQuestionsCommand(i.courseId(),chunks.stream().map(SourceChunk::documentVersionId).collect(java.util.stream.Collectors.toSet()),i.language(),i.numberOfQuestions(),i.difficultyDistribution(),types);
            QuestionSetValidationResult v=GeneratedQuestionQualityValidator.validate(result,cmd,chunks);
            if(!v.valid()){
                result=canonicalize(call(provider,i,chunks,v.problems()),provider,i,chunks);
                v=GeneratedQuestionQualityValidator.validate(result,cmd,chunks);
            }
            job.questionSetJson=mapper.writeValueAsString(result);
            job.validationErrorsJson=mapper.writeValueAsString(v.problems());
            job.status=v.valid()?QuestionGenerationStatus.REVIEW_REQUIRED:QuestionGenerationStatus.FAILED;
            job.errorMessage=v.valid()?null:"Kết quả AI không đạt số lượng, phân bố độ khó hoặc bằng chứng nguồn";
            job.completedAt=Instant.now();
            job.updatedAt=job.completedAt;
            return view(jobs.save(job));
        }
        catch(Exception x){
            job.status=QuestionGenerationStatus.FAILED;
            job.errorMessage=x.getMessage()==null?"Không thể sinh bộ câu hỏi":x.getMessage();
            job.completedAt=Instant.now();
            job.updatedAt=job.completedAt;
            return view(jobs.save(job));
        }

    }
    QuestionGenerationJobResponse review(UUID id,ReviewQuestionSetRequest input){
        QuestionGenerationJobEntity j=job(id);
        if(j.status!=QuestionGenerationStatus.REVIEW_REQUIRED&&j.status!=QuestionGenerationStatus.APPROVED)throw new ApiException(HttpStatus.CONFLICT,"GENERATION_NOT_REVIEWABLE","Tác vụ chưa sẵn sàng để duyệt");
        JsonNode set=input.questionSet()!=null?input.questionSet():readTree(j.questionSetJson);
        if(input.selectedExternalIds()!=null&&!input.selectedExternalIds().isEmpty()){
            ArrayNode selected=mapper.createArrayNode();
            for(JsonNode q:set.path("questions"))if(input.selectedExternalIds().contains(q.path("externalId").asText()))selected.add(q);((ObjectNode)set).set("questions",selected);
        }
        QuestionSetValidationResult v=QuestionSetBusinessValidator.validate(set);
        if(input.decision()==ReviewDecision.APPROVE&&!v.valid())throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"QUESTION_SET_INVALID","Bộ câu hỏi sau chỉnh sửa không hợp lệ");
        QuestionGenerationReviewEntity r=new QuestionGenerationReviewEntity();
        r.jobId=id;
        r.reviewerId=user();
        r.decision=input.decision();
        r.comments=input.comments();
        reviews.save(r);
        j.questionSetJson=write(set);
        j.validationErrorsJson=write(v.problems());
        j.status=switch(input.decision()){
            case APPROVE->QuestionGenerationStatus.APPROVED;
            case REJECT->QuestionGenerationStatus.FAILED;
            case REQUEST_CHANGES->QuestionGenerationStatus.REVIEW_REQUIRED;
        }
        ;
        j.updatedAt=Instant.now();
        return view(jobs.save(j));
    }
    Map<String,Object> importJob(UUID id){
        QuestionGenerationJobEntity j=job(id);
        if(j.status!=QuestionGenerationStatus.APPROVED)throw new ApiException(HttpStatus.CONFLICT,"GENERATION_NOT_APPROVED","Bộ câu hỏi chưa được duyệt");
        List<UUID>ids=importer.importQuestions(j.requestedBy,j.courseId,readTree(j.questionSetJson));
        j.status=QuestionGenerationStatus.IMPORTED;
        j.updatedAt=Instant.now();
        jobs.save(j);
        return Map.of("jobId",j.id,"questionIds",ids,"imported",ids.size());
    }
    private GenerateQuestionSetRequest normalize(GenerateQuestionSetRequest i){
        return new GenerateQuestionSetRequest(i.courseId(),i.providerConfigId(),i.documentFileIds()==null?Set.of():i.documentFileIds(),i.sourceText(),i.language()==null||i.language().isBlank()?"vi":i.language(),i.numberOfQuestions()==null?10:i.numberOfQuestions(),i.questionTypes()==null||i.questionTypes().isEmpty()?Set.of("SINGLE_CHOICE","TRUE_FALSE"):i.questionTypes(),i.difficultyDistribution()==null?Map.of():i.difficultyDistribution());
    }
    private List<SourceChunk>extract(Set<UUID>ids,String sourceText){
        List<SourceChunk>r=new ArrayList<>();
        if(ids!=null)for(UUID id:ids){
            AiSourceFileMetadata m=files.metadata(id);
            if(!Set.of("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain").contains(m.contentType()))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"AI_DOCUMENT_TYPE_UNSUPPORTED","AI chỉ hỗ trợ PDF, DOCX hoặc text");
            try{
                String text=tika.parseToString(new java.io.ByteArrayInputStream(files.content(id)));
                chunk(id,text,r);
            }
            catch(Exception e){
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENT_EXTRACT_FAILED","Không thể trích xuất tài liệu "+m.originalName());
            }

        }
        if(sourceText!=null&&!sourceText.isBlank())chunk(UUID.nameUUIDFromBytes(sourceText.getBytes(StandardCharsets.UTF_8)),sourceText,r);
        if(r.isEmpty())throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENT_TEXT_EMPTY","Tài liệu không có văn bản để sinh câu hỏi");
        return r;
    }
    private void chunk(UUID id,String text,List<SourceChunk>out){
        String n=text.replaceAll("[\\t\\r]+"," ").replaceAll("\\n{3,}","\n\n").trim();
        for(int start=0;
        start<n.length();
        start+=6000){
            int end=Math.min(n.length(),start+7000);
            out.add(new SourceChunk(id,null,"Đoạn "+(out.size()+1),n.substring(start,end)));
            if(end==n.length())break;
            start=end-6000;
        }

    }
    private JsonNode call(AiProviderConfigEntity p,GenerateQuestionSetRequest i,List<SourceChunk>chunks,List<ValidationProblem>problems){
        Map<String,Integer>expected=DifficultyDistributionPolicy.expectedCounts(i.numberOfQuestions(),i.difficultyDistribution());
        String correction=problems.isEmpty()?"":problems.stream().limit(30).map(x->x.path()+": "+x.message()).collect(java.util.stream.Collectors.joining("; "));
        String system="Bạn tạo câu hỏi cho LMSPilot. Chỉ trả JSON object schemaVersion 1.0. Không dùng kiến thức ngoài tài liệu. Tạo đúng "+i.numberOfQuestions()+" câu; EASY="+expected.get("EASY")+", MEDIUM="+expected.get("MEDIUM")+", HARD="+expected.get("HARD")+". Mỗi câu có options, correctOptionIds, explanation và citations quote nguyên văn. "+(correction.isBlank()?"":"Sửa lỗi lần trước: "+correction);
        String source=chunks.stream().map(c->"DOCUMENT_VERSION_ID="+c.documentVersionId()+"\nSECTION="+c.section()+"\n"+c.text()).collect(java.util.stream.Collectors.joining("\n---\n"));
        Map<String,Object>request=new LinkedHashMap<>();
        request.put("model",p.model);
        request.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content","Loại: "+i.questionTypes()+"; ngôn ngữ: "+i.language()+"\nTÀI LIỆU:\n"+source.substring(0,Math.min(700000,source.length())))));
        request.put("temperature",0.2);
        request.put("response_format",Map.of("type","json_object"));
        if(p.maxOutputTokens!=null)request.put("max_tokens",p.maxOutputTokens);
        RestClient.RequestBodySpec spec=RestClient.builder().baseUrl(p.baseUrl).build().post().uri("/chat/completions");
        String key=providerService.apiKey(p);
        if(key!=null&&!key.isBlank())spec.header("Authorization","Bearer "+key);
        JsonNode response=spec.body(request).retrieve().body(JsonNode.class);
        String content=response==null?null:response.path("choices").path(0).path("message").path("content").asText(null);
        if(content==null)throw new ApiException(HttpStatus.BAD_GATEWAY,"AI_INVALID_RESPONSE","Không tìm thấy nội dung trả về");
        content=content.trim().replaceFirst("^```json\\s*","").replaceFirst("^```\\s*","").replaceFirst("```$","").trim();
        try{
            return mapper.readTree(content);
        }
        catch(Exception e){
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"AI_INVALID_JSON","Kết quả AI không phải JSON hợp lệ");
        }

    }
    private JsonNode canonicalize(JsonNode generated,AiProviderConfigEntity p,GenerateQuestionSetRequest i,List<SourceChunk>chunks){
        if(!generated.isObject())return generated;
        ObjectNode root=(ObjectNode)generated.deepCopy();
        root.put("schemaVersion","1.0");
        root.put("language",i.language());
        ObjectNode src=mapper.createObjectNode();
        src.put("courseId",i.courseId().toString());
        ArrayNode docs=src.putArray("documentVersionIds");
        chunks.stream().map(SourceChunk::documentVersionId).distinct().forEach(x->docs.add(x.toString()));
        src.put("provider",p.providerType.name());
        src.put("model",p.model);
        src.put("generatedAt",Instant.now().toString());
        root.set("source",src);
        root.remove(List.of("courseId","generatedAt","sourceDocumentVersions"));
        for(JsonNode item:root.path("questions"))if(item instanceof ObjectNode q){
            if(!q.hasNonNull("stem")&&q.hasNonNull("prompt"))q.set("stem",q.path("prompt"));
            q.remove("prompt");
            JsonNode d=q.path("difficulty");
            if(d.isNumber())q.put("difficulty",d.asInt()<=2?"EASY":d.asInt()==3?"MEDIUM":"HARD");
            else q.put("difficulty",d.asText().toUpperCase(Locale.ROOT));
        }
        return root;
    }
    private QuestionGenerationJobEntity job(UUID id){
        QuestionGenerationJobEntity j=jobs.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"GENERATION_JOB_NOT_FOUND","Không tìm thấy tác vụ sinh câu hỏi"));
        if(!j.requestedBy.equals(user()))throw new ApiException(HttpStatus.FORBIDDEN,"GENERATION_JOB_OUT_OF_SCOPE","Tác vụ ngoài phạm vi");
        return j;
    }
    private QuestionGenerationJobResponse view(QuestionGenerationJobEntity j){
        Set<UUID>docs;
        Map<String,Object>opts;
        List<ValidationProblem>problems;
        try{
            docs=mapper.readValue(j.documentVersionIdsJson,new TypeReference<>(){
            }
            );
            opts=mapper.readValue(j.generationOptionsJson,new TypeReference<>(){
            }
            );
            problems=j.validationErrorsJson==null?List.of():mapper.readValue(j.validationErrorsJson,new TypeReference<>(){
            }
            );
        }
        catch(Exception e){
            docs=Set.of();
            opts=Map.of();
            problems=List.of();
        }
        return new QuestionGenerationJobResponse(j.id,j.courseId,j.requestedBy,j.providerConfigId,docs,opts,j.status,j.questionSetJson==null?null:readTree(j.questionSetJson),problems,j.errorMessage,j.createdAt,j.updatedAt,j.completedAt);
    }
    private JsonNode readTree(String s){
        try{
            return mapper.readTree(s);
        }
        catch(Exception e){
            return mapper.createObjectNode();
        }

    }
    private String write(Object o){
        try{
            return mapper.writeValueAsString(o);
        }
        catch(Exception e){
            return "{}";
        }

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
@RequestMapping("/api/v1/ai")
public class QuestionGenerationApi{
    private final AiProviderConfigurationService providers;
    private final QuestionGenerationService generation;
    public QuestionGenerationApi(AiProviderConfigurationService p,QuestionGenerationService g){
        providers=p;
        generation=g;
    }
    @GetMapping("/providers")
    public List<AiProviderResponse>providers(){
        return providers.list();
    }
    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    public AiProviderResponse create(@Valid
    @RequestBody AiProviderRequest i){
        return providers.save(null,i);
    }
    @PutMapping("/providers/{id}")
    public AiProviderResponse update(@PathVariable UUID id,@Valid
    @RequestBody AiProviderRequest i){
        return providers.save(id,i);
    }
    @GetMapping("/question-generation-jobs")
    public List<QuestionGenerationJobResponse>jobs(){
        return generation.list();
    }
    @GetMapping("/question-generation-jobs/{id}")
    public QuestionGenerationJobResponse job(@PathVariable UUID id){
        return generation.get(id);
    }
    @PostMapping("/question-generation-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionGenerationJobResponse generate(@Valid
    @RequestBody GenerateQuestionSetRequest i){
        return generation.generate(i);
    }
    @PostMapping("/question-generation-jobs/{id}/review")
    public QuestionGenerationJobResponse review(@PathVariable UUID id,@Valid
    @RequestBody ReviewQuestionSetRequest i){
        return generation.review(id,i);
    }
    @PostMapping("/question-generation-jobs/{id}/import")
    public Map<String,Object> importJob(@PathVariable UUID id){
        return generation.importJob(id);
    }

}
