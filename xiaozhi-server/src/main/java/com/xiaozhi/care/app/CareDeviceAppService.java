package com.xiaozhi.care.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.CareDeviceDO;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import com.xiaozhi.care.mqtt.CareCommandGateway;
import com.xiaozhi.care.model.CareDeviceResp;
import com.xiaozhi.care.model.CommandLogResp;
import com.xiaozhi.care.service.CareDeviceService;
import com.xiaozhi.care.support.CareAccessService;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.CareDevicePageReq;
import com.xiaozhi.common.model.care.CommandLogPageReq;
import com.xiaozhi.common.model.care.CommandSendReq;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关怀设备状态 + 子女命令下发编排
 */
@Service
public class CareDeviceAppService {

    @Resource
    private CareDeviceService careDeviceService;

    @Resource
    private CareCommandGateway commandGateway;

    @Resource
    private CareAccessService access;

    @Resource
    private com.xiaozhi.care.service.CommandService commandService;

    public PageResult<CareDeviceResp> page(CareDevicePageReq req, Integer userId) {
        CareDevicePageReq r = req == null ? new CareDevicePageReq() : req;
        IPage<CareDeviceDO> page = careDeviceService.page(r.getPageNo(), r.getPageSize(),
                r.getElderId(), r.getOnline(), access.visibleElderIds(userId));
        Map<String, String> names = access.elderNameMap(
                page.getRecords().stream().map(CareDeviceDO::getElderId).toList());
        List<CareDeviceResp> list = page.getRecords().stream().map(d -> {
            CareDeviceResp resp = CareDeviceResp.from(d);
            resp.setElderName(names.get(d.getElderId()));
            return resp;
        }).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 子女端命令下发（仅 care_message / query_status 两类；提醒计划走 Medication 接口）
     */
    public CommandLogResp sendCommand(CommandSendReq req, Integer userId) {
        access.checkElderAccess(req.getElderId(), userId);
        Map<String, Object> data = new LinkedHashMap<>();
        switch (req.getCommandType()) {
            case "care_message" -> {
                if (!StringUtils.hasText(req.getMessage())) {
                    throw new IllegalArgumentException("关怀消息不能为空");
                }
                data.put("text", req.getMessage());
                if (StringUtils.hasText(req.getExtra())) {
                    data.put("extra", req.getExtra());
                }
            }
            case "query_status" -> data.put("fields", List.of("battery", "rssi", "firmware", "time"));
            default -> throw new IllegalArgumentException(
                    "不支持的命令类型: " + req.getCommandType() + "（提醒类命令请走吃药提醒接口）");
        }
        var log = commandGateway.send(req.getElderId(), req.getCommandType(), userId,
                access.currentFamilyId(userId), data);
        CommandLogResp resp = CommandLogResp.from(log);
        resp.setElderName(access.elderNameMap(List.of(req.getElderId())).get(req.getElderId()));
        return resp;
    }

    public PageResult<CommandLogResp> commandLogs(CommandLogPageReq req, Integer userId) {
        CommandLogPageReq r = req == null ? new CommandLogPageReq() : req;
        IPage<CommandLogDO> page = commandLogPage(r, userId);
        Map<String, String> names = access.elderNameMap(
                page.getRecords().stream().map(CommandLogDO::getElderId).toList());
        List<CommandLogResp> list = page.getRecords().stream().map(l -> {
            CommandLogResp resp = CommandLogResp.from(l);
            resp.setElderName(names.get(l.getElderId()));
            return resp;
        }).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    private IPage<CommandLogDO> commandLogPage(CommandLogPageReq r, Integer userId) {
        return commandService.page(r.getPageNo(), r.getPageSize(), r.getElderId(),
                r.getCommandType(), r.getStatus(), access.visibleElderIds(userId));
    }
}
