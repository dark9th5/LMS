package com.lmspilot.assessment.platform;
import com.fasterxml.jackson.databind.JsonNode; import com.lmspilot.assessment.domain.QuestionType; import java.util.*;
public final class ObjectiveScoring {
 private ObjectiveScoring(){}
 public record ObjectiveScore(double earned,double possible,boolean exact){}
 public static ObjectiveScore score(QuestionType type,List<String> expected,JsonNode answer,double points){
  if(type==QuestionType.ESSAY||type==QuestionType.SHORT_TEXT) return new ObjectiveScore(0,points,false);
  Set<String> exp=new LinkedHashSet<>(); expected.forEach(v->exp.add(normal(v))); Set<String> got=new LinkedHashSet<>();
  if(answer!=null){ if(answer.isArray()) answer.forEach(n->got.add(normal(n.asText()))); else got.add(normal(answer.asText())); }
  if(exp.equals(got)) return new ObjectiveScore(points,points,true);
  if(type==QuestionType.MULTIPLE_CHOICE && !got.isEmpty() && exp.containsAll(got)){ double ratio=(double)got.size()/Math.max(1,exp.size()); return new ObjectiveScore(points*ratio,points,false); }
  return new ObjectiveScore(0,points,false);
 }
 private static String normal(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
}
