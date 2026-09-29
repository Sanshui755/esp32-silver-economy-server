package com.xiaozhi.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.CareDeviceDO;

import java.util.Collection;

/**
 * 关怀设备状态
 */
public interface CareDeviceService {

    CareDeviceDO getByDeviceId(String deviceId);

    /** 上线（存在则置在线，不存在则创建） */
    void markOnline(String deviceId, String elderId);

    /** 下线（LWT / 断连） */
    void markOffline(String deviceId);

    /** 心跳/状态遥测：只更新非空字段（电量/信号/IP/固件/最后心跳） */
    void updateTelemetry(String deviceId, Integer battery, Integer rssi, String ip, String firmware);

    /** 老人绑定/换绑设备时同步归属 */
    void assignElder(String deviceId, String elderId);

    IPage<CareDeviceDO> page(long pageNo, long pageSize, String elderId, String online,
                             Collection<String> scopeElderIds);
}
