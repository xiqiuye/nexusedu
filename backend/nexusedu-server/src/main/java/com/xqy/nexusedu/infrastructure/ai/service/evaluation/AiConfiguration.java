package com.xqy.nexusedu.infrastructure.ai.service.evaluation;

import cn.hutool.json.JSONUtil;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import com.xqy.nexusedu.common.dto.ManagementRows;

/** Validates only supported configuration fields; model credentials are environment references. */
public final class AiConfiguration {
    private AiConfiguration() { }
    public static String type(Object value) {
        String type=ManagementRows.text(value,16,true);
        if(!Set.of("MODEL","TOOL","PROMPT").contains(type))throw new IllegalArgumentException("类型应为 MODEL、TOOL 或 PROMPT");
        return type;
    }
    public static String key(Object value) {
        String key=ManagementRows.text(value,128,true);
        if(!key.matches("[a-zA-Z0-9_*:.-]+"))throw new IllegalArgumentException("无效资源标识");
        return key;
    }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> object(Object value) {
        if(value instanceof Map<?,?> map)return (Map<String,Object>)map;
        if(value instanceof String text && JSONUtil.isTypeJSONObject(text))return (Map<String,Object>)json(text);
        throw new IllegalArgumentException("配置必须是 JSON 对象");
    }
    public static Object json(String value) {
        try {return new com.fasterxml.jackson.databind.ObjectMapper().readValue(value,Object.class);}
        catch(Exception error){throw new IllegalArgumentException("无效 JSON",error);}
    }
    public static Map<String,Object> validate(String type,String key,Object value) {
        Map<String,Object> config=new java.util.LinkedHashMap<>(object(value));
        Set<String> fields=switch(type) {
            case "MODEL" -> Set.of("provider","modelName","baseUrl","apiKeyEnv","maxOutputTokens","enabled");
            case "TOOL" -> Set.of("domain","description","argumentSpecification","keywords","enabled");
            default -> Set.of("content","enabled");
        };
        if(!fields.containsAll(config.keySet()))throw new IllegalArgumentException("配置包含不支持的字段");
        if(config.containsKey("enabled") && !(config.get("enabled") instanceof Boolean))throw new IllegalArgumentException("enabled 必须为布尔值");
        config.putIfAbsent("enabled",true);
        switch(type) {
            case "MODEL" -> {
                String provider=ManagementRows.text(config.get("provider"),64,true);
                if(!Set.of("openai","deepseek").contains(provider))throw new IllegalArgumentException("未部署对应模型适配器");
                ManagementRows.text(config.get("modelName"),128,true);
                String env=ManagementRows.text(config.get("apiKeyEnv"),128,true);
                if(!env.matches("[A-Z][A-Z0-9_]{2,127}"))throw new IllegalArgumentException("密钥仅支持环境变量名");
                URI uri=URI.create(ManagementRows.text(config.get("baseUrl"),500,true));
                if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)throw new IllegalArgumentException("模型地址必须是无凭据的 HTTPS 地址");
                config.put("maxOutputTokens",integer(config.getOrDefault("maxOutputTokens",4096),1,32768));
            }
            case "TOOL" -> {
                String domain=ManagementRows.text(config.get("domain"),64,true);
                if(!Set.of("student","knowledge").contains(domain) || !("query_student".equals(key) && "student".equals(domain) || "query_knowledge".equals(key) && "knowledge".equals(domain)))throw new IllegalArgumentException("只能配置已部署的查询工具");
                for(String field:ListFields.TOOL)ManagementRows.text(config.get(field),8000,false);
            }
            default -> ManagementRows.text(config.get("content"),24000,true);
        }
        return config;
    }
    private static final class ListFields { static final String[] TOOL={"description","argumentSpecification","keywords"}; }
    public static int integer(Object value,int min,int max) {
        try { int number=new java.math.BigDecimal(String.valueOf(value)).intValueExact(); if(number<min || number>max)throw new ArithmeticException();return number; }
        catch(Exception error) {throw new IllegalArgumentException("整数范围应为 "+min+"–"+max);}
    }
}
