package com.wisdom.acm.szxm.service.app;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.wisdom.acm.szxm.vo.app.AppUserInfoVo;
import com.wisdom.acm.szxm.vo.rygl.KqRecordVo;
import com.wisdom.base.common.form.SysMessageAddForm;

import java.util.List;
import java.util.Map;

/**
 * 移动端Service
 */
public interface EwxService
{

    /**
     * 获取企业微信平台鉴权code
     * @param code
     * @return
     */
    public AppUserInfoVo getEwxAppUserInfo(String code);

    /**
     * 获取东软平台鉴权code
     * @param code
     * @return
     */
    AppUserInfoVo getNeusoftAppUserInfo(String code);

    /**
     * 获取企业微信的jsapi_ticket
     * @param url
     * @return
     */
    Map<String, Object> getJsApiTicket(String url);

    /**
     * 企业微信移动端消息推送
     * @param sysMessageAddForm
     */
    void sendEwxMessage(SysMessageAddForm sysMessageAddForm);

    /**
     * 企业微信获取 考勤数据
     * @param reqMap
     * @return
     */
    List<KqRecordVo> getKqRecord(Map<String, Object> reqMap);

    /**
     * 品高移动端待办消息推送
     * @param paramsMap
     */
    void sendNeuSoftMessage(Map<String,String> paramsMap);
}