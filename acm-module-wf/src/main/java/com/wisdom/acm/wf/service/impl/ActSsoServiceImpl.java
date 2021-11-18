package com.wisdom.acm.wf.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wisdom.acm.wf.form.TodoBusinessForm;
import com.wisdom.acm.wf.form.TodoMsgForm;
import com.wisdom.acm.wf.service.ActSsoService;
import com.wisdom.acm.wf.service.WfBizTypeService;
import com.wisdom.base.common.form.WfStartProcessForm;
import com.wisdom.base.common.util.BaseUtil;
import com.wisdom.base.common.util.DateUtil;
import com.wisdom.base.common.vo.wf.WfActivityVo;
import com.wisdom.base.common.vo.wf.WfCandidateUserVo;
import com.wisdom.base.common.vo.wf.WfRunProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class ActSsoServiceImpl implements ActSsoService {
    @Autowired
    private WfBizTypeService bizTypeService;
    String baseUrl = "http://58.221.217.2:17003/portal/union-todo-api/todoBusiness";
    String baseMsgUrl = "http://58.221.217.2:17003/portal/union-todo-api/todoMsg";

    @Override
    public int insertTodoMsg(WfStartProcessForm form, WfRunProcessVo procVo) {
        List<TodoMsgForm> list = new ArrayList<>();
        if (form.getNextActPart() != null) {
            for (WfActivityVo w : form.getNextActPart()) {
                for (WfCandidateUserVo wf : w.getCandidateUsers()) {
                    if (wf.getId() != null && wf.getId() != "") {
                        TodoMsgForm f = new TodoMsgForm();
                        f.setMsgCode(procVo.getActivityId());
                        f.setMsgTitle(form.getTitle());
                        TodoMsgForm msgForm = bizTypeService.selectUserNameByUserId(Integer.valueOf(wf.getId()));
                        if (msgForm != null) {
                            f.setMsgReceiverId(msgForm.getMsgSenderId());
                            f.setMsgReceiverName(msgForm.getMsgSenderName());
                            f.setMsgReceiveOrgId(msgForm.getMsgReceiveOrgId());
                            f.setMsgReceiveOrgName(msgForm.getMsgReceiveOrgName());
                        }
                        f.setMsgReceiveTime(DateUtil.formatDateTime(new Date()));
                        f.setMsgStatus(form.getMsgStatus());
                        f.setMsgType("办理");
                        TodoMsgForm todo = bizTypeService.selectUserNameByUserId(Integer.valueOf(form.getUserId()));
                        if (todo != null) {
                            f.setMsgSenderId(todo.getMsgSenderId());
                            f.setMsgSenderName(todo.getMsgSenderName());
                            f.setMsgCreatorId(todo.getMsgSenderId());
                            f.setMsgCreatorName(todo.getMsgSenderName());
                        }
                        f.setBusinessStatus(form.getBusinessStatus());
                        list.add(f);
                    }
                }
            }
        } else {
            TodoMsgForm f = new TodoMsgForm();
            f.setMsgCode(procVo.getActivityId());
            f.setMsgTitle(form.getTitle());
            TodoMsgForm msgForm = bizTypeService.selectUserNameByUserId(Integer.valueOf(procVo.getUserId()));
            if (msgForm != null) {
                f.setMsgReceiverId(msgForm.getMsgSenderId());
                f.setMsgReceiverName(msgForm.getMsgSenderName());
                f.setMsgReceiveOrgId(msgForm.getMsgReceiveOrgId());
                f.setMsgReceiveOrgName(msgForm.getMsgReceiveOrgName());
            }
            f.setMsgReceiveTime(DateUtil.formatDateTime(new Date()));
            f.setMsgStatus(form.getMsgStatus());
            f.setMsgType("办理");
            TodoMsgForm todo = bizTypeService.selectUserNameByUserId(Integer.valueOf(form.getUserId()));
            if (todo != null) {
                f.setMsgSenderId(todo.getMsgSenderId());
                f.setMsgSenderName(todo.getMsgSenderName());
                f.setMsgCreatorId(todo.getMsgSenderId());
                f.setMsgCreatorName(todo.getMsgSenderName());
            }
            f.setBusinessStatus(form.getBusinessStatus());
            list.add(f);
        }
        RestTemplate restTemplate = new RestTemplate();
        if (list != null && list.size() > 0) {
            for (TodoMsgForm tf : list) {
                tf.setSourceId("37ee39aa-6dc3-364f-0e0a-2ec1b50b076e");
                tf.setSourceName("调度系统");
                tf.setTenantCode("platform");
            }
        }
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(list), headers);
        String responseEntity = restTemplate.postForObject(baseMsgUrl, requestEntity, String.class);

        return 0;
    }

    @Override
    public int insertTodoBusiness(WfStartProcessForm form, WfRunProcessVo procVo) {
        List<TodoBusinessForm> list = new ArrayList<>();
        TodoBusinessForm busform = new TodoBusinessForm();
        busform.setBusinessCode(procVo.getProcInstId());
        busform.setBusinessName(form.getTitle());
        busform.setStatus(form.getStatus());
        TodoMsgForm msgForm = bizTypeService.selectUserNameByUserId(Integer.valueOf(form.getUserId()));
        if (msgForm != null) {
            busform.setBusinessCreatorId(msgForm.getMsgSenderId());
        }
        busform.setBeginTime(DateUtil.formatDateTime(new Date()));
        busform.setEndTime(DateUtil.formatDateTime(new Date()));
        busform.setCurrentTaskName(procVo.getActivityName());
        list.add(busform);
        RestTemplate restTemplate = new RestTemplate();
        if (list != null && list.size() > 0) {
            for (TodoBusinessForm f : list) {
                f.setSourceId("37ee39aa-6dc3-364f-0e0a-2ec1b50b076e");
                f.setSourceName("调度系统");
                f.setTenantCode("platform");
            }
        }
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(list), headers);
        String responseEntity = restTemplate.postForObject(baseUrl, requestEntity, String.class);
        return 0;
    }
}
