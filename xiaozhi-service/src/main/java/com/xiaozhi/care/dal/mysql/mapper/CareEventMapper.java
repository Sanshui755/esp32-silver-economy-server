package com.xiaozhi.care.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CareEventMapper extends BaseMapper<CareEventDO> {
}
