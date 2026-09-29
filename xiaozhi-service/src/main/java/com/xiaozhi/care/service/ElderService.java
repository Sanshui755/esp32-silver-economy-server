package com.xiaozhi.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;

import java.util.Collection;
import java.util.List;

/**
 * 老人管理
 */
public interface ElderService {

    /** 分页查询；scopeElderIds 为 null 表示不限（管理员），非空则只查范围内老人 */
    IPage<ElderDO> page(long pageNo, long pageSize, String name, String phone,
                        String elderId, Collection<String> scopeElderIds);

    ElderDO getByElderId(String elderId);

    /** 按绑定设备 MAC 反查老人（设备端 REST 认证用），未绑定返回 null */
    ElderDO getByDeviceId(String deviceId);

    List<ElderDO> listByElderIds(Collection<String> elderIds);

    void create(ElderDO elder);

    void update(ElderDO elder);

    /** 逻辑注销：state=0 */
    void delete(String elderId);
}
