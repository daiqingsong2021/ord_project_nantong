package com.wisdom.acm.wf.form;

/**
 * @author fqq
 * @since 2021/9/23
 */
public class TodoBusinessForm {
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
     * 业务数据标识
     */
    private String businessCode;
    /**
     * 类型编码
     */
    private String businessTypeId;
    /**
     * 类型名称
     */
    private String businessTypeName;
    /**
     * 标题
     */
    private String businessName;
    /**
     * 开始时间
     */
    private String beginTime;
    /**
     * 结束时间
     */
    private String endTime;
    /**
     * 状态
     */
    private String status;
    /**
     * 创建人id
     */
    private String businessCreatorId;
    /**
     * 创建人名称
     */
    private String businessCreatorName;
    /**
     * 创建组织id
     */
    private String createOrgId;
    /**
     * 创建组织名称
     */
    private String createOrgName;
    /**
     * 当前任务id
     */
    private String currentTaskId;
    /**
     * 当前任务名称
     */
    private String currentTaskName;
    /**
     * 当前任务处理人id
     */
    private String currentTaskUserId;
    /**
     * 当前任务处理人姓名
     */
    private String currentTaskUserName;
    /**
     * pc端链接
     */
    private String businessPcUrl;
    /**
     * 移动端链接
     */
    private String businessMobileUrl;
    /**
     * 缓急程度
     */
    private String important;
    /**
     * 密级
     */
    private String secret;

    public TodoBusinessForm(){

    }

    public TodoBusinessForm(String tenantCode, String sourceId, String sourceName) {
        this.tenantCode = tenantCode;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getBusinessTypeId() {
        return businessTypeId;
    }

    public void setBusinessTypeId(String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    public String getBusinessTypeName() {
        return businessTypeName;
    }

    public void setBusinessTypeName(String businessTypeName) {
        this.businessTypeName = businessTypeName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBusinessCreatorId() {
        return businessCreatorId;
    }

    public void setBusinessCreatorId(String businessCreatorId) {
        this.businessCreatorId = businessCreatorId;
    }

    public String getBusinessCreatorName() {
        return businessCreatorName;
    }

    public void setBusinessCreatorName(String businessCreatorName) {
        this.businessCreatorName = businessCreatorName;
    }

    public String getCreateOrgId() {
        return createOrgId;
    }

    public void setCreateOrgId(String createOrgId) {
        this.createOrgId = createOrgId;
    }

    public String getCreateOrgName() {
        return createOrgName;
    }

    public void setCreateOrgName(String createOrgName) {
        this.createOrgName = createOrgName;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public String getCurrentTaskName() {
        return currentTaskName;
    }

    public void setCurrentTaskName(String currentTaskName) {
        this.currentTaskName = currentTaskName;
    }

    public String getCurrentTaskUserId() {
        return currentTaskUserId;
    }

    public void setCurrentTaskUserId(String currentTaskUserId) {
        this.currentTaskUserId = currentTaskUserId;
    }

    public String getCurrentTaskUserName() {
        return currentTaskUserName;
    }

    public void setCurrentTaskUserName(String currentTaskUserName) {
        this.currentTaskUserName = currentTaskUserName;
    }

    public String getBusinessPcUrl() {
        return businessPcUrl;
    }

    public void setBusinessPcUrl(String businessPcUrl) {
        this.businessPcUrl = businessPcUrl;
    }

    public String getBusinessMobileUrl() {
        return businessMobileUrl;
    }

    public void setBusinessMobileUrl(String businessMobileUrl) {
        this.businessMobileUrl = businessMobileUrl;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
