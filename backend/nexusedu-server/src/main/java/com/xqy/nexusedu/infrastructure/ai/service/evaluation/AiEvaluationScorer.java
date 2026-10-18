package com.xqy.nexusedu.infrastructure.ai.service.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Pattern;
import com.xqy.nexusedu.common.dto.ai.AiEvaluationScore;

/** Deterministic golden-set evaluation, not an LLM judge. */
public final class AiEvaluationScorer {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Pattern CITATION=Pattern.compile("\\[([^\\[\\]\\s#]+)#[^\\[\\]\\s]+\\]");
    private AiEvaluationScorer() { }
    public static AiEvaluationScore score(Map<String,Object> sample,String actualPlan,String answer,List<Map<String,Object>> sources) {
        JsonNode expected=tree(sample.get("expectedPlan")),actual=tree(actualPlan==null?"[]":actualPlan);
        int plan=expected.isArray() && actual.isArray() && expected.size()==actual.size()?1:0;
        if(plan==1)for(int i=0;i<expected.size();i++)if(!contains(actual.get(i),expected.get(i))) {plan=0;break;}
        Set<String> tools=new TreeSet<>();
        if(actual.isArray())for(JsonNode task:actual)if(task.hasNonNull("toolName") && !task.path("toolName").asText().isBlank())tools.add(task.path("toolName").asText());
        Set<String> goldTools=strings(tree(sample.get("expectedTools"))),goldSources=strings(tree(sample.get("expectedSources")));
        Set<String> retrieved=new HashSet<>();for(var source:sources)retrieved.add(source.get("documentNo")+"#"+source.get("chunkNo"));
        Integer rag=goldSources.isEmpty()?null:Collections.disjoint(goldSources,retrieved)?0:1;
        Set<String> citations=new HashSet<>();var matcher=CITATION.matcher(answer==null?"":answer);
        while(matcher.find())citations.add(matcher.group().substring(1,matcher.group().length()-1));
        int correct=0;for(String citation:citations)if(retrieved.contains(citation) && goldSources.contains(citation))correct++;
        Boolean refusal=answer==null?null:answer.stripLeading().startsWith("【拒答】");
        boolean expectedRefusal=Boolean.TRUE.equals(sample.get("expectedRefusal"));
        return new AiEvaluationScore(plan,tools.equals(goldTools)?1:0,rag,citations.size(),correct,refusal,refusal==null?null:refusal==expectedRefusal);
    }
    private static JsonNode tree(Object value) {
        try{return value instanceof String text?JSON.readTree(text):JSON.valueToTree(value);}catch(Exception error){throw new IllegalArgumentException("无效评测 JSON",error);}
    }
    private static Set<String> strings(JsonNode array) {Set<String> result=new TreeSet<>();if(array!=null && array.isArray())array.forEach(v->result.add(v.asText()));return result;}
    /** Expected plan fixes domains/tools and any supplied query fields; explanatory text is ignored. */
    private static boolean contains(JsonNode actual,JsonNode expected) {
        if(actual==null || expected==null)return actual==expected;
        if(expected.isObject()) {var fields=expected.fields();while(fields.hasNext()) {var field=fields.next();if(!contains(actual.get(field.getKey()),field.getValue()))return false;}return true;}
        if(expected.isArray()) {if(!actual.isArray() || actual.size()!=expected.size())return false;for(int i=0;i<expected.size();i++)if(!contains(actual.get(i),expected.get(i)))return false;return true;}
        if(expected.isNumber() && actual.isNumber())return expected.decimalValue().compareTo(actual.decimalValue())==0;
        return expected.equals(actual);
    }
}
