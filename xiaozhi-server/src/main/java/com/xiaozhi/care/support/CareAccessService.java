package com.xiaozhi.care.support;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.service.ElderService;
import com.xiaozhi.care.service.FamilyBindingService;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关怀模块数据可见性/越权防护：
 * - 管理员（sys_user.isAdmin=1）可见全部老人
 * - 普通用户（家属）只能看到/操作 elder_family_binding 里绑定且 state=1 的老人
 */
@Component
public class CareAccessService {

    @Resource
    private UserService userService;

    @Resource
    private FamilyBindingService familyBindingService;

    @Resource
    private ElderService elderService;

    public Integer loginId() {
        return StpUtil.getLoginIdAsInt();
    }

    public boolean isAdmin(Integer userId) {
        UserBO user = userService.getBO(userId);
        return user != null && UserBO.ADMIN_YES.equals(user.getIsAdmin());
    }

    /**
     * 当前登录人的可见老人范围。
     *
     * @return null = 不限制（管理员）；空集合 = 无可见老人；非空 = 可见 elderId 集合
     */
    public Collection<String> visibleElderIds(Integer userId) {
        if (isAdmin(userId)) {
            return null;
        }
        return familyBindingService.boundElderIds(userId);
    }

    /** 单老人操作前的越权校验（绑定校验 + 状态校验） */
    public ElderDO checkElderAccess(String elderId, Integer userId) {
        if (!StringUtils.hasText(elderId)) {
            throw new IllegalArgumentException("老人ID不能为空");
        }
        ElderDO elder = elderService.getByElderId(elderId);
        if (elder == null || ElderDO.STATE_CANCELLED.equals(elder.getState())) {
            throw new IllegalArgumentException("老人不存在或已注销");
        }
        if (!isAdmin(userId) && !familyBindingService.isBound(elderId, userId)) {
            throw new IllegalArgumentException("无权操作该老人：未建立绑定关系");
        }
        return elder;
    }

    /** 批量取 elderId -> 姓名（列表 enrich 用） */
    public Map<String, String> elderNameMap(Collection<String> elderIds) {
        if (elderIds == null || elderIds.isEmpty()) {
            return Map.of();
        }
        return elderService.listByElderIds(elderIds).stream()
                .collect(Collectors.toMap(ElderDO::getElderId, ElderDO::getName, (a, b) -> a));
    }

    /** 家属操作下发的命令记录 familyId（管理员为 null） */
    public String currentFamilyId(Integer userId) {
        var member = familyBindingService.getFamilyMemberByUserId(userId);
        return member == null ? null : member.getFamilyId();
    }
}
