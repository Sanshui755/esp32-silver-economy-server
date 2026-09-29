package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Schema(description = "吃药提醒计划")
public class MedicationPlanResp implements Serializable {

    private Long id;
    private String planId;
    private String elderId;

    @Schema(description = "老人姓名")
    private String elderName;

    private String medicine;
    private String dosage;
    private LocalTime takeTime;
    private String repeatRule;
    private String weekdays;
    private String voicePrompt;
    private String state;
    private Integer createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static MedicationPlanResp from(MedicationPlanDO d) {
        MedicationPlanResp r = new MedicationPlanResp();
        r.setId(d.getId());
        r.setPlanId(d.getPlanId());
        r.setElderId(d.getElderId());
        r.setMedicine(d.getMedicine());
        r.setDosage(d.getDosage());
        r.setTakeTime(d.getTakeTime());
        r.setRepeatRule(d.getRepeatRule());
        r.setWeekdays(d.getWeekdays());
        r.setVoicePrompt(d.getVoicePrompt());
        r.setState(d.getState());
        r.setCreatedBy(d.getCreatedBy());
        r.setCreateTime(d.getCreateTime());
        r.setUpdateTime(d.getUpdateTime());
        return r;
    }
}
