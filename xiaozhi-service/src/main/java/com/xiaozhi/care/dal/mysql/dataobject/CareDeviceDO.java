package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 关怀设备状态表 care_device（电量/心跳/在线是 MQTT 语义，不污染 sys_device）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("care_device")
public class CareDeviceDO extends BaseDO {

    public static final String ONLINE = "1";
    public static final String OFFLINE = "0";

    /** 设备ID(MQTT clientId) */
    @TableId(value = "deviceId", type = IdType.INPUT)
    private String deviceId;

    private String elderId;
    /** MQTT 在线状态 1-在线 0-离线 */
    private String online;
    /** 电量 0-100 */
    private Integer battery;
    /** WiFi 信号强度 */
    private Integer rssi;
    private String ip;
    private String firmware;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastOnlineTime;
}
