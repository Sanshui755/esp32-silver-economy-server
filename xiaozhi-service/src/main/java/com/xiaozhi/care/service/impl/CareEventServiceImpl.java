package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import com.xiaozhi.care.dal.mysql.mapper.CareEventMapper;
import com.xiaozhi.care.service.CareEventService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Slf4j
@Service
public class CareEventServiceImpl implements CareEventService {

    @Resource
    private CareEventMapper careEventMapper;

    @Override
    public boolean saveIdempotent(CareEventDO event) {
        try {
            careEventMapper.insert(event);
            return true;
        } catch (DuplicateKeyException e) {
            // uk_msgId 冲突 = QoS1 重复投递/设备重发，幂等丢弃
            log.info("关怀事件重复投递，已丢弃: msgId={}, type={}", event.getMsgId(), event.getEventType());
            return false;
        }
    }

    @Override
    public IPage<CareEventDO> page(long pageNo, long pageSize, String elderId, String eventType,
                                   String severity, String status, Collection<String> scopeElderIds) {
        QueryWrapper<CareEventDO> qw = new QueryWrapper<>();
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (StringUtils.hasText(eventType)) {
            qw.eq("eventType", eventType);
        }
        if (StringUtils.hasText(severity)) {
            qw.eq("severity", severity);
        }
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (scopeElderIds != null) {
            if (scopeElderIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in("elderId", scopeElderIds);
        }
        qw.orderByDesc("createTime");
        return careEventMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public CareEventDO get(Long id) {
        return careEventMapper.selectById(id);
    }

    @Override
    public void handle(Long id, Integer operatorId) {
        CareEventDO patch = new CareEventDO();
        patch.setId(id);
        patch.setStatus(CareEventDO.STATUS_HANDLED);
        patch.setHandledBy(operatorId);
        patch.setHandledTime(LocalDateTime.now());
        careEventMapper.updateById(patch);
    }

    @Override
    public LocalDateTime lastHeartbeatEventTime(String elderId) {
        CareEventDO last = careEventMapper.selectOne(new QueryWrapper<CareEventDO>()
                .eq("elderId", elderId)
                .eq("eventType", CareEventDO.TYPE_HEARTBEAT)
                .orderByDesc("createTime")
                .last("LIMIT 1"));
        return last == null ? null : last.getCreateTime();
    }
}
