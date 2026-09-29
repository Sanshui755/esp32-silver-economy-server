-- ============================================================
-- V100: 老年关怀模块建表 + 权限点
-- 智聊（ICAN 参赛项目）二次开发：老年端 ⇄ 子女端 端到端通信
-- 原则：只增不改；逻辑外键（项目惯例，无 FOREIGN KEY）；
--       统一 InnoDB / utf8mb4_unicode_ci；时间列满足 BaseDO 自动填充
-- ============================================================

-- 老人信息表
CREATE TABLE `elder` (
  `elderId`     varchar(32)  NOT NULL COMMENT '老人ID(业务主键 elder_xxx)',
  `name`        varchar(50)  NOT NULL COMMENT '姓名',
  `gender`      enum('1','0')     DEFAULT '1' COMMENT '1-男 0-女',
  `birthday`    date              DEFAULT NULL COMMENT '出生日期',
  `phone`       varchar(20)       DEFAULT NULL COMMENT '联系电话',
  `address`     varchar(255)      DEFAULT NULL COMMENT '居住地址',
  `avatar`      varchar(255)      DEFAULT NULL COMMENT '头像',
  `deviceId`    varchar(64)       DEFAULT NULL COMMENT '绑定设备ID(care_device.deviceId)',
  `healthNote`  text              DEFAULT NULL COMMENT '健康备注(慢病/用药史)',
  `state`       enum('1','0')     DEFAULT '1' COMMENT '1-正常 0-注销',
  `userId`      int unsigned NOT NULL COMMENT '创建人(sys_user)',
  `createTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`elderId`),
  KEY `idx_userId` (`userId`),
  KEY `idx_deviceId` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人信息表';

-- 子女/家属表（账号复用 sys_user，本表只存关怀侧档案）
CREATE TABLE `family_member` (
  `familyId`    varchar(32)  NOT NULL COMMENT '家属ID(业务主键 family_xxx)',
  `userId`      int unsigned NOT NULL COMMENT '关联账号(sys_user.userId)',
  `nickname`    varchar(50)       DEFAULT NULL COMMENT '称呼(如: 大儿子)',
  `phone`       varchar(20)       DEFAULT NULL COMMENT '联系电话',
  `state`       enum('1','0')     DEFAULT '1' COMMENT '1-正常 0-禁用',
  `createTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`familyId`),
  UNIQUE KEY `uk_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='子女/家属表';

-- 老人-子女绑定表
CREATE TABLE `elder_family_binding` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT,
  `elderId`     varchar(32)  NOT NULL COMMENT '老人ID',
  `familyId`    varchar(32)  NOT NULL COMMENT '家属ID',
  `relation`    varchar(20)       DEFAULT NULL COMMENT '关系(son/daughter/other)',
  `isPrimary`   enum('1','0')     DEFAULT '0' COMMENT '主监护人',
  `state`       enum('1','0')     DEFAULT '1' COMMENT '1-绑定 0-解绑',
  `createTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_elder_family` (`elderId`,`familyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人-子女绑定表';

