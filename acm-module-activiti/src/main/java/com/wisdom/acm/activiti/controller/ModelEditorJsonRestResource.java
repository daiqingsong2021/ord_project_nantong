/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wisdom.acm.activiti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.wisdom.acm.activiti.po.Role;
import com.wisdom.acm.activiti.po.User;
import com.wisdom.acm.activiti.service.RoleService;
import com.wisdom.acm.activiti.service.TestLeaveService;
import com.wisdom.acm.activiti.service.UserService;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.calc.calendar.Tools;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author Tijs Rademakers
 */
@RestController
@RequestMapping(value = "/service")
public class ModelEditorJsonRestResource implements ModelDataJsonConstants {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ModelEditorJsonRestResource.class);

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	ProcessEngine processEngine;

	@Autowired
	TestLeaveService testLeaveService;

	@Autowired
	UserService userService;

	@Autowired
	RoleService roleService;

	/**
	 * ???????????????
	 */
	@Autowired
	private RuntimeService runtimeService;

	/**
	 * ????????????
	 *
	 * @param model ??????
	 * @return
	 */
	private ObjectNode getEditorJson(final Model model) {
		ObjectNode modelNode = null;
		if (model != null) {
			try {
				if (StringUtils.isNotEmpty(model.getMetaInfo())) {
					modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
					modelNode.put(MODEL_NAME, model.getName());
				} else {
					modelNode = objectMapper.createObjectNode();
					modelNode.put(MODEL_NAME, model.getName());
				}
				modelNode.put(MODEL_ID, model.getId());
				byte[] source = this.repositoryService.getModelEditorSource(model.getId());
				ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(new String(source, "utf-8"));
				modelNode.putPOJO("model", editorJsonNode);
			} catch (Exception e) {
				LOGGER.error("Error creating model JSON", e);
				throw new ActivitiException("Error creating model JSON", e);
			}
		}
		return modelNode;
	}

	/**
	 * ????????????ID????????????
	 *
	 * @param modelId ??????ID
	 * @return
	 */
	@RequestMapping(value = "/model/{modelId}/json", method = RequestMethod.GET, produces = "application/json")
	public ObjectNode getEditorJson(@PathVariable final String modelId) {
		Model model = this.repositoryService.getModel(modelId);
		ObjectNode modelNode = this.getEditorJson(model);
		return modelNode;
	}

	/**
	 * ????????????ID????????????
	 *
	 * @param procDefId ??????ID
	 * @return ??????
	 */
	@RequestMapping(value = "/process/definition/{procDefId}/json", method = RequestMethod.GET, produces = "application/json")
	public ObjectNode getProcessDefinitionJson(@PathVariable final String procDefId) {
		BpmnModel bpmnModel = repositoryService.getBpmnModel(procDefId);
		return this.getObjectNode(bpmnModel);
	}

	/**
	 * ????????????ID????????????
	 *
	 * @param deployId ??????ID
	 * @return ??????
	 */
	@RequestMapping(value = "/process/deployment/{deployId}/json", method = RequestMethod.GET, produces = "application/json")
	public ObjectNode getDeploymentJson(@PathVariable final String deployId) {
		ProcessDefinition procDef = this.repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
		ObjectNode modelNode = this.getProcessDefinitionJson(procDef.getId());
		return modelNode;
	}

	/**
	 * ??????????????????ID????????????
	 *
	 * @param procInstId ????????????ID
	 * @return?????????
	 */
	@RequestMapping(value = "/process/instance/{procInstId}/json", method = RequestMethod.GET, produces = "application/json")
	public ObjectNode getProcessInstanceJson(@PathVariable final String procInstId) {
		ObjectNode modelNode = null;
		HistoricProcessInstance procInst = processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(procInstId).singleResult();
		if (procInst != null) {
			ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(procInst.getProcessDefinitionId());
			BpmnModel bpmnModel = repositoryService.getBpmnModel(procInst.getProcessDefinitionId());
			List<HistoricActivityInstance> hais = this.getActivityInstance(definition, procInst); //??????????????????
			modelNode = this.getObjectNode(bpmnModel);
			Map<String, List<HistoricActivityInstance>> haiMap = this.getGroupHistoricActivityInstances(hais); //??????????????????
			modelNode.putPOJO("activityInstances", hais); //????????????
			modelNode.putPOJO("activityTitles", this.getActivityTitle(bpmnModel, hais)); //???????????????????????????
			modelNode.putPOJO("flows", this.getHighLightedFlows(definition, haiMap)); //????????????
		}
		return modelNode;
	}

	/**
	 * ??????????????????(??????????????????????????????????????????<????????????>)
	 *
	 * @param definition ????????????
	 * @param procInst   ????????????
	 * @return ????????????
	 */
	private List<HistoricActivityInstance> getActivityInstance(ProcessDefinitionEntity definition, HistoricProcessInstance procInst) {
		List<HistoricActivityInstance> hais = this.processEngine.getHistoryService().createHistoricActivityInstanceQuery() //
			.processInstanceId(procInst.getId()).orderByHistoricActivityInstanceStartTime().asc().list();
		Map<String, HistoricActivityInstance> haiMap = this.getLastHistoricActivityInstances(hais); //???????????????????????????
		for (HistoricActivityInstance ha : haiMap.values()) {
			if ("startEvent".equals(ha.getActivityType())) {
				PvmActivity activity = definition.findActivity(ha.getActivityId()); //??????????????????
				getAllNextActivity(haiMap, ha, activity.getOutgoingTransitions());   //???????????????????????????
				break;
			}
		}
		return new ArrayList(haiMap.values());
	}

	/**
	 * ???????????????????????????????????????????????????????????????
	 *
	 * @param haiMap         ??????????????????
	 * @param actInst        ???????????????
	 * @param outTransitions ??????????????????????????????
	 */
	private void getAllNextActivity(Map<String, HistoricActivityInstance> haiMap, HistoricActivityInstance actInst, List<PvmTransition> outTransitions) {
		if (!Tools.isEmpty(outTransitions)) {
			for (PvmTransition pt : outTransitions) {
				PvmActivity ac = pt.getDestination(); //???????????????????????????
				HistoricActivityInstance ha = haiMap.get(ac.getId());
				if (!ObjectUtils.isEmpty(ha)) {
					Date actDate = ObjectUtils.isEmpty(ha.getEndTime()) ? ha.getStartTime() : ha.getEndTime();
					if (ObjectUtils.isEmpty(actInst.getEndTime()) || actInst.getEndTime().after(actDate)) {
						haiMap.remove(ha.getActivityId()); //??????????????????????????????????????????????????????????????????????????????
						this.getAllNextActivity(haiMap, actInst, ac.getOutgoingTransitions());
					} else {
						this.getAllNextActivity(haiMap, ha, ac.getOutgoingTransitions());
					}
				} else {
					this.getAllNextActivity(haiMap, actInst, ac.getOutgoingTransitions());
				}
			}
		}
	}

	/**
	 * ???????????????????????????
	 *
	 * @param hais ????????????
	 * @return ??????
	 */
	private Map<String, HistoricActivityInstance> getLastHistoricActivityInstances(final List<HistoricActivityInstance> hais) {
		Map<String, HistoricActivityInstance> haiMap = new LinkedHashMap<>();
		for (HistoricActivityInstance hai : hais) {
			haiMap.put(hai.getActivityId(), hai);
		}
		return haiMap;
	}

	/**
	 * bpmnModel???objectNode
	 *
	 * @param bpmnModel BpmnModel
	 * @return ObjectNode
	 */
	public ObjectNode getObjectNode(final BpmnModel bpmnModel) {
		ObjectNode modelNode = objectMapper.createObjectNode();
		if (bpmnModel != null) {
			ObjectNode editorJsonNode = new BpmnJsonConverter().convertToJson(bpmnModel);
			modelNode.putPOJO("model", editorJsonNode);
		}
		return modelNode;
	}

	/**
	 * ????????????????????????
	 *
	 * @param hais ????????????
	 * @return ??????
	 */
	private Map<String, List<HistoricActivityInstance>> getGroupHistoricActivityInstances(final List<HistoricActivityInstance> hais) {
		Map<String, List<HistoricActivityInstance>> haiMap = new LinkedHashMap<>();
		for (HistoricActivityInstance hai : hais) {// ??????????????????????????????????????????
			List<HistoricActivityInstance> ais = haiMap.get(hai.getActivityId());
			if (ais == null) {
				ais = new ArrayList<>();
			}
			ais.add(hai);
			haiMap.put(hai.getActivityId(), ais);
		}
		return haiMap;
	}

	/**
	 * ????????????????????????
	 *
	 * @param procDefEntity ????????????
	 * @param haiMap        ??????
	 * @return ??????ID
	 */
	private List<String> getHighLightedFlows(ProcessDefinitionEntity procDefEntity, Map<String, List<HistoricActivityInstance>> haiMap) {
		Set<String> highFlows = new HashSet<>();// ????????????????????????flowId
		for (List<HistoricActivityInstance> hais : haiMap.values()) {// ?????????????????????????????????
			for (HistoricActivityInstance hai : hais) {// ?????????????????????????????????
				ActivityImpl activityImpl = procDefEntity.findActivity(hai.getActivityId());// ?????????????????????????????????
				List<PvmTransition> transitions = activityImpl.getOutgoingTransitions();// ?????????????????????????????????
				for (PvmTransition transition : transitions) {// ???????????????????????????
					List<HistoricActivityInstance> nextHai = haiMap.get(transition.getDestination().getId());
					if (nextHai != null) {
						if (nextHai.size() == 1) {  //?????????????????????????????????????????????
							highFlows.add(transition.getId());
						} else {
							for (HistoricActivityInstance _hai : nextHai) {// ????????????????????????????????????????????????????????????????????????ID???????????????
								if (hai.getExecutionId().equals(_hai.getExecutionId())) {
									highFlows.add(transition.getId());
								}
							}
						}
					}
				}
			}
		}
		return new ArrayList<>(highFlows);
	}

	/**
	 * ???????????????????????????
	 *
	 * @param bpmnModel ????????????
	 * @param hais      ????????????
	 */
	private Map<String, Object> getActivityTitle(BpmnModel bpmnModel, List<HistoricActivityInstance> hais) {
		Map<String, Object> titles = new LinkedHashMap<>();
		Map<String, User> userMap = this.getActivityUser(bpmnModel, hais); //????????????????????????
		Map<String, Role> roleMap = this.getActivityGroups(bpmnModel, hais); //????????????????????????
		if (!Tools.isEmpty(bpmnModel) && !Tools.isEmpty(bpmnModel.getProcesses())) { //?????????????????????????????????
			Collection<FlowElement> flowElementList = bpmnModel.getProcesses().get(0).getFlowElements();
			for (FlowElement flow : flowElementList) {
				if (flow instanceof UserTask) {
					UserTask userTask = (UserTask) flow;
					List<String> names = new ArrayList<>();
					for (String userId : userTask.getCandidateUsers()) {
						User user = userMap.get(userId);
						names.add(Tools.isEmpty(user) ? "" : (user.getUserName() + "(" + user.getLoginId() + ")"));
					}
					for (String groupId : userTask.getCandidateGroups()) {
						Role role = roleMap.get(groupId);
						names.add(Tools.isEmpty(role) ? "" : role.getName());
					}
					titles.put(userTask.getId(), names.toString().replace("[", "").replace("]", ""));
				}
			}
		}
		this.updateActivityTitle(titles, userMap, roleMap, hais); //???????????????????????????
		return titles;
	}

	/**
	 * ???????????????????????????
	 *
	 * @param titles  ???????????????
	 * @param userMap ??????
	 * @param roleMap ??????
	 * @param hais    ????????????
	 */
	private void updateActivityTitle(Map<String, Object> titles, Map<String, User> userMap, Map<String, Role> roleMap, List<HistoricActivityInstance> hais) {
		for (HistoricActivityInstance act : hais) {
			if (!Tools.isEmpty(act.getAssignee())) {
				User user = userMap.get(act.getAssignee());
				String assigneeName = Tools.isEmpty(user) ? "" : (user.getUserName() + "(" + user.getLoginId() + ")");
				String time = Tools.isEmpty(act.getEndTime()) ? Tools.toDateTimeString(act.getStartTime()) : Tools.toDateTimeString(act.getEndTime());
				titles.put(act.getActivityId(), assigneeName + " " + time);
			} else if (!Tools.isEmpty(act.getTaskId())) {
				List<IdentityLink> ils = this.processEngine.getTaskService().getIdentityLinksForTask(act.getTaskId());
				if (!Tools.isEmpty(ils)) {
					List<String> names = new ArrayList<>();
					for (IdentityLink il : ils) {
						if (!Tools.isEmpty(il.getUserId())) {
							User user = userMap.get(il.getUserId());
							names.add(Tools.isEmpty(user) ? "" : (user.getUserName() + "(" + user.getLoginId() + ")"));
						}
						if (!Tools.isEmpty(il.getGroupId())) {
							Role role = roleMap.get(il.getGroupId());
							names.add(Tools.isEmpty(role) ? "" : role.getName());
						}
					}
					titles.put(act.getActivityId(), names.toString().replace("[", "").replace("]", ""));
				}
			}
		}
	}

	/**
	 * ?????????????????????
	 *
	 * @param bpmnModel ????????????
	 * @param hais      ????????????
	 * @return ????????????
	 */
	private Map<String, User> getActivityUser(BpmnModel bpmnModel, List<HistoricActivityInstance> hais) {
		Map<String, User> userMap = new LinkedHashMap<>();
		Set<String> users = new HashSet<>();
		if (!Tools.isEmpty(bpmnModel) && !Tools.isEmpty(bpmnModel.getProcesses())) {
			Collection<FlowElement> flowElementList = bpmnModel.getProcesses().get(0).getFlowElements();
			for (FlowElement flow : flowElementList) {
				if (flow instanceof UserTask) {
					UserTask userTask = (UserTask) flow;
					users.addAll(userTask.getCandidateUsers());
				}
			}
		}
		for (HistoricActivityInstance act : hais) {
			if (!Tools.isEmpty(act.getAssignee())) {
				users.add(act.getAssignee());
			} else if (!Tools.isEmpty(act.getTaskId())) {
				List<IdentityLink> ils = this.processEngine.getTaskService().getIdentityLinksForTask(act.getTaskId());
				if (!Tools.isEmpty(ils)) {
					for (IdentityLink il : ils) {
						if (!Tools.isEmpty(il.getUserId())) {
							users.add(il.getUserId());
						}
					}
				}
			}
		}
		if (!Tools.isEmpty(users)) {
			userMap = ListUtil.listToMap(this.userService.getListById(new ArrayList<>(users)), "id", String.class);
		}
		return userMap;
	}

	/**
	 * ????????????????????????
	 *
	 * @param bpmnModel ????????????
	 * @return ?????????
	 */
	private Map<String, Role> getActivityGroups(BpmnModel bpmnModel, List<HistoricActivityInstance> hais) {
		Map<String, Role> roleMap = new LinkedHashMap<>();
		Set<String> groups = new HashSet<>();
		if (!Tools.isEmpty(bpmnModel) && !Tools.isEmpty(bpmnModel.getProcesses())) {
			Collection<FlowElement> flowElementList = bpmnModel.getProcesses().get(0).getFlowElements();
			for (FlowElement flow : flowElementList) {
				if (flow instanceof UserTask) {
					UserTask userTask = (UserTask) flow;
					groups.addAll(userTask.getCandidateGroups());
				}
			}
		}
		for (HistoricActivityInstance act : hais) {
			if (Tools.isEmpty(act.getAssignee()) && !Tools.isEmpty(act.getTaskId())) {
				List<IdentityLink> ils = this.processEngine.getTaskService().getIdentityLinksForTask(act.getTaskId());
				if (!Tools.isEmpty(ils)) {
					for (IdentityLink il : ils) {
						if (!Tools.isEmpty(il.getGroupId())) {
							groups.add(il.getGroupId());
						}
					}
				}
			}
		}
		if (!Tools.isEmpty(groups)) {
			roleMap = ListUtil.listToMap(this.roleService.getListById(new ArrayList<>(groups)), "id", String.class);
		}
		return roleMap;
	}
}
