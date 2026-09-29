package com.xiaozhi.care.dal.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommandLogMapper extends BaseMapper<CommandLogDO> {
}
