package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import com.xiaozhi.care.dal.mysql.mapper.CommandLogMapper;
import com.xiaozhi.care.service.CommandService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Slf4j
@Service
public class CommandServiceImpl implements CommandService {

    @Resource
    private CommandLogMapper commandLogMapper;

    @Override
    public boolean saveIdempotent(CommandLogDO logEntry) {
        try {
            commandLogMapper.insert(logEntry);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("命令重复下发，已丢弃: msgId={}, type={}", logEntry.getMsgId(), logEntry.getCommandType());
            return false;
        }
    }

    @Override
    public void markAcked(String msgId, String status, String ackPayload) {
        CommandLogDO existing = getByMsgId(msgId);
        if (existing == null) {
            log.warn("收到未知命令的 ack: msgId={}", msgId);
            return;
        }
        CommandLogDO patch = new CommandLogDO();
        patch.setId(existing.getId());
        patch.setStatus(StringUtils.hasText(status) ? status : CommandLogDO.STATUS_ACKED);
        patch.setAckTime(LocalDateTime.now());
        patch.setAckPayload(ackPayload);
        commandLogMapper.updateById(patch);
    }

    @Override
    public CommandLogDO getByMsgId(String msgId) {
        return commandLogMapper.selectOne(new QueryWrapper<CommandLogDO>()
                .eq("msgId", msgId).last("LIMIT 1"));
    }

    @Override
    public IPage<CommandLogDO> page(long pageNo, long pageSize, String elderId, String commandType,
                                    String status, Collection<String> scopeElderIds) {
        QueryWrapper<CommandLogDO> qw = new QueryWrapper<>();
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (StringUtils.hasText(commandType)) {
            qw.eq("commandType", commandType);
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
        return commandLogMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }
}
