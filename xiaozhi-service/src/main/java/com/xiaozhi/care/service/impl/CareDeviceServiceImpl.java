package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.care.dal.mysql.dataobject.CareDeviceDO;
import com.xiaozhi.care.dal.mysql.mapper.CareDeviceMapper;
import com.xiaozhi.care.service.CareDeviceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Slf4j
@Service
public class CareDeviceServiceImpl implements CareDeviceService {

    @Resource
    private CareDeviceMapper careDeviceMapper;

    @Override
    public CareDeviceDO getByDeviceId(String deviceId) {
        return careDeviceMapper.selectById(deviceId);
    }

    @Override
    public void markOnline(String deviceId, String elderId) {
        LocalDateTime now = LocalDateTime.now();
        CareDeviceDO existing = careDeviceMapper.selectById(deviceId);
        if (existing == null) {
            CareDeviceDO device = new CareDeviceDO();
            device.setDeviceId(deviceId);
            device.setElderId(elderId);
            device.setOnline(CareDeviceDO.ONLINE);
            device.setLastOnlineTime(now);
            careDeviceMapper.insert(device);
            return;
        }
        CareDeviceDO patch = new CareDeviceDO();
        patch.setDeviceId(deviceId);
        patch.setOnline(CareDeviceDO.ONLINE);
        patch.setLastOnlineTime(now);
        if (StringUtils.hasText(elderId) && !elderId.equals(existing.getElderId())) {
            patch.setElderId(elderId);
        }
        careDeviceMapper.updateById(patch);
    }

    @Override
    public void markOffline(String deviceId) {
        CareDeviceDO existing = careDeviceMapper.selectById(deviceId);
        if (existing == null) {
            // LWT 可能先于任何 telemetry 到达：建一条离线档案
            CareDeviceDO device = new CareDeviceDO();
            device.setDeviceId(deviceId);
            device.setOnline(CareDeviceDO.OFFLINE);
            careDeviceMapper.insert(device);
            return;
        }
        if (CareDeviceDO.OFFLINE.equals(existing.getOnline())) {
            return;
        }
        CareDeviceDO patch = new CareDeviceDO();
        patch.setDeviceId(deviceId);
        patch.setOnline(CareDeviceDO.OFFLINE);
        careDeviceMapper.updateById(patch);
    }

    @Override
    public void updateTelemetry(String deviceId, Integer battery, Integer rssi, String ip, String firmware) {
        if (careDeviceMapper.selectById(deviceId) == null) {
            markOnline(deviceId, null);
        }
        CareDeviceDO patch = new CareDeviceDO();
        patch.setDeviceId(deviceId);
        patch.setBattery(battery);
        patch.setRssi(rssi);
        patch.setIp(ip);
        patch.setFirmware(firmware);
        patch.setLastHeartbeat(LocalDateTime.now());
        try {
            careDeviceMapper.updateById(patch);
        } catch (Exception e) {
            // battery/rssi 为 null 时 MP 默认不更新对应列，但全空则无字段可更；忽略无害异常
            log.debug("设备遥测更新跳过: deviceId={}", deviceId);
        }
    }

    @Override
    public void assignElder(String deviceId, String elderId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        if (careDeviceMapper.selectById(deviceId) == null) {
            CareDeviceDO device = new CareDeviceDO();
            device.setDeviceId(deviceId);
            device.setElderId(elderId);
            careDeviceMapper.insert(device);
            return;
        }
        CareDeviceDO patch = new CareDeviceDO();
        patch.setDeviceId(deviceId);
        patch.setElderId(elderId);
        careDeviceMapper.updateById(patch);
    }

    @Override
    public IPage<CareDeviceDO> page(long pageNo, long pageSize, String elderId, String online,
                                    Collection<String> scopeElderIds) {
        QueryWrapper<CareDeviceDO> qw = new QueryWrapper<>();
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (StringUtils.hasText(online)) {
            qw.eq("online", online);
        }
        if (scopeElderIds != null) {
            if (scopeElderIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in("elderId", scopeElderIds);
        }
        qw.orderByDesc("lastHeartbeat");
        return careDeviceMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }
}
