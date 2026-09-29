package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.CareDeviceDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "关怀设备状态")
public class CareDeviceResp implements Serializable {

    private String deviceId;
    private String elderId;

    @Schema(description = "老人姓名")
    private String elderName;

    private String online;
    private Integer battery;
    private Integer rssi;
    private String ip;
    private String firmware;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime updateTime;

    public static CareDeviceResp from(CareDeviceDO d) {
        CareDeviceResp r = new CareDeviceResp();
        r.setDeviceId(d.getDeviceId());
        r.setElderId(d.getElderId());
        r.setOnline(d.getOnline());
        r.setBattery(d.getBattery());
        r.setRssi(d.getRssi());
        r.setIp(d.getIp());
        r.setFirmware(d.getFirmware());
        r.setLastHeartbeat(d.getLastHeartbeat());
        r.setLastOnlineTime(d.getLastOnlineTime());
        r.setUpdateTime(d.getUpdateTime());
        return r;
    }
}
