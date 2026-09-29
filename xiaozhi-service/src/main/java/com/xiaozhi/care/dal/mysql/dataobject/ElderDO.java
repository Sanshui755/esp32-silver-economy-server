package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 老人信息表 elder
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elder")
public class ElderDO extends BaseDO {

    public static final String STATE_NORMAL = "1";
    public static final String STATE_CANCELLED = "0";

    /** 老人ID(业务主键 elder_xxx) */
    @TableId(value = "elderId", type = IdType.INPUT)
    private String elderId;

    private String name;
    /** 1-男 0-女 */
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String address;
    private String avatar;
    /** 绑定设备ID(care_device.deviceId) */
    private String deviceId;
    /** 健康备注(慢病/用药史) */
    private String healthNote;
    /** 1-正常 0-注销 */
    private String state;
    /** 创建人(sys_user.userId) */
    private Integer userId;
}