-- 关怀事件表（摔倒/SOS/提醒触发/设备离线等；msgId 幂等）
CREATE TABLE `care_event` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `msgId`       varchar(64)  NOT NULL COMMENT '消息幂等键(UUID)',
  `eventType`   varchar(32)  NOT NULL COMMENT 'fall/sos/medication_reminder/device_offline/heartbeat/...',
  `elderId`     varchar(32)  NOT NULL COMMENT '老人ID',
  `severity`    enum('info','warning','critical') NOT NULL DEFAULT 'info' COMMENT '严重程度',
  `source`      varchar(32)       DEFAULT 'elder_device' COMMENT 'elder_device/family_app/server',
  `payload`     json              DEFAULT NULL COMMENT '事件原始数据(电量/位置/置信度)',
  `status`      enum('0','1')     DEFAULT '0' COMMENT '0-未处理 1-已处理',
  `handledBy`   int unsigned      DEFAULT NULL COMMENT '处理人(sys_user)',
  `handledTime` datetime(3)       DEFAULT NULL COMMENT '处理时间',
  `createTime`  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msgId` (`msgId`),
  KEY `idx_elder_type_time` (`elderId`,`eventType`,`createTime`),
  KEY `idx_status_time` (`status`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关怀事件表';

-- 吃药提醒计划表
CREATE TABLE `medication_plan` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `planId`      varchar(36)  NOT NULL COMMENT '计划业务ID',
  `elderId`     varchar(32)  NOT NULL COMMENT '老人ID',
  `medicine`    varchar(100) NOT NULL COMMENT '药名',
  `dosage`      varchar(50)       DEFAULT NULL COMMENT '用量(如 1片)',
  `takeTime`    time         NOT NULL COMMENT '服药时间',
  `repeatRule`  varchar(16)       DEFAULT 'daily' COMMENT 'daily/weekdays/custom',
  `weekdays`    varchar(16)       DEFAULT NULL COMMENT 'custom时: 1,3,5',
  `voicePrompt` varchar(255)      DEFAULT NULL COMMENT 'TTS播报文案',
  `state`       enum('1','0')     DEFAULT '1' COMMENT '1-启用 0-停用',
  `createdBy`   int unsigned NOT NULL COMMENT '创建家属(sys_user)',
  `createTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_planId` (`planId`),
  KEY `idx_elder_state` (`elderId`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='吃药提醒计划表';

-- 吃药执行记录表
CREATE TABLE `medication_record` (
  `id`            bigint NOT NULL AUTO_INCREMENT,
  `planId`        varchar(36) NOT NULL COMMENT '计划ID',
  `elderId`       varchar(32) NOT NULL COMMENT '老人ID',
  `plannedTime`   datetime    NOT NULL COMMENT '计划服药时间',
  `status`        enum('pending','taken','missed') NOT NULL DEFAULT 'pending',
  `takenTime`     datetime(3)      DEFAULT NULL COMMENT '实际确认时间',
  `confirmSource` varchar(32)      DEFAULT NULL COMMENT 'elder_device/family_app',
  `msgId`         varchar(64)      DEFAULT NULL COMMENT 'medication_taken 幂等键',
  `createTime`    datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msgId` (`msgId`),
  KEY `idx_elder_plan` (`elderId`,`planId`,`plannedTime`),
  KEY `idx_planned_status` (`plannedTime`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='吃药执行记录表';

-- 关怀设备状态表（不污染 sys_device；电量/心跳/在线是 MQTT 语义）
CREATE TABLE `care_device` (
  `deviceId`       varchar(64) NOT NULL COMMENT '设备ID(MQTT clientId)',
  `elderId`        varchar(32)      DEFAULT NULL COMMENT '绑定老人',
  `online`         enum('1','0')    DEFAULT '0' COMMENT 'MQTT 在线状态',
  `battery`        int              DEFAULT NULL COMMENT '电量 0-100',
  `rssi`           int              DEFAULT NULL COMMENT 'WiFi 信号强度',
  `ip`             varchar(45)      DEFAULT NULL COMMENT '设备IP',
  `firmware`       varchar(50)      DEFAULT NULL COMMENT '固件版本',
  `lastHeartbeat`  datetime(3)      DEFAULT NULL COMMENT '最后心跳时间',
  `lastOnlineTime` datetime(3)      DEFAULT NULL COMMENT '最后上线时间',
  `createTime`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`deviceId`),
  KEY `idx_elder` (`elderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关怀设备状态表';

-- 命令下发与确认记录表
CREATE TABLE `command_log` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `msgId`       varchar(64) NOT NULL COMMENT '命令幂等键(UUID)',
  `commandType` varchar(32) NOT NULL COMMENT 'set_medication_plan/delete_medication_plan/care_message/query_status',
  `elderId`     varchar(32) NOT NULL COMMENT '目标老人',
  `familyId`    varchar(32)      DEFAULT NULL COMMENT '发起家属(管理员下发为NULL)',
  `operatorId`  int unsigned NOT NULL COMMENT '操作人(sys_user)',
  `payload`     json             DEFAULT NULL COMMENT '命令内容',
  `status`      enum('sent','delivered','acked','failed') NOT NULL DEFAULT 'sent',
  `ackTime`     datetime(3)      DEFAULT NULL COMMENT '确认时间',
  `ackPayload`  json             DEFAULT NULL COMMENT '确认内容',
  `createTime`  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msgId` (`msgId`),
  KEY `idx_elder_time` (`elderId`,`createTime`),
  KEY `idx_family` (`familyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='命令下发与确认记录表';

-- ============================================================
-- 权限点（沿用 sys_permission 结构：父菜单 + 子菜单 + API）
-- ============================================================
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`path`,`component`,`icon`,`sort`,`visible`,`status`)
VALUES (NULL,'老年关怀','system:care','menu','/care','common/PageView','heart',3,'1','1');

SET @careId = (SELECT `permissionId` FROM `sys_permission` WHERE `permissionKey`='system:care');
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`path`,`component`,`icon`,`sort`,`visible`,`status`) VALUES
(@careId,'老人管理','system:care:elder','menu','/care/elder','care/ElderView','team',1,'1','1'),
(@careId,'事件中心','system:care:event','menu','/care/event','care/CareEventView','alert',2,'1','1'),
(@careId,'吃药提醒','system:care:medication','menu','/care/medication','care/MedicationView','medicine',3,'1','1'),
(@careId,'设备状态','system:care:device','menu','/care/device','care/CareDeviceView','wifi',4,'1','1');

-- API 权限：老人 CRUD
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`sort`,`visible`,`status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (SELECT '老人列表' AS `name`,'system:care:elder:api:list' AS `permissionKey`,1 AS `sort`
      UNION ALL SELECT '老人创建','system:care:elder:api:create',2
      UNION ALL SELECT '老人更新','system:care:elder:api:update',3
      UNION ALL SELECT '老人删除','system:care:elder:api:delete',4) src
JOIN `sys_permission` p ON p.`permissionKey`='system:care:elder';

-- API 权限：绑定管理
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`sort`,`visible`,`status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (SELECT '绑定列表' AS `name`,'system:care:binding:api:list' AS `permissionKey`,1 AS `sort`
      UNION ALL SELECT '绑定创建','system:care:binding:api:create',2
      UNION ALL SELECT '绑定删除','system:care:binding:api:delete',3) src
JOIN `sys_permission` p ON p.`permissionKey`='system:care:elder';

-- API 权限：事件中心
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`sort`,`visible`,`status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (SELECT '事件列表' AS `name`,'system:care:event:api:list' AS `permissionKey`,1 AS `sort`
      UNION ALL SELECT '事件处理','system:care:event:api:handle',2) src
JOIN `sys_permission` p ON p.`permissionKey`='system:care:event';

-- API 权限：吃药提醒
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`sort`,`visible`,`status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (SELECT '计划列表' AS `name`,'system:care:medication:api:list' AS `permissionKey`,1 AS `sort`
      UNION ALL SELECT '计划创建','system:care:medication:api:create',2
      UNION ALL SELECT '计划更新','system:care:medication:api:update',3
      UNION ALL SELECT '计划删除','system:care:medication:api:delete',4
      UNION ALL SELECT '记录列表','system:care:medication:api:record',5) src
JOIN `sys_permission` p ON p.`permissionKey`='system:care:medication';

-- API 权限：设备状态与命令
INSERT INTO `sys_permission` (`parentId`,`name`,`permissionKey`,`permissionType`,`sort`,`visible`,`status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (SELECT '设备状态' AS `name`,'system:care:device:api:list' AS `permissionKey`,1 AS `sort`
      UNION ALL SELECT '命令下发','system:care:command:api:send',2
      UNION ALL SELECT '命令记录','system:care:command:api:list',3) src
JOIN `sys_permission` p ON p.`permissionKey`='system:care:device';

-- 管理员角色(authRoleId=1)授予全部关怀权限
INSERT INTO `sys_auth_role_permission` (`authRoleId`,`permissionId`)
SELECT 1,`permissionId` FROM `sys_permission` WHERE `permissionKey` LIKE 'system:care%';
