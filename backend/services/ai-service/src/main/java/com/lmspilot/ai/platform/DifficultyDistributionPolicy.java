package com.lmspilot.ai.platform;

import java.util.*;
public final class DifficultyDistributionPolicy{
    public static final List<String> DIFFICULTIES=List.of("EASY","MEDIUM","HARD");
    private static final Map<String,Integer> DEFAULT=Map.of("EASY",30,"MEDIUM",50,"HARD",20);
    private DifficultyDistributionPolicy(){
    }
    public static Map<String,Integer> normalize(Map<String,Integer> input){
        if(input==null||input.isEmpty())return new LinkedHashMap<>(DEFAULT);
        Map<String,Integer> n=new HashMap<>();
        input.forEach((k,v)->n.put(k.trim().toUpperCase(Locale.ROOT),v));
        if(!DIFFICULTIES.containsAll(n.keySet()))throw new IllegalArgumentException("Độ khó chỉ nhận EASY, MEDIUM hoặc HARD");
        if(n.values().stream().anyMatch(v->v==null||v<0||v>100))throw new IllegalArgumentException("Tỷ lệ độ khó phải nằm trong khoảng 0 đến 100");
        if(DIFFICULTIES.stream().mapToInt(k->n.getOrDefault(k,0)).sum()!=100)throw new IllegalArgumentException("Tổng tỷ lệ độ khó phải bằng 100%");
        if(n.values().stream().noneMatch(v->v>0))throw new IllegalArgumentException("Cần chọn ít nhất một mức độ khó");
        Map<String,Integer> r=new LinkedHashMap<>();
        DIFFICULTIES.forEach(k->r.put(k,n.getOrDefault(k,0)));
        return r;
    }
    public static Map<String,Integer> expectedCounts(int total,Map<String,Integer> distribution){
        Map<String,Integer> n=normalize(distribution);
        Map<String,Double> exact=new LinkedHashMap<>();
        Map<String,Integer> r=new LinkedHashMap<>();
        for(String k:DIFFICULTIES){
            double x=Math.max(0,total)*n.get(k)/100.0;
            exact.put(k,x);
            r.put(k,(int)Math.floor(x));
        }
        int remaining=Math.max(0,total)-r.values().stream().mapToInt(Integer::intValue).sum();
        List<String> order=new ArrayList<>(DIFFICULTIES);
        order.sort(Comparator.<String>comparingDouble(k->-(exact.get(k)-Math.floor(exact.get(k)))).thenComparingInt(DIFFICULTIES::indexOf));
        for(String k:order)if(remaining-->0)r.put(k,r.get(k)+1);
        return r;
    }

}
