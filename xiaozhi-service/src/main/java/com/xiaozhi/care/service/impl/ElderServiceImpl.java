package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.dal.mysql.mapper.ElderMapper;
import com.xiaozhi.care.service.ElderService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

@Service
public class ElderServiceImpl implements ElderService {

    @Resource
    private ElderMapper elderMapper;

    @Override
    public IPage<ElderDO> page(long pageNo, long pageSize, String name, String phone,
                               String elderId, Collection<String> scopeElderIds) {
        QueryWrapper<ElderDO> qw = new QueryWrapper<>();
        qw.eq("state", ElderDO.STATE_NORMAL);
        if (StringUtils.hasText(name)) {
            qw.like("name", name);
        }
        if (StringUtils.hasText(phone)) {
            qw.like("phone", phone);
        }
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (scopeElderIds != null) {
            // 空集合 = 无可见老人，直接返回空页，避免 in () 生成非法 SQL
            if (scopeElderIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in("elderId", scopeElderIds);
        }
        qw.orderByDesc("createTime");
        return elderMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public ElderDO getByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return elderMapper.selectOne(new QueryWrapper<ElderDO>()
                .eq("deviceId", deviceId)
                .eq("state", ElderDO.STATE_NORMAL)
                .last("LIMIT 1"));
    }

    public ElderDO getByElderId(String elderId) {
        return elderMapper.selectById(elderId);
    }

    @Override
    public List<ElderDO> listByElderIds(Collection<String> elderIds) {
        if (elderIds == null || elderIds.isEmpty()) {
            return List.of();
        }
        return elderMapper.selectList(new QueryWrapper<ElderDO>().in("elderId", elderIds));
    }

    @Override
    public void create(ElderDO elder) {
        if (!StringUtils.hasText(elder.getElderId())) {
            elder.setElderId("elder_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        elder.setState(ElderDO.STATE_NORMAL);
        elderMapper.insert(elder);
    }

    @Override
    public void update(ElderDO elder) {
        elderMapper.updateById(elder);
    }

    @Override
    public void delete(String elderId) {
        ElderDO elder = new ElderDO();
        elder.setElderId(elderId);
        elder.setState(ElderDO.STATE_CANCELLED);
        elderMapper.updateById(elder);
    }
}
