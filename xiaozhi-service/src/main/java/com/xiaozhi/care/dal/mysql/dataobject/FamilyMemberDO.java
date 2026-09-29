package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子女/家属表 family_member（账号复用 sys_user，本表只存关怀侧档案）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_member")
public class FamilyMemberDO extends BaseDO {

    public static final String STATE_NORMAL = "1";

    /** 家属ID(业务主键 family_xxx) */
    @TableId(value = "familyId", type = IdType.INPUT)
    private String familyId;

    /** 关联账号(sys_user.userId)，唯一 */
    private Integer userId;
    /** 称呼(如: 大儿子) */
    private String nickname;
    private String phone;
    /** 1-正常 0-禁用 */
    private String state;
}
