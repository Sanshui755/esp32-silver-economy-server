package com.xiaozhi.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;

import java.util.Collection;

/**
 * 命令下发与确认记录
 */
public interface CommandService {

    /** 幂等落库：msgId 冲突返回 false */
    boolean saveIdempotent(CommandLogDO log);

    /** 设备 command_ack 回执：更新状态与确认内容 */
    void markAcked(String msgId, String status, String ackPayload);

    CommandLogDO getByMsgId(String msgId);

    IPage<CommandLogDO> page(long pageNo, long pageSize, String elderId, String commandType,
                             String status, Collection<String> scopeElderIds);
}
