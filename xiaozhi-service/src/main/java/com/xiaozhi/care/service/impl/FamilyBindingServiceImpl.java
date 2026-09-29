package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaozhi.care.dal.mysql.dataobject.ElderFamilyBindingDO;
import com.xiaozhi.care.dal.mysql.dataobject.FamilyMemberDO;
import com.xiaozhi.care.dal.mysql.mapper.ElderFamilyBindingMapper;
import com.xiaozhi.care.dal.mysql.mapper.FamilyMemberMapper;
import com.xiaozhi.care.service.FamilyBindingService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.List;

@Service
public class FamilyBindingServiceImpl implements FamilyBindingService {

    @Resource
    private ElderFamilyBindingMapper bindingMapper;

    @Resource
    private FamilyMemberMapper familyMemberMapper;

    @Override
    public List<ElderFamilyBindingDO> listByElder(String elderId) {
        return bindingMapper.selectList(new QueryWrapper<ElderFamilyBindingDO>()
                .eq("elderId", elderId)
                .orderByDesc("isPrimary")
                .orderByAsc("id"));
    }

    @Override
    public List<String> boundElderIds(Integer userId) {
        FamilyMemberDO member = getFamilyMemberByUserId(userId);
        if (member == null) {
            return Collections.emptyList();
        }
        return bindingMapper.selectList(new QueryWrapper<ElderFamilyBindingDO>()
                        .eq("familyId", member.getFamilyId())
                        .eq("state", ElderFamilyBindingDO.STATE_BOUND))
                .stream().map(ElderFamilyBindingDO::getElderId).distinct().toList();
    }

    @Override
    public boolean isBound(String elderId, Integer userId) {
        return boundElderIds(userId).contains(elderId);
    }

    @Override
    public FamilyMemberDO getFamilyMemberByUserId(Integer userId) {
        return familyMemberMapper.selectOne(new QueryWrapper<FamilyMemberDO>()
                .eq("userId", userId)
                .eq("state", FamilyMemberDO.STATE_NORMAL)
                .last("LIMIT 1"));
    }

    @Override
    public FamilyMemberDO getFamilyMemberByFamilyId(String familyId) {
        return familyMemberMapper.selectById(familyId);
    }

    @Override
    public FamilyMemberDO getOrCreateFamilyMember(Integer userId) {
        FamilyMemberDO existing = getFamilyMemberByUserId(userId);
        if (existing != null) {
            return existing;
        }
        FamilyMemberDO member = new FamilyMemberDO();
        member.setFamilyId("family_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        member.setUserId(userId);
        member.setState(FamilyMemberDO.STATE_NORMAL);
        familyMemberMapper.insert(member);
        return member;
    }

    @Override
    public ElderFamilyBindingDO bind(String elderId, Integer userId, String relation, String isPrimary) {
        FamilyMemberDO member = getOrCreateFamilyMember(userId);
        // 唯一键(elderId, familyId)幂等：曾解绑则重新激活
        ElderFamilyBindingDO existing = bindingMapper.selectOne(new QueryWrapper<ElderFamilyBindingDO>()
                .eq("elderId", elderId)
                .eq("familyId", member.getFamilyId())
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setState(ElderFamilyBindingDO.STATE_BOUND);
            if (StringUtils.hasText(relation)) {
                existing.setRelation(relation);
            }
            if (StringUtils.hasText(isPrimary)) {
                existing.setIsPrimary(isPrimary);
            }
            bindingMapper.updateById(existing);
            return existing;
        }
        ElderFamilyBindingDO binding = new ElderFamilyBindingDO();
        binding.setElderId(elderId);
        binding.setFamilyId(member.getFamilyId());
        binding.setRelation(StringUtils.hasText(relation) ? relation : "other");
        binding.setIsPrimary(StringUtils.hasText(isPrimary) ? isPrimary : "0");
        binding.setState(ElderFamilyBindingDO.STATE_BOUND);
        bindingMapper.insert(binding);
        return binding;
    }

    @Override
    public ElderFamilyBindingDO getBinding(Integer id) {
        return bindingMapper.selectById(id);
    }

    @Override
    public void updateBinding(Integer id, String state, String isPrimary) {
        ElderFamilyBindingDO patch = new ElderFamilyBindingDO();
        patch.setId(id);
        if (StringUtils.hasText(state)) {
            patch.setState(state);
        }
        if (StringUtils.hasText(isPrimary)) {
            patch.setIsPrimary(isPrimary);
        }
        bindingMapper.updateById(patch);
    }
}
