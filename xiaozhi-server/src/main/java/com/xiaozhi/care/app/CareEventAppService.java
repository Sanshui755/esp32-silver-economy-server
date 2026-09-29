package com.xiaozhi.care.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import com.xiaozhi.care.model.CareEventResp;
import com.xiaozhi.care.service.CareEventService;
import com.xiaozhi.care.support.CareAccessService;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.CareEventPageReq;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 事件中心编排：可见性过滤 + 老人姓名 enrich
 */
@Service
public class CareEventAppService {

    @Resource
    private CareEventService careEventService;

    @Resource
    private CareAccessService access;

    public PageResult<CareEventResp> page(CareEventPageReq req, Integer userId) {
        CareEventPageReq r = req == null ? new CareEventPageReq() : req;
        // 家属不关心心跳刷屏：未显式筛选类型时隐藏 heartbeat
        String eventType = StringUtils.hasText(r.getEventType()) ? r.getEventType() : null;
        IPage<CareEventDO> page = careEventService.page(r.getPageNo(), r.getPageSize(),
                r.getElderId(), eventType, r.getSeverity(), r.getStatus(),
                access.visibleElderIds(userId));
        Map<String, String> names = access.elderNameMap(
                page.getRecords().stream().map(CareEventDO::getElderId).toList());
        List<CareEventResp> list = page.getRecords().stream().map(e -> {
            CareEventResp resp = CareEventResp.from(e);
            resp.setElderName(names.get(e.getElderId()));
            return resp;
        }).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    public CareEventResp detail(Long id, Integer userId) {
        CareEventDO event = careEventService.get(id);
        if (event == null) {
            throw new IllegalArgumentException("事件不存在");
        }
        access.checkElderAccess(event.getElderId(), userId);
        CareEventResp resp = CareEventResp.from(event);
        resp.setElderName(access.elderNameMap(List.of(event.getElderId())).get(event.getElderId()));
        return resp;
    }

    public void handle(Long id, Integer userId) {
        CareEventDO event = careEventService.get(id);
        if (event == null) {
            throw new IllegalArgumentException("事件不存在");
        }
        access.checkElderAccess(event.getElderId(), userId);
        careEventService.handle(id, userId);
    }
}
