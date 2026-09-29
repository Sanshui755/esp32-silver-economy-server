package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.ElderFamilyBindingDO;
import com.xiaozhi.care.dal.mysql.dataobject.FamilyMemberDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "老人-子女绑定关系")
public class BindingResp implements Serializable {

    @Schema(description = "绑定记录ID")
    private Integer id;

    private String elderId;
    private String familyId;
    private String relation;
    private String isPrimary;
    private String state;
    private LocalDateTime createTime;

    @Schema(description = "家属关联账号ID(sys_user.userId)")
    private Integer userId;

    @Schema(description = "家属账号名")
    private String username;

    @Schema(description = "家属称呼")
    private String nickname;

    public static BindingResp from(ElderFamilyBindingDO b, FamilyMemberDO member, String username) {
        BindingResp r = new BindingResp();
        r.setId(b.getId());
        r.setElderId(b.getElderId());
        r.setFamilyId(b.getFamilyId());
        r.setRelation(b.getRelation());
        r.setIsPrimary(b.getIsPrimary());
        r.setState(b.getState());
        r.setCreateTime(b.getCreateTime());
        if (member != null) {
            r.setUserId(member.getUserId());
            r.setNickname(member.getNickname());
        }
        r.setUsername(username);
        return r;
    }
}
