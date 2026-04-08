package com.costanalysis.domain.model.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ChangeRequestReviewDto {

    private String comment;

    /** 승인 시 적용 부서 목록 */
    private List<String> approvedDepartments;
}
