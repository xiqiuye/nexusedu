package com.xqy.nexusedu.ai.evaluation.vo;

import java.io.Serializable;

/** Scoring result produced by the deterministic AI evaluation pipeline. */
public class AiEvaluationScore implements Serializable {
    private static final long serialVersionUID = 1L;
    private int planCorrect;
    private int toolCorrect;
    private Integer ragHit;
    private int citedCount;
    private int correctCitedCount;
    private Boolean refused;
    private Boolean refusalCorrect;

    public AiEvaluationScore() {
    }

    public AiEvaluationScore(int planCorrect, int toolCorrect, Integer ragHit, int citedCount,
                             int correctCitedCount, Boolean refused, Boolean refusalCorrect) {
        this.planCorrect = planCorrect;
        this.toolCorrect = toolCorrect;
        this.ragHit = ragHit;
        this.citedCount = citedCount;
        this.correctCitedCount = correctCitedCount;
        this.refused = refused;
        this.refusalCorrect = refusalCorrect;
    }

    public int getPlanCorrect() { return planCorrect; }
    public void setPlanCorrect(int planCorrect) { this.planCorrect = planCorrect; }
    public int getToolCorrect() { return toolCorrect; }
    public void setToolCorrect(int toolCorrect) { this.toolCorrect = toolCorrect; }
    public Integer getRagHit() { return ragHit; }
    public void setRagHit(Integer ragHit) { this.ragHit = ragHit; }
    public int getCitedCount() { return citedCount; }
    public void setCitedCount(int citedCount) { this.citedCount = citedCount; }
    public int getCorrectCitedCount() { return correctCitedCount; }
    public void setCorrectCitedCount(int correctCitedCount) { this.correctCitedCount = correctCitedCount; }
    public Boolean getRefused() { return refused; }
    public void setRefused(Boolean refused) { this.refused = refused; }
    public Boolean getRefusalCorrect() { return refusalCorrect; }
    public void setRefusalCorrect(Boolean refusalCorrect) { this.refusalCorrect = refusalCorrect; }

    public int planCorrect() { return planCorrect; }
    public int toolCorrect() { return toolCorrect; }
    public Integer ragHit() { return ragHit; }
    public int citedCount() { return citedCount; }
    public int correctCitedCount() { return correctCitedCount; }
    public Boolean refused() { return refused; }
    public Boolean refusalCorrect() { return refusalCorrect; }
}
