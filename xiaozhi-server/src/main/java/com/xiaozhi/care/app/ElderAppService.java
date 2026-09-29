package com.xiaozhi.care.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.dal.mysql.dataobject.FamilyMemberDO;
import com.xiaozhi.care.model.BindingResp;
import com.xiaozhi.care.model.ElderResp;
import com.xiaozhi.care.service.CareDeviceService;
import com.xiaozhi.care.service.ElderService;
import com.xiaozhi.care.service.FamilyBindingService;
import com.xiaozhi.care.support.CareAccessService;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.BindingCreateReq;
import com.xiaozhi.common.model.care.BindingUpdateReq;
import com.xiaozhi.common.model.care.ElderPageReq;
import com.xiaozhi.common.model.care.ElderSaveReq;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 老人管理 + 子女绑定编排（可见性过滤 / 设备归属同步 / 账号校验）
 */
@Service
public class ElderAppService {

    @Resource
    private ElderService elderService;

    @Resource
    private FamilyBindingService familyBindingService;

    @Resource
    private CareDeviceService careDeviceService;

    @Resource
    private CareAccessService access;

    @Resource
    private UserService userService;

    public PageResult<ElderResp> page(ElderPageReq req, Integer userId) {
        ElderPageReq r = req == null ? new ElderPageReq() : req;
        Collection<String> scope = access.visibleElderIds(userId);
        IPage<ElderDO> page = elderService.page(r.getPageNo(), r.getPageSize(),
                r.getName(), r.getPhone(), r.getElderId(), scope);
        List<ElderResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    public ElderResp detail(String elderId, Integer userId) {
        access.checkElderAccess(elderId, userId);
        return toResp(elderService.getByElderId(elderId));
    }

    public ElderResp create(ElderSaveReq req, Integer userId) {
        ElderDO elder = new ElderDO();
        copySaveReq(req, elder);
        elder.setUserId(userId);
        elderService.create(elder);
        if (StringUtils.hasText(req.getDeviceId())) {
            careDeviceService.assignElder(req.getDeviceId(), elder.getElderId());
        }
        return toResp(elderService.getByElderId(elder.getElderId()));
    }

    public ElderResp update(ElderSaveReq req, Integer userId) {
        if (!StringUtils.hasText(req.getElderId())) {
            throw new IllegalArgumentException("编辑时 elderId 不能为空");
        }
        access.checkElderAccess(req.getElderId(), userId);
        ElderDO elder = new ElderDO();
        elder.setElderId(req.getElderId());
        copySaveReq(req, elder);
        elderService.update(elder);
        if (StringUtils.hasText(req.getDeviceId())) {
            careDeviceService.assignElder(req.getDeviceId(), req.getElderId());
        }
        return toResp(elderService.getByElderId(req.getElderId()));
    }

    public void delete(String elderId, Integer userId) {
        access.checkElderAccess(elderId, userId);
        elderService.delete(elderId);
    }

    // -------------------- 绑定 --------------------

    public List<BindingResp> bindings(String elderId, Integer userId) {
        access.checkElderAccess(elderId, userId);
        return familyBindingService.listByElder(elderId).stream()
                .map(b -> {
                    FamilyMemberDO member = familyMemberOf(b.getFamilyId());
                    return BindingResp.from(b, member, usernameOf(member));
                }).toList();
    }

    public BindingResp bind(BindingCreateReq req, Integer userId) {
        access.checkElderAccess(req.getElderId(), userId);
        if (req.getUserId() == null) {
            throw new IllegalArgumentException("家属账号 userId 不能为空");
        }
        UserBO target = userService.getBO(req.getUserId());
        if (target == null) {
            throw new IllegalArgumentException("家属账号不存在: " + req.getUserId());
        }
        var binding = familyBindingService.bind(req.getElderId(), req.getUserId(),
                req.getRelation(), req.getIsPrimary());
        FamilyMemberDO member = familyBindingService.getFamilyMemberByUserId(req.getUserId());
        return BindingResp.from(binding, member, target.getUsername());
    }

    public void updateBinding(BindingUpdateReq req, Integer userId) {
        var binding = familyBindingService.getBinding(req.getId());
        if (binding == null) {
            throw new IllegalArgumentException("绑定记录不存在");
        }
        access.checkElderAccess(binding.getElderId(), userId);
        familyBindingService.updateBinding(req.getId(), req.getState(), req.getIsPrimary());
    }

    // -------------------- 内部 --------------------

    private ElderResp toResp(ElderDO elder) {
        ElderResp resp = ElderResp.from(elder);
        if (StringUtils.hasText(elder.getDeviceId())) {
            var device = careDeviceService.getByDeviceId(elder.getDeviceId());
            if (device != null) {
                resp.setDeviceOnline(device.getOnline());
                resp.setDeviceBattery(device.getBattery());
            }
        }
        resp.setBindings(familyBindingService.listByElder(elder.getElderId()).stream()
                .filter(b -> b.getState() != null && "1".equals(b.getState()))
                .map(b -> {
                    FamilyMemberDO member = familyMemberOf(b.getFamilyId());
                    return BindingResp.from(b, member, usernameOf(member));
                }).collect(Collectors.toList()));
        return resp;
    }

    private FamilyMemberDO familyMemberOf(String familyId) {
        return familyBindingService.getFamilyMemberByFamilyId(familyId);
    }

    private String usernameOf(FamilyMemberDO member) {
        if (member == null || member.getUserId() == null) {
            return null;
        }
        UserBO user = userService.getBO(member.getUserId());
        return user == null ? null : user.getUsername();
    }

    private void copySaveReq(ElderSaveReq req, ElderDO elder) {
        elder.setName(req.getName());
        elder.setGender(req.getGender());
        elder.setBirthday(req.getBirthday());
        elder.setPhone(req.getPhone());
        elder.setAddress(req.getAddress());
        elder.setAvatar(req.getAvatar());
        elder.setDeviceId(req.getDeviceId());
        elder.setHealthNote(req.getHealthNote());
    }
}
