package com.lmspilot.operations.api;import java.util.Map;public record ServiceHealth(String name,String status,String version,Map<String,Object> details){}
