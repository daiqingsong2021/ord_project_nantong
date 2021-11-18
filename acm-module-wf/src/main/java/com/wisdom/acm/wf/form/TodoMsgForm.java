package com.wisdom.acm.wf.form;

import lombok.Data;

@Data
public class TodoMsgForm {
    /**
     * 所属租户编码
     */
    private String tenantCode;
    /**
     * 接入id
     */
    private String sourceId;
    /**
     * 接入标识
     */
    private String sourceName;
    /**
     * 消息标识
     */
    private String msgCode;
    /**
     * 消息标题
     */
    private String msgTitle;
    /**
     * 消息接受人id
     */
    private String msgReceiverId;
    /**
     * 消息接受人名称
     */
    private String msgReceiverName;
    /**
     * 消息所属部门id
     */
    private String msgReceiveOrgId;
    /**
     * 消息所属部门名称
     */
    private String msgReceiveOrgName;
    /**
     * 消息发送人id
     */
    private String msgSenderId;
    /**
     * 消息发送人名称
     */
    private String msgSenderName;
    /**
     * 消息接收时间
     */
    private String msgReceiveTime;
    /**
     * 状态
     */
    private String msgStatus;
    /**
     * 类型
     */
    private String msgType;
    /**
     * 拟稿人id
     */
    private String msgCreatorId;
    /**
     * 拟稿人姓名
     */
    private String msgCreatorName;
    /**
     * 流程状态
     */
    private String businessStatus;
    /**
     * pc端链接
     */
    private String businessPcUrl;


    public TodoMsgForm() {

    }

    public TodoMsgForm(String tenantCode, String sourceId, String sourceName) {
        this.tenantCode = tenantCode;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
    }


}
