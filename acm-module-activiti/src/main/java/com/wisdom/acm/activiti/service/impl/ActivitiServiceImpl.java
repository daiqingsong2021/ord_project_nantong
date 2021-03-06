package com.wisdom.acm.activiti.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.wisdom.acm.activiti.mapper.ActivitiMapper;
import com.wisdom.acm.activiti.po.WorkflowPo;
import com.wisdom.acm.activiti.service.ActivitiService;
import com.wisdom.acm.activiti.util.ExprUtil;
import com.wisdom.acm.activiti.vo.WorkFlowLogVo;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.form.*;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.DateUtil;
import com.wisdom.base.common.util.FormatUtil;
import com.wisdom.base.common.util.JsonUtil;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.calc.calendar.Tools;
import com.wisdom.base.common.vo.base.ActModelVo;
import com.wisdom.base.common.vo.sys.UserVo;
import com.wisdom.base.common.vo.wf.*;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.javax.el.ExpressionFactory;
import org.activiti.engine.impl.javax.el.ValueExpression;
import org.activiti.engine.impl.juel.ExpressionFactoryImpl;
import org.activiti.engine.impl.juel.SimpleContext;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.impl.util.io.InputStreamSource;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ActivitiServiceImpl extends BaseService<ActivitiMapper, WorkflowPo> implements ActivitiService {

    /**
     * ????????????
     */
    @Autowired
    private TaskService taskService;

    /**
     * ???????????????
     */
    @Autowired
    private RuntimeService runtimeService;

    /**
     * ????????????
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * ????????????
     */
    @Autowired
    private HistoryService historyService;

    /**
     * ????????????
     */
    @Autowired
    private IdentityService identityService;

    /**
     * ??????
     */
    @Autowired
    private ProcessEngine processEngine;

    /**
     * ?????????
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * ??????????????????
     *
     * @param procDefId ????????????ID
     * @return ????????????
     */
    private ProcessDefinitionEntity getProcessDefinitionEntity(String procDefId) {
        return (ProcessDefinitionEntity) ((RepositoryServiceImpl) this.repositoryService).getDeployedProcessDefinition(procDefId);
    }

    /**
     * ???????????????????????????
     *
     * @param task ????????????
     * @return ?????????????????????
     */
    @Override
    public List<PvmActivity> getNextUserActivity(TaskInfo task) {
        List<PvmActivity> nextActivitys = new ArrayList<>();
        if (!ObjectUtils.isEmpty(task)) {
            Execution execution = this.runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            Map<String, Object> vars = execution != null ? this.runtimeService.getVariables(execution.getId()) : null;
            ProcessDefinitionEntity procDefEnt = this.getProcessDefinitionEntity(task.getProcessDefinitionId());
            ActivityImpl actImpl = procDefEnt.findActivity(task.getTaskDefinitionKey()); //????????????????????????
            nextActivitys = this.getNextUserActivity(actImpl, vars);   //?????????????????????????????????
        }
        return nextActivitys;
    }

    /**
     * ???????????????????????????
     *
     * @param actImpl ??????ID
     * @param vars    ??????????????????
     * @return ????????????????????????
     */
    private List<PvmActivity> getNextUserActivity(ActivityImpl actImpl, Map<String, Object> vars) {
        List<PvmActivity> nextActs = new ArrayList<>();
        if (!ObjectUtils.isEmpty(actImpl)) {
            getNextUserActivity(nextActs, actImpl.getOutgoingTransitions(), vars);   //???????????????????????????
        }
        return nextActs;
    }

    /**
     * ???????????????????????????,
     * ???????????????????????????????????????????????????,
     * ????????????????????????????????????, ??????????????????Id??????, ??????????????????Id?????????execution??????????????????????????????Id???key????????????,
     * ??????????????????????????????????????????????????????el?????????, ?????????el????????????????????????????????????????????????
     *
     * @param nextActs       ??????????????????
     * @param outTransitions ??????????????????????????????, ???????????????????????????????????????, ????????????????????????????????????????????????${money>1000}, ??????????????????????????????variables??????money>1000, ?????????????????????????????????
     * @param vars           ??????????????????
     */
    private void getNextUserActivity(List<PvmActivity> nextActs, List<PvmTransition> outTransitions, Map<String, Object> vars) {
        for (PvmTransition pt : outTransitions) {
            String srcType = Tools.toString(pt.getSource().getProperty("type")); //??????????????????,????????????=exclusiveGateway;????????????=ParallelGateway;????????????=InclusiveGateway
            PvmActivity ac = pt.getDestination(); //???????????????????????????
            String outType = Tools.toString(ac.getProperty("type"));
            String cond = FormatUtil.toString(pt.getProperty("conditionText")).trim();  //??????????????????????????????????????????
            if (!Tools.isEmpty(vars) && !Tools.isEmpty(cond) && !cond.replace(" ", "").contains("expr.existActivity(nextActPart,") //????????????????????????
                && ("exclusiveGateway".equals(srcType) || "exclusiveGateway".equals(outType)
                || "inclusiveGateway".equals(srcType) || "inclusiveGateway".equals(outType)
                || "userTask".equals(srcType) || "userTask".equals(outType))) { //??????????????????????????????????????????
                if (!this.parseExpression(cond, vars)) {
                    continue;
                }
            }
            if ("userTask".equals(outType)) {
                nextActs.add(ac);
            } else {
                this.getNextUserActivity(nextActs, ac.getOutgoingTransitions(), vars);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param expression el?????????key??????
     * @param execution  ????????????Id??????
     * @return VariableValue
     */
    private Boolean parseExpression(String expression, Execution execution) {
        if (!Tools.isEmpty(expression) && !Tools.isEmpty(execution)) {
            Map<String, Object> vars = this.runtimeService.getVariables(execution.getId());
            if (!ObjectUtils.isEmpty(vars)) {
                if (this.parseExpression(expression, vars)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ??????key???value??????el???????????????????????????
     *
     * @param expression el?????????
     * @param vars       el????????????????????????
     * @return booelan
     */
    private Boolean parseExpression(String expression, Map<String, Object> vars) {
        if (!ObjectUtils.isEmpty(expression) && !ObjectUtils.isEmpty(vars)) {
            ExpressionFactory factory = new ExpressionFactoryImpl();
            SimpleContext context = new SimpleContext();
            try {
                context.setFunction("expr", "equals", ExprUtil.class.getMethod("equals", Object.class, Object.class));
                context.setFunction("expr", "equalsIgnoreCase", ExprUtil.class.getMethod("equalsIgnoreCase", Object.class, Object.class));
                context.setFunction("expr", "contains", ExprUtil.class.getMethod("contains", Object.class, Object.class));
                context.setFunction("expr", "startsWith", ExprUtil.class.getMethod("startsWith", Object.class, Object.class));
                context.setFunction("expr", "endsWith", ExprUtil.class.getMethod("endsWith", Object.class, Object.class));
                context.setFunction("expr", "matches", ExprUtil.class.getMethod("matches", Object.class, Object.class));
                context.setFunction("expr", "split", ExprUtil.class.getMethod("split", Object.class, Object.class, Object.class));
                context.setFunction("expr", "indexOf", ExprUtil.class.getMethod("indexOf", Object.class, Object.class));
                context.setFunction("expr", "lastIndexOf", ExprUtil.class.getMethod("lastIndexOf", Object.class, Object.class));
                context.setFunction("expr", "existActivity", ExprUtil.class.getMethod("existActivity", Object.class, Object.class));
                for (Map.Entry<String, Object> d : vars.entrySet()) {
                    context.setVariable(d.getKey(), factory.createValueExpression(d.getValue(), d.getValue().getClass()));
                }
            } catch (NoSuchMethodException e) {
                throw new BaseException(e);
            }
            //SimpleContext??????????????????????????????:????????????????????????????????????????????????????????????.??????
            expression = expression.contains("expr.") ? expression.replace("expr.", "expr:") : expression;
            ValueExpression e = factory.createValueExpression(context, expression, Boolean.class);
            return Tools.parseBoolean(e.getValue(context));
        } else {
            return false;
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param procDefEnt    ????????????
     * @param activity      ??????????????????
     * @param nextActivitys ??????????????????
     * @return ?????????
     */
    private WfCandidateVo getTaskDefCandidate(ProcessDefinitionEntity procDefEnt, ActivityImpl activity, List<PvmActivity> nextActivitys) {
        WfCandidateVo vo = new WfCandidateVo();
        if (!ObjectUtils.isEmpty(nextActivitys)) {
            List<WfActivityVo> activities = new ArrayList<>();
            vo.setActivityOnly(this.isActivityOnly(activity));
            for (PvmActivity pa : nextActivitys) {
                TaskDefinition taskDef = procDefEnt.getTaskDefinitions().get(pa.getId());
                WfActivityVo activityVo = new WfActivityVo(taskDef.getKey(), taskDef.getNameExpression().getExpressionText());
                activityVo.setCandidateGroups(this.getCandidateGroup(taskDef));
                activityVo.setCandidateUsers(this.getCandidateUser(taskDef));
                activities.add(activityVo);
            }
            vo.setActivities(activities);
        }
        return vo;
    }

    /**
     * ????????????????????????????????????
     *
     * @param activity ????????????
     * @return ???/???
     */
    private boolean isActivityOnly(ActivityImpl activity) {
        boolean only = true;
        List<PvmTransition> pts = activity.getOutgoingTransitions();
        if (!Tools.isEmpty(pts)) {
            for (PvmTransition pt : pts) {
                String type = Tools.toString(pt.getDestination().getProperty("type"));
                if (type.equalsIgnoreCase("parallelGateway") || type.equalsIgnoreCase("inclusiveGateway")) {
                    only = false;
                }
            }
        }
        return only;
    }

    /**
     * ???????????????
     *
     * @param taskDef ????????????
     */
    private List<WfCandidateGroupVo> getCandidateGroup(TaskDefinition taskDef) {
        List<WfCandidateGroupVo> candidates = new ArrayList<>();
        Set<Expression> candidateExp = taskDef.getCandidateGroupIdExpressions();
        if (!ObjectUtils.isEmpty(candidateExp)) {
            for (Expression exp : candidateExp) {
                candidates.add(new WfCandidateGroupVo(exp.getExpressionText()));
            }
        }
        return candidates;
    }

    /**
     * ???????????????
     *
     * @param taskDef ????????????
     */
    private List<WfCandidateUserVo> getCandidateUser(TaskDefinition taskDef) {
        List<WfCandidateUserVo> candidates = new ArrayList<>();
        Set<Expression> candidateExp = taskDef.getCandidateUserIdExpressions();
        if (!ObjectUtils.isEmpty(candidateExp)) {
            for (Expression exp : candidateExp) {
                candidates.add(new WfCandidateUserVo(exp.getExpressionText()));
            }
        }
        return candidates;
    }

    /**
     * ?????????????????????(?????????????????????)
     *
     * @param procDefEnt ????????????
     * @return ?????????????????????
     */
    private ActivityImpl getFirstActivity(ProcessDefinitionEntity procDefEnt) {
        List<ActivityImpl> tasks = procDefEnt.getActivities();
        if (!ObjectUtils.isEmpty(tasks)) {
            for (ActivityImpl act : tasks) {
                if ("startEvent".equals(FormatUtil.toString(act.getProperty("type")))) {
                    if (!ObjectUtils.isEmpty(act.getOutgoingTransitions())) {
                        return (ActivityImpl) act.getOutgoingTransitions().get(0).getDestination();
                    }
                }
            }
        }
        return null;
    }

    /**
     * ?????????????????????(?????????????????????)
     *
     * @param procDefEnt ????????????
     * @return ?????????????????????
     */
    private List<ActivityImpl> getLastActivity(ProcessDefinitionEntity procDefEnt) {
        List<ActivityImpl> lastTasks = new ArrayList<>();
        List<ActivityImpl> tasks = procDefEnt.getActivities();
        if (!ObjectUtils.isEmpty(tasks)) {
            for (ActivityImpl act : tasks) {
                if ("endEvent".equals(FormatUtil.toString(act.getProperty("type")))) {
                    if (!ObjectUtils.isEmpty(act.getIncomingTransitions())) {
                        for (PvmTransition pt : act.getIncomingTransitions()) {
                            lastTasks.add((ActivityImpl) pt.getSource());
                        }
                    }
                }
            }
        }
        return lastTasks;
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param procDefKey ????????????
     * @return ??????????????????????????????
     */
    @Override
    public WfCandidateVo getStartNextCandidate(String procDefKey, Map<String, Object> vars) {
        WfCandidateVo candidate = new WfCandidateVo();
        ProcessDefinition procDef = this.repositoryService.createProcessDefinitionQuery().processDefinitionKey(procDefKey).latestVersion().singleResult();
        if (!ObjectUtils.isEmpty(procDef)) {
            ProcessDefinitionEntity procDefEnt = this.getProcessDefinitionEntity(procDef.getId());
            ActivityImpl actImpl = this.getFirstActivity(procDefEnt);
            if (!ObjectUtils.isEmpty(actImpl)) {
                List<PvmActivity> nextUserActivities = this.getNextUserActivity(actImpl, vars); //??????????????????????????????
                candidate = this.getTaskDefCandidate(procDefEnt, actImpl, nextUserActivities);
            }
        }
        return candidate;
    }

    /**
     * ???????????????????????????
     *
     * @param taskId ??????ID
     * @return ?????????
     */
    @Override
    public WfCandidateVo getNextCandidate(String taskId) {
        WfCandidateVo candidate = new WfCandidateVo();
        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        if (!ObjectUtils.isEmpty(task)) {
            Execution execution = this.runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            Map<String, Object> vars = execution != null ? this.runtimeService.getVariables(execution.getId()) : null;
            ProcessDefinitionEntity procDefEnt = this.getProcessDefinitionEntity(task.getProcessDefinitionId());
            ActivityImpl actImpl = procDefEnt.findActivity(task.getTaskDefinitionKey()); //????????????????????????
            List<PvmActivity> nextActivitys = this.getNextUserActivity(actImpl, vars); //??????????????????????????????
            candidate = this.getTaskDefCandidate(procDefEnt, actImpl, nextActivitys);
        }
        return candidate;
    }

    /**
     * ????????????
     *
     * @param startForm form
     * @return ????????????VO
     */
    @Override
    public WfRunProcessVo startProcess(WfStartProcessForm startForm) {
        String procDefKey = startForm.getProcDefKey();
        String userId = startForm.getUserId();

        this.identityService.setAuthenticatedUserId(userId); //????????????????????????
        Map<String, Object> vars = startForm.getVars(); //???????????????????????????
        ProcessInstance processInstance = this.runtimeService.startProcessInstanceByKey(procDefKey, startForm.getTitle(), vars); //??????????????????

        this.runtimeService.setProcessInstanceName(processInstance.getId(), startForm.getTitle()); //??????????????????
        Task task = this.taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        this.completeTask(userId, task, startForm.getNextActPart(), startForm.getComment(), vars); //?????????????????????(???????????????????????????
        return this.getWfRuningProcessVo(task, userId);
    }

    /**
     * ??????????????????????????????
     *
     * @param outTransitions ??????
     * @return ????????????
     */
    private boolean isExistCondition(List<PvmTransition> outTransitions) {
        for (PvmTransition pt : outTransitions) {
            String condition = FormatUtil.toString(pt.getProperty("conditionText")).trim();  //??????????????????????????????????????????
            if (!ObjectUtils.isEmpty(condition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ??????????????????VO
     *
     * @param task ??????
     * @return ????????????VO
     */
    private WfRunProcessVo getWfRuningProcessVo(Task task, String userId) {
        WfRunProcessVo procVo = new WfRunProcessVo();
        procVo.setProcDefId(task.getProcessDefinitionId());
        procVo.setProcInstId(task.getProcessInstanceId());
        procVo.setActivityId(task.getTaskDefinitionKey());
        procVo.setActivityName(task.getName());
        procVo.setTaskId(task.getId());
        procVo.setUserId(userId);
        return procVo;
    }

    /**
     * ??????
     *
     * @param claimTaskForm ??????
     * @return ??????VO
     */
    @Override
    public WfClaimVo claimTask(WfClaimTaskForm claimTaskForm) {
        WfClaimVo vo = this.dozerMapper.map(claimTaskForm, WfClaimVo.class);
        vo.setProcInstId(claimTaskForm.getProcInstId());
        vo.setUserId(claimTaskForm.getUserId());
        Task task = this.taskService.createTaskQuery().taskId(claimTaskForm.getTaskId()).taskCandidateOrAssigned(claimTaskForm.getUserId()).singleResult();
        if (!ObjectUtils.isEmpty(task)) {
            ProcessDefinitionEntity procDefEnt = this.getProcessDefinitionEntity(task.getProcessDefinitionId()); //????????????????????????
            ActivityImpl firstActivity = this.getFirstActivity(procDefEnt); //?????????????????????
            vo.setActivityId(task.getTaskDefinitionKey());
            vo.setActivityName(task.getName());
            vo.setAgree(!task.isSuspended()); //??????
            vo.setReject(!task.isSuspended() && !task.getTaskDefinitionKey().equals(firstActivity.getId())); //????????????????????????????????????????????????
            vo.setTerminate(task.getTaskDefinitionKey().equals(firstActivity.getId())); //?????????????????????????????????
            vo.setStart(firstActivity.getId().equals(vo.getActivityId()));
            this.setEnd(vo, procDefEnt); //??????????????????
        } else {
            vo.setCancel(this.getCancel(vo.getProcInstId(), vo.getTaskId(), vo.getUserId()));
        }
        return vo;
    }

    /**
     * ?????????????????????
     * @param procInstId ????????????
     * @param taskId ??????ID
     * @param userId ??????ID
     * @return
     */
    private boolean getCancel(String procInstId, String taskId, String userId){
        boolean cancel = false;
        if(!ObjectUtils.isEmpty(procInstId) && !ObjectUtils.isEmpty(taskId) && !ObjectUtils.isEmpty(userId)){
            ProcessInstance procInst = this.runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
            if(!ObjectUtils.isEmpty(procInst)) { //???????????????
                ProcessDefinitionEntity procDefEnt = this.getProcessDefinitionEntity(procInst.getProcessDefinitionId());
                List<HistoricTaskInstance> tasks = this.historyService.createHistoricTaskInstanceQuery() //
                    .processInstanceId(procInstId).finished().orderByHistoricTaskInstanceEndTime().desc().list();
                Map<String, Object> vars = procInst.getProcessVariables();
                HistoricTaskInstance task = this.getCancelTask(tasks, taskId, userId);
                if(!ObjectUtils.isEmpty(task)){
                    ActivityImpl actImpl = procDefEnt.findActivity(task.getTaskDefinitionKey()); //????????????????????????
                    if(this.isActivityCancel(actImpl)) {
                        cancel = false;
                    } else {
                        List<PvmActivity> nextActs = this.getNextUserActivity(actImpl, vars);
                        if (!ObjectUtils.isEmpty(nextActs)) {
                            cancel = true;
                            for (PvmActivity act : nextActs) {
                                for (HistoricTaskInstance hiTask : tasks) {
                                    if (act.getId().equals(hiTask.getTaskDefinitionKey()) && !Tools.isEmpty(hiTask.getEndTime()) && hiTask.getEndTime().after(task.getEndTime())) {
                                        cancel = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return cancel;
    }

    /**
     * ??????????????????
     * @param tasks
     * @param taskId
     * @param userId
     * @return
     */
    private HistoricTaskInstance getCancelTask(List<HistoricTaskInstance> tasks, String taskId, String userId){
        HistoricTaskInstance hiTask = null;
        if(!ObjectUtils.isEmpty(tasks) && !ObjectUtils.isEmpty(taskId) && !ObjectUtils.isEmpty(userId)){
            for(HistoricTaskInstance task : tasks){
                if(taskId.equals(task.getId()) && !Tools.isEmpty(task.getEndTime()) && userId.equals(task.getAssignee())){
                    hiTask = task;
                    break;
                }
            }
        }
        return hiTask;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param activity ????????????
     * @return ???/???
     */
    private boolean isActivityCancel(PvmActivity activity) {
        boolean cancel = false;
        List<PvmTransition> pts = activity.getOutgoingTransitions();
        if (!Tools.isEmpty(pts)) {
            for (PvmTransition pt : pts) {
                String type = Tools.toString(pt.getDestination().getProperty("type"));
                if (type.equalsIgnoreCase("parallelGateway") || type.equalsIgnoreCase("inclusiveGateway")) {
                    if(pt.getDestination().getIncomingTransitions().size() > 1) {
                        cancel = true;
                    }
                }
            }
        }
        return cancel;
    }

    /**
     * ???????????????????????????
     *
     * @param vo         ??????
     * @param procDefEnt ????????????
     */
    public void setEnd(WfClaimVo vo, ProcessDefinitionEntity procDefEnt) {
        List<ActivityImpl> last = this.getLastActivity(procDefEnt);
        for (ActivityImpl ai : last) {
            if (vo.getActivityId().equals(ai.getId())) {
                vo.setEnd(true); //???????????????????????????
            }
        }
    }

    /**
     * ???????????????
     *
     * @param completeForm ???????????????
     * @return ????????????VO
     */
    @Override
    public WfRunProcessVo completeTask(WfCompleteTaskForm completeForm) {
        WfRunProcessVo procVo = null;
        String userId = completeForm.getUserId();
        String taskId = completeForm.getTaskId();
        Task task = this.taskService.createTaskQuery().taskId(taskId).taskCandidateOrAssigned(userId).singleResult();
        if (!ObjectUtils.isEmpty(task)) {
            this.completeTask(userId, task, completeForm.getNextActPart(), completeForm.getComment(), completeForm.getVars());
            procVo = this.getWfRuningProcessVo(task, userId);
            ProcessInstance pi = this.runtimeService.createProcessInstanceQuery().processInstanceId(completeForm.getProcInstId()).singleResult();
            procVo.setProcComplete(ObjectUtils.isEmpty(pi)); //?????????????????????
        } else {
            throw new BaseException("??????????????????????????????????????????????????????!");
        }
        return procVo;
    }

    /**
     * ????????????
     * ???????????????????????????????????????
     *
     * @param userId      ??????
     * @param task        ??????
     * @param nextActPart ???????????????
     * @param comment     ??????
     */
    private void completeTask(String userId, Task task, List<WfActivityVo> nextActPart, String comment, Map<String, Object> vars) {
        vars = ObjectUtils.isEmpty(vars) ? new HashMap<>() : vars; //????????????,????????????
        if (!ObjectUtils.isEmpty(task) && !task.isSuspended()) {
            if (!ObjectUtils.isEmpty(nextActPart)) {
                vars.put("nextActPart", JsonUtil.toJson(nextActPart)); //?????????????????????????????????????????????
            }
            this.taskService.claim(task.getId(), userId);
            //this.taskService.addComment(task.getId(), task.getProcessInstanceId(), FormatUtil.toString(comment)); //????????????
            this.taskService.complete(task.getId(), vars); //????????????
        }
    }

    /**
     * ????????????????????????
     *
     * @param taskId ??????ID
     * @return ??????????????????
     */
    @Override
    public List<WfActivityVo> getRejectActivity(String taskId) {
        List<ActivityImpl> rtnList = new ArrayList<>();
        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        if (!ObjectUtils.isEmpty(task)) {
            ProcessDefinitionEntity procDef = (ProcessDefinitionEntity) ((RepositoryServiceImpl) this.repositoryService).getDeployedProcessDefinition(task.getProcessDefinitionId());
            Execution execution = this.runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            ActivityImpl currActivity = procDef.findActivity(execution.getActivityId());// ????????????
            rtnList = this.getUpBackActivity(procDef, task.getProcessInstanceId(), taskId, currActivity, new ArrayList<>(), new ArrayList<>());
        }
        return reverList(rtnList);
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     *
     * @param procDefEnt   ????????????
     * @param proceInstId  ????????????ID
     * @param taskId       ????????????ID
     * @param currActivity ??????????????????
     * @param rtnList      ????????????????????????
     * @param tempList     ???????????????????????????????????????????????????????????????userTask?????????
     * @return ??????????????????
     */
    private List<ActivityImpl> getUpBackActivity(ProcessDefinitionEntity procDefEnt, String proceInstId, String taskId, ActivityImpl currActivity, List<ActivityImpl> rtnList, List<ActivityImpl> tempList) {
        List<PvmTransition> incomingTransitions = currActivity.getIncomingTransitions();  // ???????????????????????????
        List<ActivityImpl> exclusiveGateways = new ArrayList<>(); // ???????????????????????????userTask????????????????????????????????????????????????????????????????????????userTask??????
        List<ActivityImpl> parallelGateways = new ArrayList<>();  // ?????????????????????userTask????????????????????????????????????????????????????????????????????????userTask??????
        for (PvmTransition pvmTransition : incomingTransitions) { // ????????????????????????????????????
            TransitionImpl transitionImpl = (TransitionImpl) pvmTransition;
            ActivityImpl activityImpl = transitionImpl.getSource();
            String type = (String) activityImpl.getProperty("type");
            /**
             * ???????????????????????????<br>
             * ????????????????????????????????????????????????ID???:XXX_start(??????)???XXX_end(??????)
             */
            if ("parallelGateway".equals(type)) {// ????????????
                String gatewayId = activityImpl.getId();
                String gatewayType = gatewayId.substring(gatewayId.lastIndexOf("_") + 1);
                if ("START".equals(gatewayType.toUpperCase())) {// ???????????????????????????
                    return rtnList;
                } else {// ??????????????????????????????????????????????????????????????????????????????????????????userTask??????
                    parallelGateways.add(activityImpl);
                }
            } else if ("startEvent".equals(type)) {// ???????????????????????????
                return rtnList;
            } else if ("userTask".equals(type)) {// ????????????
                tempList.add(activityImpl);
            } else if ("exclusiveGateway".equals(type)) {// ??????????????????????????????????????????????????????????????????????????????????????????userTask??????
                currActivity = transitionImpl.getSource();
                exclusiveGateways.add(currActivity);
            }
        }
        for (ActivityImpl activityImpl : exclusiveGateways) { //??????????????????????????????????????????userTask??????
            getUpBackActivity(procDefEnt, proceInstId, taskId, activityImpl, rtnList, tempList);
        }
        for (ActivityImpl activityImpl : parallelGateways) { //????????????????????????????????????userTask??????
            getUpBackActivity(procDefEnt, proceInstId, taskId, activityImpl, rtnList, tempList);
        }
        currActivity = filterNewestActivity(proceInstId, tempList); //????????????userTask????????????????????????????????????
        if (currActivity != null) {
            String id = findParallelGatewayId(currActivity);  // ????????????????????????????????????????????????????????????????????????ID
            if (ObjectUtils.isEmpty(id)) {// ????????????ID?????????????????????????????????????????????????????????????????????????????????
                rtnList.add(currActivity);
            } else {// ??????????????????ID???????????????????????????????????????????????????userTask????????????
                currActivity = procDefEnt.findActivity(id);
            }
            tempList.clear(); // ??????????????????????????????
            getUpBackActivity(procDefEnt, proceInstId, taskId, currActivity, rtnList, tempList); // ??????????????????
        }
        return rtnList;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????ID
     *
     * @param activityImpl ????????????
     * @return String
     */
    private String findParallelGatewayId(ActivityImpl activityImpl) {
        List<PvmTransition> incomingTransitions = activityImpl.getOutgoingTransitions();
        for (PvmTransition pvmTransition : incomingTransitions) {
            TransitionImpl transitionImpl = (TransitionImpl) pvmTransition;
            activityImpl = transitionImpl.getDestination();
            String type = (String) activityImpl.getProperty("type");
            if ("parallelGateway".equals(type)) {// ????????????
                String gatewayId = activityImpl.getId();
                String gatewayType = gatewayId.substring(gatewayId.lastIndexOf("_") + 1);
                if ("END".equals(gatewayType.toUpperCase())) {
                    return gatewayId.substring(0, gatewayId.lastIndexOf("_")) + "_start";
                }
            }
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     *
     * @param proceInstId ????????????
     * @param tempList    ??????????????????
     * @return ????????????
     */
    private ActivityImpl filterNewestActivity(String proceInstId, List<ActivityImpl> tempList) {
        while (tempList.size() > 0) {
            ActivityImpl activity_1 = tempList.get(0);
            HistoricActivityInstance activityInstance_1 = findHistoricUserTask(proceInstId, activity_1.getId());
            if (activityInstance_1 == null) {
                tempList.remove(activity_1);
                continue;
            }
            if (tempList.size() > 1) {
                ActivityImpl activity_2 = tempList.get(1);
                HistoricActivityInstance activityInstance_2 = findHistoricUserTask(proceInstId, activity_2.getId());
                if (activityInstance_2 == null) {
                    tempList.remove(activity_2);
                    continue;
                }
                if (activityInstance_1.getEndTime().before(activityInstance_2.getEndTime())) {
                    tempList.remove(activity_1);
                } else {
                    tempList.remove(activity_2);
                }
            } else {
                break;
            }
        }
        if (tempList.size() > 0) {
            return tempList.get(0);
        }
        return null;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param proceInstId ????????????ID
     * @param activityId  ????????????ID
     * @return ??????????????????
     */
    private HistoricActivityInstance findHistoricUserTask(String proceInstId, String activityId) {
        HistoricActivityInstance rtnVal = null;
        // ???????????????????????????????????????????????????
        List<HistoricActivityInstance> historicActivityInstances = this.historyService.createHistoricActivityInstanceQuery()
            .activityType("userTask").processInstanceId(proceInstId).activityId(activityId).finished() //
            .orderByHistoricActivityInstanceEndTime().desc().list();
        if (historicActivityInstances.size() > 0) {
            rtnVal = historicActivityInstances.get(0);
        }
        return rtnVal;
    }

    /**
     * ????????????list??????????????????????????????????????????
     *
     * @param list ??????
     * @return ??????
     */
    private List<WfActivityVo> reverList(List<ActivityImpl> list) {
        List<ActivityImpl> rtnList = new ArrayList<>();
        List<WfActivityVo> rtnAiList = new ArrayList<>();
        // ?????????????????????????????????????????????
        for (int i = list.size(); i > 0; i--) {
            if (!rtnList.contains(list.get(i - 1)))
                rtnList.add(list.get(i - 1));
        }
        for (ActivityImpl ai : list) {
            rtnAiList.add(new WfActivityVo(ai.getId(), FormatUtil.toString(ai.getProperty("name"))));
        }
        return rtnAiList;
    }

    /**
     * ????????????
     *
     * @param form ??????
     */
    @Override
    public WfRunProcessVo cancelTask(WfRejectTaskForm form) {
        WfRunProcessVo procVo = null;
        List<Task> nextTasks = new ArrayList<>();
        boolean cancel = this.getCancel(form.getProcInstId(), form.getTaskId(), form.getUserId());
        if (cancel) {
            nextTasks = this.getCancelTask(form);
            if(!ObjectUtils.isEmpty(nextTasks)){
                for(Task task : nextTasks){
                    if(task.getId().equals(nextTasks.get(nextTasks.size() - 1).getId())){
                        this.rejectTask(task, form.getActivityId(), form.getUserId());
                        List<Task> tasks_ = this.taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).taskDefinitionKey(form.getActivityId()).list();
                        for(Task task_ : tasks_){
                            procVo = this.getWfRuningProcessVo(task_, form.getUserId());
                        }
                    }else {
                        this.cancelTask(task, form.getActivityId(), form.getUserId());
                    }
                }
            }
        }
        if(ObjectUtils.isEmpty(nextTasks)) {
            throw new BaseException("?????????????????????");
        }
        return procVo;
    }

    /**
     *  ?????????????????????
     * @param form
     * @return
     */
    public List<Task> getCancelTask(WfRejectTaskForm form) {
        List<Task> nextTasks = new ArrayList<>();
        HistoricTaskInstance hiTask = historyService.createHistoricTaskInstanceQuery().taskId(form.getTaskId()).singleResult();
        if(ObjectUtils.isEmpty(form.getActivityId())) {
            form.setActivityId(hiTask.getTaskDefinitionKey());
        }
        List<PvmActivity> nextActs = this.getNextUserActivity(hiTask); //????????????????????????
        List<Task> tasks = this.taskService.createTaskQuery().processInstanceId(form.getProcInstId()).list(); //????????????????????????
        for (PvmActivity act : nextActs) {
            for (int i = 0; i <  tasks.size(); i++) {
                if (act.getId().equals(tasks.get(i).getTaskDefinitionKey())) {
                    nextTasks.add(tasks.get(i));
                }
            }
        }
        return nextTasks;
    }

    /**
     * ????????????
     *
     * @param form ??????
     */
    @Override
    public WfRunProcessVo rejectTask(WfRejectTaskForm form) {
        WfRunProcessVo procVo = null;
        Task task = this.taskService.createTaskQuery().taskId(form.getTaskId()).taskCandidateOrAssigned(form.getUserId()).singleResult();
        if (!ObjectUtils.isEmpty(task)) {
            String assignee = this.rejectTask(task, form.getActivityId(), form.getUserId());
            procVo = this.getWfRuningProcessVo(task, assignee);
        }
        return procVo;
    }

    /**
     * ????????????
     * @param task
     * @param activityId
     * @param userId
     * @return
     */
    private String rejectTask(Task task, String activityId, String userId) {
        if (!ObjectUtils.isEmpty(task)) {
            HistoricActivityInstance hai = this.findHistoricUserTask(task.getProcessInstanceId(), activityId);
            ProcessDefinitionEntity procDef = this.getProcessDefinitionEntity(task.getProcessDefinitionId());
            ActivityImpl currActivity = procDef.findActivity(task.getTaskDefinitionKey());// ????????????
            List<PvmTransition> oriPvmTransitionList = this.clearTransition(currActivity); // ??????????????????
            TransitionImpl newTransition = currActivity.createOutgoingTransition(); // ???????????????
            ActivityImpl pointActivity = procDef.findActivity(activityId); // ????????????
            newTransition.setDestination(pointActivity); // ??????????????????????????????

            List<WfActivityVo> nextActPart = this.getRejectActPart(hai);
            this.completeTask(userId, task, nextActPart, null, null); //????????????

            currActivity.getOutgoingTransitions().remove(newTransition); // ???????????????????????????
            pointActivity.getIncomingTransitions().remove(newTransition); // ???????????????????????????
            this.restoreTransition(currActivity, oriPvmTransitionList); // ??????????????????
            return hai.getAssignee();
        }
        return null;
    }

    /**
     * ??????????????????????????????
     * @param hai
     * @return
     */
    private List<WfActivityVo> getRejectActPart(HistoricActivityInstance hai) {
        List<WfActivityVo> nextActPart = new ArrayList<>();
        if (!ObjectUtils.isEmpty(hai)) {
            WfActivityVo activityVo = new WfActivityVo();
            activityVo.setId(hai.getActivityId());
            activityVo.setName(hai.getActivityName());
            List<WfCandidateUserVo> candidateUsers = new ArrayList<>();
            candidateUsers.add(new WfCandidateUserVo(hai.getAssignee()));
            activityVo.setCandidateUsers(candidateUsers);
            nextActPart.add(activityVo);
        }
        return nextActPart;
    }

    /**
     * ????????????
     * @param task
     * @param activityId
     * @param userId
     * @return
     */
    private void cancelTask(Task task, String activityId, String userId) {
        if (!ObjectUtils.isEmpty(task)) {
            ProcessDefinitionEntity procDef = this.getProcessDefinitionEntity(task.getProcessDefinitionId());
            ActivityImpl currActivity = procDef.findActivity(activityId);// ????????????
            List<PvmTransition> oriPvmTransitionList = this.clearTransition(currActivity); // ??????????????????
            this.completeTask(userId, task, null, null, null); //????????????
            this.restoreTransition(currActivity, oriPvmTransitionList); // ??????????????????
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param activityImpl ????????????
     * @return ??????????????????
     */
    private List<PvmTransition> clearTransition(ActivityImpl activityImpl) {
        List<PvmTransition> oriPvmTransitionList = new ArrayList<>();  // ??????????????????????????????????????????
        if (!ObjectUtils.isEmpty(activityImpl)) {
            List<PvmTransition> pvmTransitionList = activityImpl.getOutgoingTransitions();  // ?????????????????????????????????????????????????????????????????????
            oriPvmTransitionList.addAll(pvmTransitionList);
            pvmTransitionList.clear();
        }
        return oriPvmTransitionList;
    }

    /**
     * ??????????????????????????????
     *
     * @param activityImpl         ????????????
     * @param oriPvmTransitionList ????????????????????????
     */
    private void restoreTransition(ActivityImpl activityImpl, List<PvmTransition> oriPvmTransitionList) {
        List<PvmTransition> pvmTransitionList = activityImpl.getOutgoingTransitions(); // ??????????????????
        pvmTransitionList.clear();
        pvmTransitionList.addAll(oriPvmTransitionList); // ??????????????????
    }

    @Override
    public void terminateProcess(WfTerminateTaskForm terminateTaskForm) {
        this.runtimeService.deleteProcessInstance(terminateTaskForm.getProcInstId(), FormatUtil.toString(terminateTaskForm.getComment()));
    }

    @Override
    public void deleteProcess(String procInstId) {
        ProcessInstance pi = this.runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        if (!ObjectUtils.isEmpty(pi)) { //??????????????????????????????????????????????????????
            this.runtimeService.deleteProcessInstance(procInstId, "????????????");
        }
        this.historyService.deleteHistoricProcessInstance(procInstId);
    }

    @Override
    public void activeProcess(String procInstId) {
        this.runtimeService.activateProcessInstanceById(procInstId);
    }

    @Override
    public void suspendProcess(String procInstId) {
        this.runtimeService.suspendProcessInstanceById(procInstId);
    }

    @Override
    public List<WfTaskVo> getTaskByActDefId(String procInstId, String actDefId) {
        List<WfTaskVo> wfTasks = new ArrayList<>();
        WfTaskVo wfTask;
        List<Task> tasks = this.taskService.createTaskQuery().processInstanceId(procInstId).taskDefinitionKey(actDefId).orderByTaskCreateTime().desc().list();
        if (!ObjectUtils.isEmpty(tasks)) {
            for (Task task : tasks) {
                wfTask = new WfTaskVo(task.getId(), task.getName());
                wfTask.setProcDefId(task.getProcessDefinitionId());
                wfTask.setProcInstId(task.getProcessInstanceId());
                wfTask.setAssignee(task.getAssignee());
                wfTask.setOwner(task.getOwner());
                wfTask.setDelegation(task.getDelegationState().name());
                wfTask.setCreateTime(task.getCreateTime());
            }
        }
        return wfTasks;
    }

    /**
     * ????????????????????????
     *
     * @param userId
     * @return
     */
    @Override
    public Long queryWfUnfinishCount(String userId) {
        return this.taskService.createTaskQuery().taskCandidateOrAssigned(userId).active().count();
    }

    /**
     * ??????????????????
     *
     * @param userId ??????
     * @param form   Form
     * @return ??????
     */
    @Override
    public PageInfo<MyUnFinishTaskVo> queryMyUnfinishTaskList(String userId, int pageSize, int pageNum, WfUnFinishSearchForm form) {
        PageInfo page = new PageInfo();
        List<MyUnFinishTaskVo> taskVos = new ArrayList<>();
        TaskQuery taskQuery = this.taskService.createTaskQuery().taskCandidateOrAssigned(userId).active();
        if (!Tools.isEmpty(form.getStartTime())) {
            taskQuery.taskCreatedAfter(form.getStartTime());
        }
        if (!Tools.isEmpty(form.getEndTime())) {
            taskQuery.taskCreatedBefore(Tools.toDate(Tools.toDateString(form.getEndTime()) + " 23:59:59"));
        }
        if (!Tools.isEmpty(form.getBizType())) {
            taskQuery.processCategoryIn(Lists.newArrayList(form.getBizType()));
        }
        if (!Tools.isEmpty(form.getKeyword())) {
            taskQuery.processInstanceBusinessKeyLikeIgnoreCase("%" + form.getKeyword() + "%");
        }
        if (pageNum == 1) {
            page.setTotal(taskQuery.count());
        }
        List<Task> tasks = taskQuery.orderByTaskCreateTime().desc().listPage((pageNum - 1) * pageSize, pageSize);
        if (!ObjectUtils.isEmpty(tasks)) {
            for (Task task : tasks) {
                taskVos.add(this.getMyUnFinishTask(task));
            }
        }
        page.setList(taskVos);
        return page;
    }

    /**
     * ???????????????????????????
     *
     * @param task ??????
     * @return ??????
     */
    private MyUnFinishTaskVo getMyUnFinishTask(Task task) {
        MyUnFinishTaskVo taskVo = new MyUnFinishTaskVo();
        taskVo.setId(task.getId());
        taskVo.setProcInstId(task.getProcessInstanceId()); //????????????id
        taskVo.setTaskName(task.getName());
        taskVo.setCreateTime(task.getCreateTime());
        if (!Tools.isEmpty(task.getDescription())) {
            Map<String, Object> desc = JsonUtil.readValue(task.getDescription(), Map.class);
            if (!Tools.isEmpty(desc) && !Tools.isEmpty(desc.get("sender"))) {
                taskVo.setSender(JsonUtil.readValue(JsonUtil.toJson(desc.get("sender")), UserVo.class));
            }
        }
        return taskVo;
    }

    /**
     * ??????????????????
     *
     * @param userId ??????
     * @return ??????
     */
    @Override
    public PageInfo<MyFinishTaskVo> queryMyFinishTaskList(String userId, int pageSize, int pageNum, WfFinishSearchForm form) {
        PageInfo page = new PageInfo();
        List<MyFinishTaskVo> taskVos = new ArrayList<>();
        HistoricTaskInstanceQuery taskQuery = this.historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).finished();
        if (!Tools.isEmpty(form.getStartTime())) {
            taskQuery.taskCreatedAfter(form.getStartTime());
        }
        if (!Tools.isEmpty(form.getEndTime())) {
            taskQuery.taskCreatedBefore(Tools.toDate(Tools.toDateString(form.getEndTime()) + " 23:59:59"));
        }
        if (!Tools.isEmpty(form.getBizType())) {
            taskQuery.processCategoryIn(Lists.newArrayList(form.getBizType()));
        }
        if (!Tools.isEmpty(form.getKeyword())) {
            taskQuery.processInstanceBusinessKeyLikeIgnoreCase("%" + form.getKeyword() + "%");
        }
        if (pageNum == 1) {
            page.setTotal(taskQuery.count());
        }
        List<HistoricTaskInstance> tasks = taskQuery.orderByTaskCreateTime().desc().listPage((pageNum - 1) * pageSize, pageSize);
        if (!ObjectUtils.isEmpty(tasks)) {
            Map<String, ProcessInstance> procInstMap = this.getProcessInstance(tasks);
            for (HistoricTaskInstance task : tasks) {
                taskVos.add(this.getMyFinishTask(task, procInstMap));
            }
        }
        page.setList(taskVos);
        return page;
    }

    /**
     * ????????????
     *
     * @param tasks
     * @return
     */
    private Map<String, ProcessInstance> getProcessInstance(List<HistoricTaskInstance> tasks) {
        Map<String, ProcessInstance> procInstMap = new HashMap<>();
        if (!Tools.isEmpty(tasks)) {
            Set<String> procInstIds = new HashSet<>();
            for (HistoricTaskInstance task : tasks) {
                procInstIds.add(task.getProcessInstanceId());
            }
            List<ProcessInstance> procInsts = this.runtimeService.createProcessInstanceQuery().processInstanceIds(procInstIds).list();
            for (ProcessInstance procInst : procInsts) {
                procInstMap.put(procInst.getProcessInstanceId(), procInst);
            }
        }
        return procInstMap;
    }

    /**
     * ????????????
     *
     * @param task
     * @return
     */
    private MyFinishTaskVo getMyFinishTask(HistoricTaskInstance task, Map<String, ProcessInstance> procInstMap) {
        MyFinishTaskVo taskVo = new MyFinishTaskVo();
        taskVo.setId(task.getId());
        taskVo.setProcInstId(task.getProcessInstanceId());
        taskVo.setTaskName(task.getName());
        taskVo.setCreateTime(task.getCreateTime());
        taskVo.setEndTime(task.getEndTime());
        if (!Tools.isEmpty(task.getDescription())) {
            Map<String, Object> desc = JsonUtil.readValue(task.getDescription(), Map.class);
            if (!Tools.isEmpty(desc) && !Tools.isEmpty(desc.get("sender"))) {
                taskVo.setSender(JsonUtil.readValue(JsonUtil.toJson(desc.get("sender")), UserVo.class));
            }
        }
        taskVo.setStatus(procInstMap.containsKey(task.getProcessInstanceId()) ? "?????????" : "?????????");
        return taskVo;
    }

    /**
     * ????????????????????????
     *
     * @param userId   ??????
     * @param pageSize ????????????
     * @param pageNum  ?????????
     * @return ???????????????
     */
    @Override
    public PageInfo<WfMineTaskVo> queryWfMineTaskList(String userId, int pageSize, int pageNum, WfMineSearchForm search) {
        PageHelper.startPage(pageNum, pageSize, pageNum == 1);
        search.setKeyword(Tools.isEmpty(search.getKeyword()) ? null : ("%" + search.getKeyword() + "%"));
        List<WfMineTaskVo> taskVos = this.mapper.selectWfMineTaskList(userId, search);
        return new PageInfo(taskVos);
    }

    @Override
    public List<WfProcLogVo> queryProcessHandleLogList(List<String> procInstIds) throws ParseException {
        List<WfProcLogVo> wfProcLogVos = new ArrayList<>();
        //????????????????????????????????????
        List<WfProcLogVo> wfProcLogVos1 = this.mapper.selectWorkFlowInfoByProcInstIds(procInstIds);
        //?????????????????????????????????
        List<WfProcLogVo> wfProcLogVos2 = this.mapper.selectWorkFlowInfo2ByProcInstIds(procInstIds);
        Map<String, List<WfProcLogVo>> wfProcLogVosMap = ListUtil.bulidTreeListMap(wfProcLogVos2, "procInstId", String.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (!ObjectUtils.isEmpty(wfProcLogVos1)) {
            for (WfProcLogVo procLogVo : wfProcLogVos1) {
                String procInstId = procLogVo.getProcInstId();
                List<WfProcLogVo> childWfProcLogVos = wfProcLogVosMap.get(procInstId);
                if (!ObjectUtils.isEmpty(childWfProcLogVos)) {
                    WfProcLogVo wfProcLogChildVos2 = childWfProcLogVos.get(0);
                    procLogVo.setActivity(wfProcLogChildVos2.getActivity());
                    Date creatTime = sdf.parse(wfProcLogChildVos2.getCreatTime());
                    procLogVo.setStayTime(DateUtil.getDatePoor(creatTime, new Date()));
                    if (!Tools.isEmpty(wfProcLogChildVos2.getNewUser())) {
                        procLogVo.setNewUser(wfProcLogChildVos2.getNewUser());
                    } else if (!Tools.isEmpty(wfProcLogChildVos2.getId())) {
                        List<IdentityLink> ils = this.processEngine.getTaskService().getIdentityLinksForTask(FormatUtil.toString(wfProcLogChildVos2.getId()));
                        if (!Tools.isEmpty(ils)) {
                            for (IdentityLink il : ils) {
                                if (!Tools.isEmpty(il.getUserId())) {
                                    procLogVo.setNewUser(il.getUserId());
                                }
                            }
                        }
                    }
                }
                if (!ObjectUtils.isEmpty(procLogVo.getEndTime())) {
                    procLogVo.setStatus("??????");
                } else {
                    procLogVo.setStatus("?????????");
                }
                wfProcLogVos.add(procLogVo);
            }
        }
        return wfProcLogVos;
    }

    /**
     * ??????????????????
     *
     * @param procInstId ????????????ID
     * @return ??????
     */
    @Override
    public WfLogDetailVo queryProcessHandleLogDetail(String procInstId) {
        List<Comment> comments = this.taskService.getProcessInstanceComments(procInstId);
        Map<String, Comment> taskId_comment = new HashMap<>();
        if (comments != null) {
            for (Comment comment : comments) {
                taskId_comment.put(comment.getTaskId(), comment);
            }
        }
        HistoricProcessInstance historicProcessInstance = this.historyService.createHistoricProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        List<HistoricTaskInstance> historicTaskInstances = this.historyService.createHistoricTaskInstanceQuery().processInstanceId(procInstId).orderByHistoricTaskInstanceEndTime().desc().list();
        List<WorkFlowLogVo> logList = this.mapper.selectWorkFlowLogByProcInsId(procInstId);
        Map<String, String> logMap = new HashMap<>();
        Map<String, String> dateMap = new HashMap<>();
        Map<String, String> nextUserMap = new HashMap<>();
        Map<String, Date> actEndMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(logList)) {
            for (WorkFlowLogVo log : logList) {
                logMap.put("WORKITEMID", log.getOperate());
                dateMap.put("WORKITEMID", log.getWsdCreator());
                nextUserMap.put("WORKITEMID", log.getNextUserId());
                actEndMap.put("WORKITEMID", log.getActEndTime());

            }
        }
        //??????id??????
        List<String> userIdList = ListUtil.toValueList(logList, "nextUserId", String.class);
        List<String> userIdsList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(userIdList)) {
            for (String userId : userIdList) {
                if (userId.contains(",")) {
                    List<String> userIds = ListUtil.spliceArrayListByStr(userId, ",");
                    userIdsList.addAll(userIds);
                } else {
                    userIdsList.add(userId);
                }
            }
        }
        ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(historicProcessInstance.getProcessDefinitionId());
        BpmnModel bpmnModel = this.getBpmnModel(processDefinitionEntity);
        List<WfProcLogDetailVo> wfProcLogDetailVos = new ArrayList<>();
        if (!ObjectUtils.isEmpty(historicTaskInstances)) {
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                WfProcLogDetailVo wfProcLogDetailVo = new WfProcLogDetailVo();
                wfProcLogDetailVo.setOpinion(ObjectUtils.isEmpty(taskId_comment.get(historicTaskInstance.getId())) ? "" : taskId_comment.get(historicTaskInstance.getId()).getFullMessage());
                wfProcLogDetailVo.setOperateUser(!ObjectUtils.isEmpty(dateMap) ? dateMap.get(historicTaskInstance.getId()) : "");
                wfProcLogDetailVo.setCreateTime(ObjectUtils.isEmpty(taskId_comment.get(historicTaskInstance.getId())) ? null : taskId_comment.get(historicTaskInstance.getId()).getTime());
                String operate = logMap.get(historicTaskInstance.getId());
                wfProcLogDetailVo.setOperateType(ObjectUtils.isEmpty(operate) ? "" : (operate.equals("EXECUTE") ? "??????" : operate.equals("BACKPLAN") ? "????????????" : (operate.equals("BACK") ? "??????" : (operate.equals("TERMINATE") ? "??????" : (operate.equals("HELP") ? "??????" : (operate.equals("DELEG") ? "??????" : (operate.equals("REDO") ? "??????" : (operate.equals("CALLBACK") ? "??????" : (operate.equals("REJECT") ? "??????" : (operate.equals("CONFIRM") ? "??????" : ""))))))))));
                wfProcLogDetailVo.setWorkItemName(ObjectUtils.isEmpty(bpmnModel.getMainProcess().getFlowElement(historicTaskInstance.getTaskDefinitionKey()).getName()) ? "" : bpmnModel.getMainProcess().getFlowElement(historicTaskInstance.getTaskDefinitionKey()).getName());
                wfProcLogDetailVo.setNextUserName(nextUserMap.get(historicTaskInstance.getId()));
                wfProcLogDetailVo.setStayTime(DateUtil.getDatePoor(FormatUtil.toDate(historicTaskInstance.getStartTime() == null ? "" : FormatUtil.stringFromTime(historicTaskInstance.getStartTime())), (FormatUtil.toDate(historicTaskInstance.getStartTime() == null ? "" : FormatUtil.stringFromTime(historicTaskInstance.getEndTime())))));
                wfProcLogDetailVos.add(wfProcLogDetailVo);
            }
        }
        WfLogDetailVo wfLogDetailVo = new WfLogDetailVo();
        if (!ObjectUtils.isEmpty(historicProcessInstance)) {
            wfLogDetailVo.setId(0);
            wfLogDetailVo.setProcessInstName(historicProcessInstance.getName());
            wfLogDetailVo.setWfLog(wfProcLogDetailVos);
        }
        return wfLogDetailVo;

    }

    /**
     * ??????????????????
     *
     * @param processDefinitionEntity Entity
     * @return BpmnModel
     */
    private BpmnModel getBpmnModel(ProcessDefinitionEntity processDefinitionEntity) {
        BpmnModel bpmnModel = null;
        try {
            InputStream is = this.repositoryService.getResourceAsStream(processDefinitionEntity.getDeploymentId(), processDefinitionEntity.getResourceName());
            BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
            bpmnModel = bpmnXMLConverter.convertToBpmnModel(new InputStreamSource(is), false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bpmnModel;
    }

    /**
     * queryTaskIdByCurrentUserIdAndProcInstrId
     *
     * @param procInstId procInstId
     * @param userId     userId
     * @return String
     */
    @Override
    public String queryTaskIdByUserIdAndProcInstrId(String procInstId, String userId) {
        //????????????
        List<Task> tasks = this.taskService.createTaskQuery().processInstanceId(procInstId).taskCandidateOrAssigned(userId).list();
        if (!Tools.isEmpty(tasks)) {
            return tasks.get(tasks.size() - 1).getId();
        } else { //????????????
            List<HistoricTaskInstance> hiTasks = this.historyService.createHistoricTaskInstanceQuery().processInstanceId(procInstId).taskAssignee(userId).finished().list();
            if (!Tools.isEmpty(hiTasks)) {
                return hiTasks.get(hiTasks.size() - 1).getId();
            }
        }
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param modelIds ID
     * @return vo
     */
    @Override
    public List<ActModelVo> queryWfAssignList(List<String> modelIds) {
        List<ActModelVo> actModelVos = new ArrayList<>();
        if (!ObjectUtils.isEmpty(modelIds)) {
            actModelVos = this.mapper.selectActAssignList(modelIds);
            if (!ObjectUtils.isEmpty(actModelVos)) {
                for (ActModelVo actModelVo : actModelVos) {
                    actModelVo.setStatus(!ObjectUtils.isEmpty(actModelVo.getStatus()) ? "?????????" : "?????????");
                }
            }
        }
        return actModelVos;
    }

    /**
     * ??????????????????????????????
     *
     * @param modelIds ID
     * @return vo
     */
    @Override
    public List<WfProcessDefVo> queryProcDefByModulIdList(List<String> modelIds) {
        return this.mapper.selectProcDefByModulIdList(modelIds);
    }

    /**
     * ????????????
     *
     * @param form
     * @return modelId
     */
    @Override
    public String addModel(ActModelAddForm form) {
        int revision = 1; //??????
        RepositoryService repositoryService = this.processEngine.getRepositoryService();
        Model model = repositoryService.newModel(); //????????????????????????
        model.setName(form.getModelTitle()); //??????
        model.setCategory(form.getCategory());
        //model.setTenantId(tenantId);
        repositoryService.saveModel(model);
        String modelId = model.getId();
        //????????????
        model = repositoryService.getModel(modelId); //??????????????????????????????????????????
        String key = getNewModelKey(modelId);
        model.setKey(key);
        ObjectNode metaInfoJson = this.objectMapper.createObjectNode();
        metaInfoJson.put(ModelDataJsonConstants.MODEL_NAME, key);
        metaInfoJson.put(ModelDataJsonConstants.MODEL_DESCRIPTION, form.getModelDesc()); //??????
        metaInfoJson.put(ModelDataJsonConstants.MODEL_REVISION, revision);
        model.setMetaInfo(metaInfoJson.toString());
        repositoryService.saveModel(model);
        //??????ModelEditorSource
        ObjectNode editorNode = this.objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");

        ObjectNode properties = this.objectMapper.createObjectNode();
        properties.put("process_id", key);
        properties.put("name", form.getModelTitle());
        editorNode.putPOJO("properties", properties);

        ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.putPOJO("stencilset", stencilSetNode);

        repositoryService.addModelEditorSource(modelId, editorNode.toString().getBytes(StandardCharsets.UTF_8));
        return modelId;
    }

    /**
     * ????????????????????????KEY
     *
     * @param id Id
     * @return key
     */
    private String getNewModelKey(String id) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //yyyyMMdd_HHmmss_S
        return "model_" + sdf.format(new Date()) + "_" + id;
    }

    /**
     * ????????????
     *
     * @param modelId ID
     * @return success
     */
    @Override
    public String deployProcess(String modelId) throws IOException {
        RepositoryService repositoryService = this.processEngine.getRepositoryService(); //????????????
        Model modelData = repositoryService.getModel(modelId);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            return ("???????????????????????????????????????????????????????????????????????????");
        }
        JsonNode modelNode = new ObjectMapper().readTree(bytes);
        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return ("???????????????????????????????????????????????????????????????");
        }
        String checkModel = this.validationProcess(model); //????????????
        if (!ObjectUtils.isEmpty(checkModel)) {
            return checkModel;
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
        String processName = modelData.getName() + ".bpmn20.xml"; //??????????????????
        Deployment deployment = repositoryService.createDeployment().name(modelData.getName()) //
            .category(modelData.getCategory()).tenantId(modelData.getTenantId()) //
            .addString(processName, new String(bpmnBytes, StandardCharsets.UTF_8)).deploy(); //
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
        ProcessDefinition procDef = this.processEngine.getRepositoryService().createProcessDefinitionQuery()//
            .deploymentId(deployment.getId()).singleResult();
        if (!Tools.isEmpty(procDef)) {
            this.processEngine.getRepositoryService().setProcessDefinitionCategory(procDef.getId(), modelData.getCategory());
        }
        return "success";
    }

    /**
     * ????????????
     *
     * @param bpmnModel ??????
     */
    private String validationProcess(BpmnModel bpmnModel) {
        String retMsg = "";
        if (!Tools.isEmpty(bpmnModel) && !Tools.isEmpty(bpmnModel.getProcesses())) { //?????????????????????????????????
            Collection<FlowElement> flowElementList = bpmnModel.getProcesses().get(0).getFlowElements(); //??????????????????
            for (FlowElement flow : flowElementList) {
                if (flow instanceof UserTask || flow instanceof ExclusiveGateway) { //????????????????????????????????????????????????
                    FlowNode flowNode = (FlowNode) flow;
                    List<SequenceFlow> sequenceFlows = flowNode.getOutgoingFlows();
                    if (!ObjectUtils.isEmpty(sequenceFlows) && sequenceFlows.size() > 1) {
                        for (SequenceFlow sequenceFlow : sequenceFlows) {
                            if (ObjectUtils.isEmpty(sequenceFlow.getConditionExpression())) {
                                retMsg = "???" + flow.getName() + "?????????????????????????????????" + sequenceFlow.getName() + "?????????????????????";
                                return retMsg;
                            }
                        }
                    }
                }
                if (flow instanceof UserTask) { //???????????????????????????
                    FlowNode flowNode = (FlowNode) flow;
                    if (ObjectUtils.isEmpty(flowNode.getName())) {
                        retMsg = "????????????????????????????????????";
                        return retMsg;
                    }
                }
            }
        }
        return retMsg;
    }

    /**
     * ????????????
     *
     * @param modelId ??????id
     * @return id
     */
    @Override
    public String copyModel(String modelId) {
        return this.copyModel(modelId, null);
    }

    /**
     * ????????????
     *
     * @param modelId  ??????id
     * @param tenantId ??????ID
     * @return id
     */
    @Override
    public String copyModel(String modelId, String tenantId) {
        try {
            RepositoryService repositoryService = this.processEngine.getRepositoryService();
            //????????????????????????  ????????????????????????
            Model modelCopy = repositoryService.getModel(modelId);
            if (modelCopy != null) {
                Model model = repositoryService.newModel();
                model.setName(modelCopy.getName());
                model.setCategory(modelCopy.getCategory());
                model.setTenantId(tenantId);
                model.setVersion(1);
                //model.setCategory(category);
                repositoryService.saveModel(model);
                //????????????
                String id = model.getId();
                model = repositoryService.getModel(id); //??????????????????????????????????????????
                String key = getNewModelKey(id);
                ObjectNode metaInfoJson = (ObjectNode) objectMapper.readTree(modelCopy.getMetaInfo());
                metaInfoJson.put(ModelDataJsonConstants.MODEL_NAME, key);
                model.setKey(key);
                model.setMetaInfo(metaInfoJson.toString());
                repositoryService.saveModel(model);
                //??????ModelEditorSource
                byte[] source = repositoryService.getModelEditorSource(modelId);
                ObjectNode modelJson = (ObjectNode) objectMapper.readTree(source);
                ObjectNode properties = (ObjectNode) modelJson.get("properties");
                properties.put("process_id", key);
                repositoryService.addModelEditorSource(id, modelJson.toString().getBytes(StandardCharsets.UTF_8));
                repositoryService.addModelEditorSourceExtra(id, repositoryService.getModelEditorSourceExtra(modelId));
                return id;
            }
        } catch (Exception e) {
            throw new ActivitiException("Error saving model", e);
        }
        return null;
    }

    /**
     * ??????
     *
     * @param modelId ID
     * @return success
     */
    @Override
    public String deleteActivityNewModel(String modelId) {
        RepositoryService service = this.processEngine.getRepositoryService();
        service.deleteModel(modelId);
        return "success";
    }

    /**
     * ??????????????????Id??????????????????
     *
     * @param processInstanceId ??????ID
     * @return ??????????????????
     */
    @Override
    public WfProcessInstVo getProcessInstanceById(String processInstanceId) {
        WfProcessInstVo instVo = null;
        HistoricProcessInstanceEntity hpi = (HistoricProcessInstanceEntity) this.historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (!Tools.isEmpty(hpi)) {
            instVo = this.getProcessInstanceByHistoricProcessInstance(hpi);
        }
        return instVo;
    }

    /**
     * ??????????????????
     *
     * @param hpi ????????????
     * @return ????????????
     */
    public WfProcessInstVo getProcessInstanceByHistoricProcessInstance(HistoricProcessInstance hpi) {
        WfProcessInstVo instVo = new WfProcessInstVo();
        instVo.setProcDefKey(hpi.getProcessDefinitionKey());
        instVo.setProcDefId(hpi.getProcessDefinitionId());
        instVo.setProcDefName(hpi.getProcessDefinitionName());
        instVo.setProcInstId(hpi.getId());
        instVo.setProcInstName(hpi.getName());
        instVo.setStartUserId(hpi.getStartUserId());
        instVo.setStartTime(hpi.getStartTime());
        instVo.setEndTime(hpi.getEndTime());
        return instVo;
    }

    /**
     * ??????????????????Id??????????????????
     *
     * @param processInstanceId ??????ID
     * @return ??????????????????
     */
    @Override
    public List<WfActivityInstanceVo> getActivityInstanceByProcessInstanceId(String processInstanceId) {
        List<WfActivityInstanceVo> instVos = new ArrayList<>();
        List<HistoricActivityInstance> hais = this.historyService.createHistoricActivityInstanceQuery() //
            .processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
        if (!Tools.isEmpty(hais)) {
            for (HistoricActivityInstance hai : hais) {
                instVos.add(this.getActivityInstanceByHistoricActivityInstance(hai));
            }
        }
        return instVos;
    }

    /**
     * ????????????
     *
     * @param hai
     * @return
     */
    public WfActivityInstanceVo getActivityInstanceByHistoricActivityInstance(HistoricActivityInstance hai) {
        WfActivityInstanceVo instVo = new WfActivityInstanceVo();
        instVo.setId(hai.getId());
        instVo.setActivityId(hai.getActivityId());
        instVo.setActivityName(hai.getActivityName());
        instVo.setActivityType(hai.getActivityType());
        instVo.setProcessDefinitionId(hai.getProcessDefinitionId());
        instVo.setProcessInstanceId(hai.getProcessInstanceId());
        instVo.setAssignee(hai.getAssignee());
        instVo.setStartTime(hai.getStartTime());
        instVo.setEndTime(hai.getEndTime());
        instVo.setGetDurationInMillis(hai.getDurationInMillis());
        return instVo;
    }

    /**
     * ??????????????????Id??????????????????
     *
     * @param activityInstanceId ??????ID
     * @return ??????????????????
     */
    @Override
    public WfActivityInstanceVo getActivityInstanceByActivityInstanceId(String activityInstanceId) {
        WfActivityInstanceVo instVo = null;
        HistoricActivityInstance hai = this.historyService.createHistoricActivityInstanceQuery() //
            .activityInstanceId(activityInstanceId).singleResult();
        if (!Tools.isEmpty(hai)) {
            instVo = this.getActivityInstanceByHistoricActivityInstance(hai);
        }
        return instVo;
    }

    /**
     * ???????????????id???????????????
     *
     * @param taskId ??????ID
     * @return WfTaskVo
     */
    @Override
    public WfTaskVo getTaskByTaskId(String taskId) {
        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        if (!Tools.isEmpty(task)) {
            return this.getTaskByTask(task);
        } else {
            return this.getHistTaskById(taskId);
        }
    }

    /**
     * ????????????
     *
     * @param task ??????ID
     * @return ??????
     */
    private WfTaskVo getTaskByTask(Task task) {
        WfTaskVo taskVo = new WfTaskVo();
        taskVo.setId(task.getId());
        taskVo.setActivityId(task.getTaskDefinitionKey());
        taskVo.setName(task.getName());
        taskVo.setProcDefId(task.getProcessDefinitionId());
        taskVo.setProcInstId(task.getProcessInstanceId());
        taskVo.setDelegation(task.getDelegationState() != null ? task.getDelegationState().name() : "");
        taskVo.setAssignee(task.getAssignee());
        taskVo.setOwner(task.getOwner());
        taskVo.setCreateTime(task.getCreateTime());
        return taskVo;
    }

    /**
     * ???????????????????????????
     *
     * @param taskId ??????ID
     * @return ??????
     */
    private WfTaskVo getHistTaskById(String taskId) {
        WfTaskVo taskVo = null;
        HistoricTaskInstance task = this.historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
        if (!Tools.isEmpty(task)) {
            taskVo = this.getHistTaskByTask(task);
        }
        return taskVo;
    }

    /**
     * ????????????
     *
     * @param task ??????
     * @return ??????
     */
    private WfTaskVo getHistTaskByTask(HistoricTaskInstance task) {
        WfTaskVo taskVo = new WfTaskVo();
        taskVo.setId(task.getId());
        taskVo.setActivityId(task.getTaskDefinitionKey());
        taskVo.setName(task.getName());
        taskVo.setProcDefId(task.getProcessDefinitionId());
        taskVo.setProcInstId(task.getProcessInstanceId());
        taskVo.setAssignee(task.getAssignee());
        taskVo.setOwner(task.getOwner());
        taskVo.setCreateTime(task.getCreateTime());
        taskVo.setEndTime(task.getEndTime());
        return taskVo;
    }

    /**
     * ??????????????????????????????
     *
     * @param processInstanceId ????????????ID
     * @return ??????
     */
    @Override
    public List<WfTaskVo> getRunTaskByProcessInstanceId(String processInstanceId) {
        List<WfTaskVo> taskVos = new ArrayList<>();
        List<Task> tasks = this.taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (!Tools.isEmpty(tasks)) {
            for (Task task : tasks) {
                WfTaskVo taskVo = this.getTaskByTask(task);
                taskVos.add(taskVo);
            }
        }
        return taskVos;
    }

    /**
     * ??????????????????????????????
     *
     * @param processInstanceId ????????????ID
     * @return ??????
     */
    @Override
    public List<WfTaskVo> getTaskByProcessInstanceId(String processInstanceId) {
        List<WfTaskVo> taskVos = new ArrayList<>();
        List<HistoricTaskInstance> tasks = this.historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).list();
        if (!Tools.isEmpty(tasks)) {
            for (HistoricTaskInstance histTask : tasks) {
                WfTaskVo taskVo = this.getHistTaskByTask(histTask);
                taskVos.add(taskVo);
            }
        }
        return taskVos;
    }

    /**
     * ??????????????????????????????
     *
     * @param processInstanceId ????????????ID
     * @return ??????
     */
    @Override
    public List<WfRunTaskVo> getRunTaskByProcessInstanceId(String processInstanceId, String userId) {
        List<WfRunTaskVo> taskVos = new ArrayList<>();
        List<Task> tasks = this.taskService.createTaskQuery().processInstanceId(processInstanceId).active().taskCandidateOrAssigned(userId).list();
        if (!Tools.isEmpty(tasks)) {
            ProcessDefinitionEntity procDef = this.getProcessDefinitionEntity(tasks.get(0).getProcessDefinitionId());
            ActivityImpl first = this.getFirstActivity(procDef);
            List<ActivityImpl> last = this.getLastActivity(procDef);
            for (Task task : tasks) {
                WfTaskVo taskVo = this.getTaskByTask(task);
                WfRunTaskVo vo = this.dozerMapper.map(taskVo, WfRunTaskVo.class);
                vo.setStart(vo.getActivityId().equals(first.getId())); //?????????????????????
                for (ActivityImpl ai : last) {
                    if (vo.getActivityId().equals(ai.getId())) {
                        vo.setEnd(true); //???????????????????????????
                    }
                }
                taskVos.add(vo);
            }
        }
        return taskVos;
    }

    /**
     * ??????????????????????????????
     *
     * @param taskId ????????????ID
     * @return ??????
     */
    @Override
    public List<String> getTaskCandidates(String taskId) {
        List<String> ids = new ArrayList<>();
        List<IdentityLink> ils = this.processEngine.getTaskService().getIdentityLinksForTask(taskId);
        if (!Tools.isEmpty(ils)) {
            for (IdentityLink il : ils) {
                ids.add(il.getUserId());
            }
        }
        return ids;
    }

    /**
     * ??????????????????
     *
     * @param task
     * @return
     */
    public static boolean isStart(TaskEntity task) {
        if (!Tools.isEmpty(task.getExecution())) {
            ActivityImpl activity = task.getExecution().getActivity();
            if (!Tools.isEmpty(activity) && !Tools.isEmpty(activity.getIncomingTransitions())) {
                for (PvmTransition pt : activity.getIncomingTransitions()) {
                    if (!Tools.isEmpty(pt.getSource()) && "startEvent".equals(pt.getSource().getProperty("type"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * ??????????????????
     *
     * @param task
     * @return
     */
    public static boolean isEnd(TaskEntity task) {
        if (!Tools.isEmpty(task.getExecution())) {
            ActivityImpl activity = task.getExecution().getActivity();
            if (!Tools.isEmpty(activity) && !Tools.isEmpty(activity.getOutgoingTransitions())) {
                for (PvmTransition pt : activity.getOutgoingTransitions()) {
                    if (!Tools.isEmpty(pt.getDestination()) && "endEvent".equals(pt.getDestination().getProperty("type"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * ??????????????????URL
     *
     * @return
     */
    @Override
    public String getWfListenerConfigureUrl() {
        return this.mapper.selectWfListenerConfigureUrl();
    }

    public static void main(String[] args) {
        Map<String, Object> param = new HashMap<>();
		//param.put("sid_D16274D9_2F36_4F72_8CB5_0C6B2F9FB53A", true);
		//param.put("sid_D16274D9_2F36_4F72_8CB5_0C6B2F9FB53B", "2");
		//String expression = "${ sid_D16274D9_2F36_4F72_8CB5_0C6B2F9FB53A == true }";
        List<Object> b = new ArrayList<>();
        b.add("a");
        b.add("b");
        b.add("c");

        List<Map<?, ?>> c = new ArrayList<>();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", "a");
        m.put("b", "b");
        m.put("c", "c");

        param.put("a", "1,2,3");
        param.put("b", b);
        param.put("c", m);
        String expression = "${ expr . existActivity( nextActPart , '111111111111111111')}";
        System.out.println(expression.replace(" ", "").contains("expr.existActivity(nextActPart,"));
        System.out.println(new ActivitiServiceImpl().parseExpression(expression, param));
    }
}
