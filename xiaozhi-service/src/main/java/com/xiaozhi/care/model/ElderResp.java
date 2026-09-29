package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "老人信息")
public class ElderResp implements Serializable {

    @Schema(description = "老人ID")
    private String elderId;

    private String name;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String address;
    private String avatar;
    private String deviceId;
    private String healthNote;
    private String state;
    private LocalDateTime createTime;

    @Schema(description = "设备在线状态 1/0（未绑设备为 null）")
    private String deviceOnline;

    @Schema(description = "设备电量（未上报为 null）")
    private Integer deviceBattery;

    @Schema(description = "绑定子女列表")
    private List<BindingResp> bindings;

    public static ElderResp from(ElderDO d) {
        ElderResp r = new ElderResp();
        r.setElderId(d.getElderId());
        r.setName(d.getName());
        r.setGender(d.getGender());
        r.setBirthday(d.getBirthday());
        r.setPhone(d.getPhone());
        r.setAddress(d.getAddress());
        r.setAvatar(d.getAvatar());
        r.setDeviceId(d.getDeviceId());
        r.setHealthNote(d.getHealthNote());
        r.setState(d.getState());
        r.setCreateTime(d.getCreateTime());
        return r;
    }
}
