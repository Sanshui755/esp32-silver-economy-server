package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.MedicationRecordDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "吃药执行记录")
public class MedicationRecordResp implements Serializable {

    private Long id;
    private String planId;
    private String elderId;

    @Schema(description = "老人姓名")
    private String elderName;

    @Schema(description = "药名（来自计划，可能已删除）")
    private String medicine;

    private LocalDateTime plannedTime;
    private String status;
    private LocalDateTime takenTime;
    private String confirmSource;
    private LocalDateTime createTime;

    public static MedicationRecordResp from(MedicationRecordDO d) {
        MedicationRecordResp r = new MedicationRecordResp();
        r.setId(d.getId());
        r.setPlanId(d.getPlanId());
        r.setElderId(d.getElderId());
        r.setPlannedTime(d.getPlannedTime());
        r.setStatus(d.getStatus());
        r.setTakenTime(d.getTakenTime());
        r.setConfirmSource(d.getConfirmSource());
        r.setCreateTime(d.getCreateTime());
        return r;
    }
}
