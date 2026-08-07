package com.lmspilot.ai.platform;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import java.util.regex.Pattern;

import static com.lmspilot.ai.platform.QuestionGenerationContracts.*;
public final class GeneratedQuestionQualityValidator{
    private GeneratedQuestionQualityValidator(){
    }
    public static QuestionSetValidationResult validate(JsonNode root,GenerateQuestionsCommand cmd,List<SourceChunk>chunks){
        List<ValidationProblem>p=new ArrayList<>(QuestionSetBusinessValidator.validate(root).problems());
        JsonNode qs=root.path("questions");
        if(!qs.isArray())return new QuestionSetValidationResult(false,p);
        if(qs.size()!=cmd.numberOfQuestions())p.add(new ValidationProblem("/questions","AI phải tạo đúng "+cmd.numberOfQuestions()+" câu, hiện nhận "+qs.size()+" câu"));
        Map<String,Integer>expected=DifficultyDistributionPolicy.expectedCounts(cmd.numberOfQuestions(),cmd.difficultyDistribution());
        for(String d:DifficultyDistributionPolicy.DIFFICULTIES){
            int actual=0;
            for(JsonNode q:qs)if(d.equals(q.path("difficulty").asText()))actual++;
            if(actual!=expected.get(d))p.add(new ValidationProblem("/questions/difficulty","Phân bố "+d+" phải có "+expected.get(d)+" câu, hiện có "+actual+" câu"));
        }
        Set<String>allowed=new HashSet<>();
        cmd.questionTypes().forEach(t->allowed.add(t.toUpperCase(Locale.ROOT)));
        Set<String>stems=new HashSet<>();
        Map<UUID,List<SourceChunk>>source=new HashMap<>();
        for(SourceChunk c:chunks)source.computeIfAbsent(c.documentVersionId(),x->new ArrayList<>()).add(c);
        for(int i=0;
        i<qs.size();
        i++){
            JsonNode q=qs.get(i);
            String b="/questions/"+i;
            if(!allowed.contains(q.path("type").asText()))p.add(new ValidationProblem(b+"/type","Loại câu hỏi không nằm trong lựa chọn của giảng viên"));
            String stem=norm(q.path("stem").asText());
            if(!stem.isEmpty()&&!stems.add(stem))p.add(new ValidationProblem(b+"/stem","Câu hỏi bị trùng nội dung"));
            Set<String>opts=new HashSet<>();
            int count=0;
            for(JsonNode o:q.path("options")){
                String x=norm(o.path("text").asText());
                if(!x.isEmpty()){
                    count++;
                    opts.add(x);
                }

            }
            if(opts.size()!=count)p.add(new ValidationProblem(b+"/options","Các phương án trả lời bị trùng"));
            if(q.path("explanation").asText().trim().length()<12)p.add(new ValidationProblem(b+"/explanation","Giải thích đáp án quá ngắn"));
            JsonNode cs=q.path("citations");
            if(cs.isArray())for(int j=0;
            j<cs.size();
            j++){
                JsonNode c=cs.get(j);
                UUID id=null;
                try{
                    id=UUID.fromString(c.path("documentVersionId").asText());
                }
                catch(Exception ignored){
                }
                String quote=norm(c.path("quote").asText());
                Integer page=c.path("page").isInt()?c.path("page").asInt():null;
                boolean matched=false;
                for(SourceChunk chunk:source.getOrDefault(id,List.of()))if((page==null||chunk.page()==null||page.equals(chunk.page()))&&norm(chunk.text()).contains(quote)){
                    matched=true;
                    break;
                }
                if(!quote.isEmpty()&&!matched)p.add(new ValidationProblem(b+"/citations/"+j+"/quote","Trích dẫn không khớp nguyên văn tài liệu nguồn"));
            }

        }
        return new QuestionSetValidationResult(p.isEmpty(),p.stream().distinct().toList());
    }
    private static String norm(String v){
        return v.toLowerCase(Locale.ROOT).replaceAll("[\\s\\u00A0]+"," ").replace('“','\"').replace('”','\"').trim();
    }

}
