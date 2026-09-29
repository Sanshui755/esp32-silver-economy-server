package com.xiaozhi.care.service;

import com.xiaozhi.care.dal.mysql.dataobject.ElderFamilyBindingDO;
import com.xiaozhi.care.dal.mysql.dataobject.FamilyMemberDO;

import java.util.List;

/**
 * 老人-子女绑定关系
 */
public interface FamilyBindingService {

    List<ElderFamilyBindingDO> listByElder(String elderId);

    /** 指定账号（家属）绑定的全部在绑老人ID */
    List<String> boundElderIds(Integer userId);

    /** 判断家属是否绑定了该老人（用于 SSE 推送过滤与越权校验） */
    boolean isBound(String elderId, Integer userId);

    FamilyMemberDO getFamilyMemberByUserId(Integer userId);

    FamilyMemberDO getFamilyMemberByFamilyId(String familyId);

    /** 按 userId 取家属档案，不存在则自动创建（familyId 后端生成） */
    FamilyMemberDO getOrCreateFamilyMember(Integer userId);

    /** 绑定（唯一键幂等：已解绑的记录重新激活） */
    ElderFamilyBindingDO bind(String elderId, Integer userId, String relation, String isPrimary);

    /** 更新绑定状态（解绑/重新绑定/改主监护人） */
    void updateBinding(Integer id, String state, String isPrimary);

    ElderFamilyBindingDO getBinding(Integer id);
}
