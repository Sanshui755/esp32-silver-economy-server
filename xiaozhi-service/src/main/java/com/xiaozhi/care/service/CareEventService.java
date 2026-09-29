package com.xiaozhi.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 关怀事件
 */
public interface CareEventService {

    /** 幂等落库：msgId 冲突返回 false（唯一索引兜底） */
    boolean saveIdempotent(CareEventDO event);

    IPage<CareEventDO> page(long pageNo, long pageSize, String elderId, String eventType,
                            String severity, String status, Collection<String> scopeElderIds);

    CareEventDO get(Long id);

    /** 标记已处理 */
    void handle(Long id, Integer operatorId);

    /** 最近一次心跳事件时间（心跳事件入库节流用），无记录返回 null */
    LocalDateTime lastHeartbeatEventTime(String elderId);
}
