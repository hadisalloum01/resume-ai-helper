package com.resumeai.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LocalAnalysisServiceTest {
    @Test void returnsUsefulResult(){
        var r=new LocalAnalysisService().analyze("Java Spring SQL developer", "Need Java Spring PostgreSQL Docker developer");
        assertThat(r.matchScore()).isBetween(15,95);
        assertThat(r.strengths()).isNotEmpty();
        assertThat(r.recommendations()).isNotEmpty();
    }
}
