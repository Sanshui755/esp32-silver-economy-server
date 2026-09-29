package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 老人-子女绑定表 elder_family_binding
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elder_family_binding")
public class ElderFamilyBindingDO extends BaseDO {

    public static final String STATE_BOUND = "1";
    public static final String STATE_UNBOUND = "0";

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String elderId;
    private String familyId;
    /** 关系(son/daughter/other) */
    private String relation;
    /** 主监护人 */
    private String isPrimary;
    /** 1-绑定 0-解绑 */
    private String state;
}
