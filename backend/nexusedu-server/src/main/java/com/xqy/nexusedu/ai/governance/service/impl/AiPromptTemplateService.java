package com.xqy.nexusedu.ai.governance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.governance.entity.AiPromptTemplate;
import com.xqy.nexusedu.ai.governance.mapper.AiPromptTemplateMapper;

/** Loads enabled prompt templates from the database. */
@Service
public class AiPromptTemplateService {

    private final AiPromptTemplateMapper mapper;

    public AiPromptTemplateService(AiPromptTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public AiPromptTemplate findEnabled(String promptCode) {
        if (promptCode == null || promptCode.isBlank()) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getPromptCode, promptCode)
                .and(wrapper -> wrapper
                        .eq(AiPromptTemplate::getEnabled, "1")
                        .or()
                        .eq(AiPromptTemplate::getEnabled, "true")
                        .or()
                        .eq(AiPromptTemplate::getEnabled, "Y")
                        .or()
                        .eq(AiPromptTemplate::getEnabled, "yes"))
                .last("LIMIT 1"));
    }

    public String findEnabledContent(String promptCode) {
        AiPromptTemplate template = findEnabled(promptCode);
        return template == null ? null : template.getPromptContent();
    }
}
