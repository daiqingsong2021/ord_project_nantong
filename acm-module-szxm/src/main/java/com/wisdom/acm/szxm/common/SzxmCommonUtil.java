package com.wisdom.acm.szxm.common;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wisdom.acm.szxm.common.officeUtils.ExcelError;
import com.wisdom.acm.szxm.common.officeUtils.ExcelUtil;
import com.wisdom.acm.szxm.mapper.SysMessageMapper;
import com.wisdom.acm.szxm.mapper.menu.MenuMapper;
import com.wisdom.acm.szxm.po.SysMessageRecvPo;
import com.wisdom.acm.szxm.po.SysMessageUserPo;
import com.wisdom.acm.szxm.service.app.EwxService;
import com.wisdom.acm.szxm.service.wf.ActivitiTemplateService;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommCalendarService;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.feign.CommProjectTeamService;
import com.wisdom.base.common.feign.CommUserService;
import com.wisdom.base.common.feign.LeafService;
import com.wisdom.base.common.form.SysMessageAddForm;
import com.wisdom.base.common.form.WfRuningProcessForm;
import com.wisdom.base.common.msg.ApiResult;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.TreeUtil;
import com.wisdom.base.common.util.calc.calendar.PcCalendar;
import com.wisdom.base.common.util.calc.calendar.PmCalendar;
import com.wisdom.base.common.vo.CalendarVo;
import com.wisdom.base.common.vo.DictionaryVo;
import com.wisdom.base.common.vo.GeneralVo;
import com.wisdom.base.common.vo.ProjectTeamUserVo;
import com.wisdom.base.common.vo.ProjectTeamVo;
import com.wisdom.base.common.vo.SectionTreeVo;
import com.wisdom.base.common.vo.TreeVo;
import com.wisdom.base.common.vo.UserInfo;
import com.wisdom.base.common.vo.UserVo;
import com.wisdom.base.common.vo.wf.WfActivityVo;
import com.wisdom.base.common.vo.wf.WfCandidateGroupVo;
import com.wisdom.base.common.vo.wf.WfCandidateUserVo;
import com.wisdom.webservice.token.Md5Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ?????????????????????
 */
@Component
public class SzxmCommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(SzxmCommonUtil.class);
    /**
     * ??????????????????
     */
    @Autowired
    private CommDictService commDictService;

    @Autowired
    private CommProjectTeamService commProjectTeamService;

    /**
     * ????????????????????????
     */
    @Autowired
    private CommUserService commUserService;

    @Autowired
    private org.dozer.Mapper dozerMapper;

    @Autowired
    private CommCalendarService commCalendarService;

    @Autowired
    private SysMessageMapper sysMessageMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private ActivitiTemplateService activitiTemplateService;

    @Autowired
    private LeafService leafService;

    @Autowired
    private EwxService ewxService;

    /**
     * ?????????????????????????????????List
     *
     * @param projectId  ????????????ID
     * @param sectionIds ????????????IDS
     * @return
     */
    public List<String> getSectionList(String projectId, String sectionIds) {
        List<String> sectionList = Lists.newArrayList();

        if (StringHelper.isNullAndEmpty(projectId)) {
            throw new BaseException("??????ID ??????????????????");
        } else if (StringHelper.isNotNullAndEmpty(projectId) && StringHelper.isNotNullAndEmpty(sectionIds)) {
            String[] sectionIdsArray = sectionIds.split(",");
            sectionList = new ArrayList<String>(Arrays.asList(sectionIdsArray));
        } else {
            UserInfo userInfo = commUserService.getLoginUser();//??????????????????
            List<SectionTreeVo> sectionTreeVoList =
                    commProjectTeamService.querySectionTreeList(Integer.valueOf(projectId), userInfo.getId());
            //????????? ????????????
            sectionList = toTreeIds(sectionTreeVoList);
        }
        if (ObjectUtils.isEmpty(sectionList))
            sectionList.add("");
        return sectionList;
    }

    private List<String> toTreeIds(List<SectionTreeVo> sectionTreeVoList) {
        List<String> sectionList = Lists.newArrayList();

        if (!ObjectUtils.isEmpty(sectionTreeVoList)) {
            for (SectionTreeVo sectionTreeVo : sectionTreeVoList) {
                sectionList.add(String.valueOf(sectionTreeVo.getId()));
                if (!ObjectUtils.isEmpty(sectionTreeVo.getChildren()))
                    sectionList.addAll(toTreeIds(sectionTreeVo.getChildren()));
            }
        }
        return sectionList;
    }

    /**
     * ??????????????????????????????
     *
     * @param dictionaryType ????????????
     * @param dictionaryCode ????????????
     * @return
     */
    public String getDictionaryName(String dictionaryType, String dictionaryCode) {
        String dictionaryName = "";
        Map<String, DictionaryVo> dictMap = commDictService.getDictMapByTypeCode(dictionaryType);
        if (!ObjectUtils.isEmpty(dictMap) && !StringUtils.isEmpty(dictionaryCode)) {
            dictionaryName = String.valueOf(dictMap.get(dictionaryCode).getName());
        }
        return dictionaryName;
    }

    /**
     * ?????????????????? Map ??????code?????????name
     *
     * @param dictMap
     * @param dictionaryCode
     * @return
     */
    public String getDictionaryName(Map<String, DictionaryVo> dictMap, String dictionaryCode) {
        String dictionaryName = "";
        if (!ObjectUtils.isEmpty(dictMap) && !StringUtils.isEmpty(dictionaryCode)) {
            dictionaryName = String.valueOf(dictMap.get(dictionaryCode).getName());
        }
        return dictionaryName;
    }

    /**
     * ????????????Map??????
     * commProjectTeamService.querySectionList()
     *
     * @param
     * @return
     */
    public Map<Integer, ProjectTeamVo> getSectionMap(Integer projectId) {
        List<ProjectTeamVo> sectionVos = commProjectTeamService.querySectioinListByProjectId(projectId);
        Map<Integer, ProjectTeamVo> mapTeam = ListUtil.listToMap(sectionVos, "id", Integer.class);
        return mapTeam;
    }

    /**
     * ??????????????????????????????Code
     *
     * @param dictionaryType ????????????
     * @param dictionaryName ????????????
     * @return
     */
    public String getDictionaryCode(String dictionaryType, String dictionaryName) {
        String dictionaryCode = "";
        Map<String, DictionaryVo> dictMap = commDictService.getDictMapByTypeCode(dictionaryType);
        if (!ObjectUtils.isEmpty(dictMap) && StringHelper.isNotNullAndEmpty(dictionaryName)) {
            for (Map.Entry<String, DictionaryVo> entry : dictMap.entrySet()) {
                DictionaryVo value = entry.getValue();
                if (value.getName().equals(dictionaryName))
                    return entry.getKey();
            }
        }
        return dictionaryCode;
    }

    public String getDictionaryCode(Map<String, DictionaryVo> dictMap, String dictionaryName) {
        String dictionaryCode = "";
        if (!ObjectUtils.isEmpty(dictMap) && !StringUtils.isEmpty(dictionaryName)) {
            for (Map.Entry<String, DictionaryVo> entry : dictMap.entrySet()) {
                DictionaryVo value = entry.getValue();
                if (value.getName().equals(dictionaryName))
                    return entry.getKey();
            }
        }
        return dictionaryCode;
    }

    /**
     * ?????????????????????
     *
     * @param treeNodes
     * @param <T>
     * @return
     */
    public <T extends TreeVo> List<T> generateTree(List<T> treeNodes) {
        List<T> retList = Lists.newArrayList();
        if (!ObjectUtils.isEmpty(treeNodes)) {
            List<Integer> idList = ListUtil.toIdList(treeNodes);
            List<T> rootNodes = Lists.newArrayList();//??????????????????
            for (T treeNode : treeNodes) {
                if (!idList.contains(treeNode.getParentId())) {
                    //????????????????????????????????????????????????
                    rootNodes.add(treeNode);
                }
            }
            for (T rootNode : rootNodes) {//???????????????????????????????????????????????????
                //List<T> tree =TreeUtil.bulid(treeNodes, rootNode.getId());
                //???????????????
                Map<Integer, List<T>> childrenMap = TreeUtil.toChildrenMap(treeNodes);
                List<T> childrenList = this.bulidChildren(childrenMap, rootNode.getId());
                rootNode.setChildren(childrenList);
                retList.add(rootNode);
            }
        }
        return retList;
    }

    /**
     * ?????????????????????
     *
     * @param childrenMap
     * @param parentId
     * @param <T>
     * @return
     */
    private static <T extends TreeVo> List<T> bulidChildren(Map<Integer, List<T>> childrenMap, Integer parentId) {
        List<T> list = childrenMap.get(parentId);
        if (!ObjectUtils.isEmpty(list)) {
            for (T t : list) {
                // ?????????????????????
                List<T> children = bulidChildren(childrenMap, t.getId());
                t.setChildren(null);
                if (!ObjectUtils.isEmpty(children)) {
                    t.setChildren(children);
                }
            }
        }

        return list;
    }

    /**
     * ????????????ID ?????? ???????????? ??????????????????????????????????????????
     *
     * @param sectionId ??????ID
     * @param roleCode  ????????????
     * @param sections  ??????????????????
     * @return
     */
    public List<ProjectTeamUserVo> getSgdwRoleUser(List<ProjectTeamVo> sections, Integer sectionId, String roleCode) {
        for (ProjectTeamVo projectTeamVo : sections) {
            if (sectionId.equals(projectTeamVo.getId())) {//???????????? ??????ID ???????????????

                if (ObjectUtils.isEmpty(projectTeamVo.getCuList()))
                    return Lists.newArrayList();
                List<ProjectTeamUserVo> projectTeamUserVos = Lists.newArrayList();
                List<GeneralVo> cuList = projectTeamVo.getCuList();
                for (GeneralVo cuGeneralVo : cuList) {
                    //?????????????????????????????????
                    List<ProjectTeamUserVo> allTeamUserVos =
                            commProjectTeamService.queryUserListByProjectTeamId(cuGeneralVo.getId());

                    for (ProjectTeamUserVo projectTeamUserVo : allTeamUserVos) {
                        List<GeneralVo> roles = projectTeamUserVo.getRoles();
                        if (!ObjectUtils.isEmpty(roles)) {
                            for (int i = 0; i < roles.size(); i++) {
                                GeneralVo generalVo = roles.get(i);
                                if (roleCode.equals(generalVo.getCode())) {
                                    projectTeamUserVos.add(projectTeamUserVo);
                                }
                            }
                        }
                    }
                }
                return projectTeamUserVos;
            }
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param projectTeamVo ??????
     * @param roleCode      ????????????
     * @return
     */
    public List<ProjectTeamUserVo> getSgdwRoleUser(ProjectTeamVo projectTeamVo, String roleCode) {
        if (ObjectUtils.isEmpty(projectTeamVo.getCuList()))
            return Lists.newArrayList();
        List<ProjectTeamUserVo> projectTeamUserVos = Lists.newArrayList();
        List<GeneralVo> cuList = projectTeamVo.getCuList();
        for (GeneralVo cuGeneralVo : cuList) {
            //?????????????????????????????????
            List<ProjectTeamUserVo> allTeamUserVos =
                    commProjectTeamService.queryUserListByProjectTeamId(cuGeneralVo.getId());

            for (ProjectTeamUserVo projectTeamUserVo : allTeamUserVos) {
                List<GeneralVo> roles = projectTeamUserVo.getRoles();
                if (!ObjectUtils.isEmpty(roles)) {
                    for (int i = 0; i < roles.size(); i++) {
                        GeneralVo generalVo = roles.get(i);
                        if (roleCode.equals(generalVo.getCode())) {
                            projectTeamUserVos.add(projectTeamUserVo);
                        }
                    }
                }
            }
        }
        return projectTeamUserVos;
    }

    /**
     * ????????????ID ?????? ???????????? ??????????????????????????????????????????
     *
     * @param sectionId ??????ID
     * @param roleCode  ????????????
     * @param sections  ??????????????????
     * @return
     */
    public List<ProjectTeamUserVo> getJldwRoleUser(List<ProjectTeamVo> sections, Integer sectionId, String roleCode) {

        for (ProjectTeamVo projectTeamVo : sections) {
            if (sectionId.equals(projectTeamVo.getId())) {//???????????? ??????ID ???????????????

                if (ObjectUtils.isEmpty(projectTeamVo.getCcuList()))
                    return Lists.newArrayList();
                List<ProjectTeamUserVo> projectTeamUserVos = Lists.newArrayList();
                List<GeneralVo> cCuList = projectTeamVo.getCcuList();
                for (GeneralVo cCuGeneralVo : cCuList) {
                    //?????????????????????????????????
                    List<ProjectTeamUserVo> allTeamUserVos =
                            commProjectTeamService.queryUserListByProjectTeamId(cCuGeneralVo.getId());

                    for (ProjectTeamUserVo projectTeamUserVo : allTeamUserVos) {
                        List<GeneralVo> roles = projectTeamUserVo.getRoles();
                        if (!ObjectUtils.isEmpty(roles)) {
                            for (int i = 0; i < roles.size(); i++) {
                                GeneralVo generalVo = roles.get(i);
                                if (roleCode.equals(generalVo.getCode())) {
                                    projectTeamUserVos.add(projectTeamUserVo);
                                }
                            }
                        }
                    }
                }
                return projectTeamUserVos;
            }
        }
        return null;
    }

    public List<ProjectTeamUserVo> getJldwRoleUser(ProjectTeamVo projectTeamVo, String roleCode) {
        if (ObjectUtils.isEmpty(projectTeamVo.getCcuList()))
            return Lists.newArrayList();
        List<ProjectTeamUserVo> projectTeamUserVos = Lists.newArrayList();
        List<GeneralVo> cCuList = projectTeamVo.getCcuList();
        for (GeneralVo cCuGeneralVo : cCuList) {
            //?????????????????????????????????
            List<ProjectTeamUserVo> allTeamUserVos =
                    commProjectTeamService.queryUserListByProjectTeamId(cCuGeneralVo.getId());

            for (ProjectTeamUserVo projectTeamUserVo : allTeamUserVos) {
                List<GeneralVo> roles = projectTeamUserVo.getRoles();
                if (!ObjectUtils.isEmpty(roles)) {
                    for (int i = 0; i < roles.size(); i++) {
                        GeneralVo generalVo = roles.get(i);
                        if (roleCode.equals(generalVo.getCode())) {
                            projectTeamUserVos.add(projectTeamUserVo);
                        }
                    }
                }
            }
        }
        return projectTeamUserVos;
    }

    /**
     * ????????????ID ??????????????????
     *
     * @param sectionId ??????ID
     * @return
     */
    public List<ProjectTeamUserVo> getOwnerList(Integer sectionId) {
        List<ProjectTeamUserVo> returnList = Lists.newArrayList();
        //???????????????????????????????????????????????????
        List<ProjectTeamUserVo> ownerList =
                commProjectTeamService.queryUserListByProjectTeamId(sectionId);
        //List<ProjectTeamUserVo> ownerList = commProjectTeamService.getOwnerBySectionId(Integer.valueOf(sectionId));
        if (ObjectUtils.isEmpty(ownerList))
            return Lists.newArrayList();
        else {
            for (ProjectTeamUserVo projectTeamUserVo : ownerList) {
                List<GeneralVo> roles = projectTeamUserVo.getRoles();
                if (!ObjectUtils.isEmpty(roles)) {
                    for (int i = 0; i < roles.size(); i++) {
                        GeneralVo generalVo = roles.get(i);
                        if ("YZ_YZDB".equals(generalVo.getCode())) {
                            returnList.add(projectTeamUserVo);
                        }
                    }
                }
            }

        }
        return returnList;
    }

    /**
     * ??????excel??????????????????????????????list,????????????????????????????????????????????????????????????excel??????
     *
     * @param excelError
     * @param inputStream
     * @return
     * @throws FileNotFoundException
     * @throws InvalidFormatException
     * @throws BaseException
     * @throws IOException
     */
    public Workbook exportExcelError(ExcelError excelError, InputStream inputStream) throws
            FileNotFoundException,
            InvalidFormatException,
            BaseException,
            IOException {

        List<Map<String, Object>> errorList = excelError.getErrorList();
        //
        String[][] cellArr = {{"0", "error", "", "", "left", "", "", "", "", ""}};
        List<Map<String, Object>> errorMapList = new ArrayList<Map<String, Object>>();

        Map<String, Object> errorMap;
        if (!ObjectUtils.isEmpty(errorList)) {
            String error = "";
            int sheetnum = 0;
            int rownum = 0;

            for (Map<String, Object> map : errorList) {
                error = String.valueOf(map.get("error"));
                sheetnum = Integer.valueOf(String.valueOf(map.get("sheetnum")));
                rownum = Integer.valueOf(String.valueOf(map.get("row")));
                error = "???" + (sheetnum + 1) + "????????????" + rownum + "????????????" + error;

                errorMap = new HashMap<String, Object>();
                errorMap.put("error", error);
                errorMapList.add(errorMap);
            }
        }

        Workbook workbook = ExcelUtil.getWorkbook(inputStream);
        ExcelUtil.writeSheet(workbook, 0, 1, cellArr, errorMapList);

        return workbook;
    }

    /**
     * ??????excel??????????????????last?????????????????????????????????????????????
     *
     * @param excelError
     * @return
     * @throws InvalidFormatException
     * @throws IOException
     * @throws BaseException
     */
    public Workbook exportExcelErrorByLast(ExcelError excelError) throws
            InvalidFormatException,
            IOException,
            BaseException {

        List<Map<String, Object>> errorList = excelError.getErrorList();
        Workbook workbook = excelError.getWorkBook();
        List<Map<String, Object>> sheetConfigList = excelError.getSheetConfig();
        // ???????????????
        Map<String, Integer> maxCellMap = this.getMaxCellMap(workbook, sheetConfigList);

        CellStyle cellstyle = workbook.createCellStyle();

        for (Map<String, Object> map : sheetConfigList) {
            int sheetnum = Integer.valueOf(String.valueOf(map.get("sheet")));
            int row = Integer.valueOf(String.valueOf(map.get("row")));

            Sheet sheet = ExcelUtil.getSheet(workbook, sheetnum);

            for (int i = row; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row r = sheet.getRow(i);
                int maxcellnum = maxCellMap.get(sheetnum + "");
                ExcelUtil.writeCell(r, maxcellnum, "", cellstyle);
            }
        }

        Font font = workbook.createFont();
        font.setColor(HSSFColor.RED.index);
        cellstyle.setFont(font);

        for (Map<String, Object> map : errorList) {
            int sheetnum = Integer.valueOf(String.valueOf(map.get("sheetnum")));
            int rownum = Integer.valueOf(String.valueOf(map.get("row")));
            int maxcellnum = maxCellMap.get(sheetnum + "");
            String error = String.valueOf(map.get("error"));

            Sheet sheet = ExcelUtil.getSheet(workbook, sheetnum);
            Row row = sheet.getRow(rownum - 1);

            ExcelUtil.writeCell(row, maxcellnum, error, cellstyle);
        }

        return workbook;
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param workbook
     * @param sheetConfigList
     * @return
     * @throws InvalidFormatException
     * @throws IOException
     * @throws BaseException
     */
    private Map<String, Integer> getMaxCellMap(Workbook workbook, List<Map<String, Object>> sheetConfigList) throws
            InvalidFormatException,
            IOException,
            BaseException {

        Map<String, Integer> maxCellMap = new HashMap<String, Integer>();
        if (!ObjectUtils.isEmpty(sheetConfigList)) {
            Sheet sheet;
            int sheetnum, row;
            for (Map<String, Object> map : sheetConfigList) {
                sheetnum = Integer.valueOf(String.valueOf(map.get("sheet")));
                row = Integer.valueOf(String.valueOf(map.get("row")));
                sheet = ExcelUtil.getSheet(workbook, sheetnum);
                int maxCell = 0;
                for (int i = 0; i < row; i++) {
                    Row r = sheet.getRow(i);
                    int c = r.getLastCellNum();
                    if (c > maxCell)
                        maxCell = c;
                }

                maxCellMap.put(sheetnum + "", maxCell);
            }
        }

        return maxCellMap;
    }

    /**
     * pageInfo????????????
     *
     * @param source ???
     * @param dest   ??????
     * @param <T>
     * @param <D>
     */
    public <T, D> void cooyPageInfo(PageInfo<T> source, PageInfo<D> dest) {
        dest.setPageNum(source.getPageNum());
        dest.setPageSize(source.getPageSize());
        dest.setSize(source.getSize());
        dest.setTotal(source.getTotal());
        dest.setEndRow(source.getEndRow());
        dest.setStartRow(source.getStartRow());
        dest.setHasNextPage(source.isHasNextPage());
        dest.setHasPreviousPage(source.isHasPreviousPage());
        dest.setIsFirstPage(source.isIsFirstPage());
        dest.setIsLastPage(source.isIsLastPage());
        dest.setNavigateFirstPage(source.getNavigateFirstPage());
        dest.setNavigateLastPage(source.getNavigateLastPage());
        dest.setNextPage(source.getNextPage());
        dest.setPages(source.getPages());
        dest.setPrePage(source.getPrePage());
    }

    /**
     * ????????????
     *
     * @param birthDay
     * @return
     */
    public int getAge(Date birthDay) {
        if (ObjectUtils.isEmpty(birthDay))
            return 0;
        Calendar cal = Calendar.getInstance();
        if (cal.before(birthDay)) { //?????????????????????????????????????????????
            return 0;
        }
        int yearNow = cal.get(Calendar.YEAR);  //????????????
        int monthNow = cal.get(Calendar.MONTH);  //????????????
        int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH); //????????????
        cal.setTime(birthDay);
        int yearBirth = cal.get(Calendar.YEAR);
        int monthBirth = cal.get(Calendar.MONTH);
        int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);
        int age = yearNow - yearBirth;   //???????????????
        if (monthNow <= monthBirth) {
            if (monthNow == monthBirth) {
                if (dayOfMonthNow < dayOfMonthBirth)
                    age--;//??????????????????????????????????????????
            } else {
                age--;//??????????????????????????????????????????

            }
        }
        return age;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param projectId
     * @param userId
     * @return
     */
    public List<GeneralVo> queryTeamRoles(Integer projectId, Integer userId) {
        List<GeneralVo> roles = sysMessageMapper.queryTeamRoles(projectId, userId);
        return roles;
    }

    /**
     * ??????????????????
     *
     * @param sysMessageAddForm
     */
    public void sendMessageRecv(SysMessageAddForm sysMessageAddForm) {
        //??????????????????
        this.sendXtMessageRecv(sysMessageAddForm);
        //?????????????????????????????????
        ewxService.sendEwxMessage(sysMessageAddForm);
        //???????????????????????????
        PropertiesLoaderUtils prop = new PropertiesLoaderUtils("application.yml");
        String neruServiceUrl = prop.getProperty("neru.serviceUrl");
        String neruAppId = prop.getProperty("neru.appId");
        String neruSecretKey = prop.getProperty("neru.secretKey");
        for (UserVo recvUser : sysMessageAddForm.getRecvUser()) {
            Map<String, String> paramsMap = Maps.newHashMap();
            paramsMap.put("sender", neruAppId);
            paramsMap.put("senderName", "????????????");
            paramsMap.put("token", getNeruSoftToken(neruServiceUrl, neruAppId, neruSecretKey));

            JSONObject message = new JSONObject();
            message.put("recipient", recvUser.getCode());
            JSONObject msg = new JSONObject();
            msg.put("msg", sysMessageAddForm.getContent());
            msg.put("title", sysMessageAddForm.getTitle());
            msg.put("creatorName", "????????????????????????");
            msg.put("recevieTime",
                    DateUtil.getDateFormat(sysMessageAddForm.getClaimDealTime(), DateUtil.DATETIME_DEFAULT_FORMAT));
            msg.put("url", neruServiceUrl + "/forward?appUrl=");
            msg.put("redirectUrl", "https://mobileoffice.sz-mtr.com:8106");
            msg.put("avatar", "");
            msg.put("type", "text");
            msg.put("fun", "CO");
            message.put("message", msg.toString());

            paramsMap.put("message", message.toString());
            //???????????????????????????
            ewxService.sendNeuSoftMessage(paramsMap);
        }
    }

    private void sendXtMessageRecv(SysMessageAddForm sysMessageAddForm) {
        if (StringHelper.isNotNullAndEmpty(sysMessageAddForm.getPcContent()))
            sysMessageAddForm.setContent(sysMessageAddForm.getPcContent());

        SysMessageRecvPo sysMessageRecvPo = dozerMapper.map(sysMessageAddForm, SysMessageRecvPo.class);
        //????????????
        sysMessageRecvPo.setSendTime(new Date());
        //??????del??????0???????????????
        sysMessageRecvPo.setDel(0);
        //??????????????????
        sysMessageRecvPo.setType("SYSTEMNOTICE");
        //??????????????????
        sysMessageRecvPo.setCollect(0);
        sysMessageRecvPo.setOptType(0);
        sysMessageRecvPo.setParentId(0);
        sysMessageRecvPo.setSource(1);
        //??????wsd_sys_message
        Integer messageId = leafService.getId();
        sysMessageRecvPo.setId(messageId);
        sysMessageMapper.addSysMessage(sysMessageRecvPo);

        //??????????????????????????????
        //?????????????????????
        if (!ObjectUtils.isEmpty(sysMessageAddForm.getRecvUser())) {
            for (UserVo userVo : sysMessageAddForm.getRecvUser()) {
                Integer recvUserId = userVo.getId();
                SysMessageUserPo sysMessageUserPo = new SysMessageUserPo();
                //??????id
                sysMessageUserPo.setMessageId(messageId);
                //?????????
                sysMessageUserPo.setRecvUser(recvUserId);
                //????????????
                sysMessageUserPo.setRecvTime(new Date());
                //???????????????(1???????????????)
                sysMessageUserPo.setType(1);
                //?????????
                sysMessageUserPo.setDel(0);
                //?????????
                sysMessageUserPo.setCollect(0);
                //??????
                sysMessageUserPo.setRealStatus(0);
                //??????
                Integer messageUserId = leafService.getId();
                sysMessageUserPo.setId(messageUserId);
                sysMessageMapper.addSysMessageUser(sysMessageUserPo);
            }
        }
    }

    /**
     * ????????????
     *
     * @param calendarId ??????ID
     * @return
     */
    public PmCalendar getPmCalendar(Integer calendarId) {
        ApiResult<CalendarVo> calendarInfo = commCalendarService.getCalendarInfo(calendarId);
        if (!ObjectUtils.isEmpty(calendarInfo.getData()))
            return new PcCalendar(calendarInfo.getData());
        return null;
    }

    public PmCalendar getDefaultPmCalendar() {
        ApiResult<CalendarVo> calendarInfo = commCalendarService.getCalendarDefaultInfo();
        if (!ObjectUtils.isEmpty(calendarInfo.getData()))
            return new PcCalendar(calendarInfo.getData());
        return null;
    }

    /**
     * ????????????token
     *
     * @param neruServiceUrl
     * @param neruAppId
     * @param neruSecretKey
     * @return
     */
    private String getNeruSoftToken(String neruServiceUrl, String neruAppId, String neruSecretKey) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String>
                responseEntity = restTemplate.getForEntity(neruServiceUrl + "/gettoken?appid=" + neruAppId + "&secret=" + neruSecretKey, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        Map<String, Object> returnMap = jsonObject.toJavaObject(Map.class);
        String accessToken = "";//??????????????????????????????512??????
        if ("0.0".equals(String.valueOf(returnMap.get("errcode"))) || "0".equals(String.valueOf(returnMap.get("errcode")))) {
            accessToken = String.valueOf(returnMap.get("access_token"));
        } else {
            logger.error("??????????????????????????????token???????????????" + String.valueOf(returnMap.get("errmsg")));
        }
        return accessToken;
    }

    /**
     * ???????????????PDF?????????
     *
     * @param response ??????
     * @param pdfPath  ???????????????PDF??????
     */
    public void preViewPdf(HttpServletResponse response, String pdfPath) {
        if (StringHelper.isNullAndEmpty(pdfPath)) {
            throw new BaseException("?????????????????????pdf??????!");
        }

        File newFile = null;
        FileInputStream fileInputStream;
        try {
            //??????????????????
            newFile = new File(pdfPath);
            fileInputStream = new FileInputStream(pdfPath);
            // ??????????????????????????????????????????
            String fileName = URLEncoder.encode(newFile.getName(), "UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment;Filename=" + fileName);
            IOUtils.copy(fileInputStream, response.getOutputStream());
            response.getOutputStream().flush();
            response.getOutputStream().close();
            if (null != fileInputStream) {
                fileInputStream.close();
            }
        } catch (Exception e) {
            logger.info("???????????????????????????, ?????????{}", e);
            throw new BaseException("???????????????????????????!");
        } finally {
            if (!ObjectUtils.isEmpty(newFile)) {
                newFile.delete();
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param phoneNum
     */
    public String loginSafeInfoPlatform(String phoneNum) {
        PropertiesLoaderUtils prop = new PropertiesLoaderUtils("application.yml");
        String url = prop.getProperty("security.serviceUrl");
        String appKey = prop.getProperty("security.appKey");
        String secret = prop.getProperty("security.secret");

        JSONObject tokenJson = new JSONObject();
        tokenJson.put("appkey", appKey);
        tokenJson.put("tel", phoneNum);
        StringBuffer data = new StringBuffer();
        data.append(tokenJson.toString());
        data.append(secret);
        String token = Md5Util.StringInMd5Lower(data.toString());
        if (StringHelper.isNullAndEmpty(appKey) || StringHelper.isNullAndEmpty(secret) || StringHelper.isNullAndEmpty(token)) {
            logger.error("??????????????????????????????appKey???????????????secret?????????????????????token?????????appKey is {}, secret is {}, token is {}",
                    appKey, secret, token);
            return null;
        }

        return url + "&appkey=" + appKey + "&tel=" + phoneNum + "&token=" + token;
    }

    /**
     * ??????????????????id????????????????????????
     * @param form
     * @return
     */
    public Map<String, String> getActivitiCodeMap(WfRuningProcessForm form) {
        List<WfActivityVo> activities = form.getCandidate().getActivities();
        List<String> activitiIds = Lists.newArrayList();
        if(!ObjectUtils.isEmpty(activities)){
            for (WfActivityVo wfActivityVo:activities) {
                activitiIds.add(wfActivityVo.getId());
            }
        }
        //??????????????? ?????? ??????????????????
        return activitiTemplateService.queryActivitiByIds(activitiIds);
    }

    /**
     * ?????????????????????
     *
     * @param form      ????????????
     * @param sectionId ??????ID
     * @return
     */
    public List<WfActivityVo> filterFlowCandiateUser(WfRuningProcessForm form,Map<String, String> stringMap, Integer sectionId, Integer projectId) {
        Map<String,  List<GeneralVo>> activiti2Users = Maps.newHashMap();
        if (!ObjectUtils.isEmpty(stringMap)) {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                List<GeneralVo> userInfo = null;
                if ("company".equals(entry.getValue()) || "department".equals(entry.getValue())) {
                    //???????????????????????????????????????????????????
                    String userId = form.getUserId();
                    userInfo = this.getUserInfo(entry.getValue(), userId);
                } else {
                    if ("section".equals(entry.getValue()) && !ObjectUtils.isEmpty(sectionId)) {
                        userInfo = sysMessageMapper.queryTeamUsers(sectionId);
                    } else if ("project".equals(entry.getValue()) && !ObjectUtils.isEmpty(projectId)) { //TODO projectId
                        userInfo = sysMessageMapper.queryProjectUsers(projectId);
                    }
                }
                activiti2Users.put(entry.getKey(), userInfo);
            }
        }

        return getWfActivityVos(form, activiti2Users);
    }

    /**
     * ??????????????????????????????
     * @param form
     * @param activiti2Users
     * @return
     */
    private List<WfActivityVo> getWfActivityVos(WfRuningProcessForm form, Map<String, List<GeneralVo>> activiti2Users) {
        // Map<String, DictionaryVo> dictMap = commDictService.getDictMapByTypeCode("permission.allbidding");
        //???????????????????????????????????????????????????
        List<WfActivityVo> activityVoList = form.getCandidate().getActivities();
        if (ObjectUtils.isEmpty(activityVoList))
            return activityVoList;
        for (WfActivityVo wfActivityVo : activityVoList) {//??????????????????(??????????????????)
            if (!ObjectUtils.isEmpty(wfActivityVo.getCandidateGroups())) {//?????????????????????
                List<WfCandidateGroupVo> wfCandidateGroupVoList = wfActivityVo.getCandidateGroups();
                for (WfCandidateGroupVo wfCandidateGroupVo : wfCandidateGroupVoList) {
                    boolean isFilter = true;
                    //????????????????????? ?????? ???????????????????????????(????????????)?????????
                    // for (Map.Entry<String, DictionaryVo> entry : dictMap.entrySet()) {
                    //     if (wfCandidateGroupVo.getCode().trim().equals(entry.getKey().trim())) {//?????????,?????????
                    //         isFilter = false;
                    //         break;
                    //     }
                    // }
                    if (isFilter) {
                        //??????????????? candidateUsers
                        List<WfCandidateUserVo> candidateUsers = this.filterWfCandiateUserVos(wfCandidateGroupVo.getCandidateUsers(), activiti2Users.get(wfActivityVo.getId()));
                        wfCandidateGroupVo.setCandidateUsers(candidateUsers);
                    }

                }
            } else {//???????????????????????????
                List<WfCandidateUserVo> candidateUsers = this.filterWfCandiateUserVos(wfActivityVo.getCandidateUsers(), null);
                wfActivityVo.setCandidateUsers(candidateUsers);
            }
        }
        return activityVoList;
    }

    /**
     * ?????????????????????
     *
     * @param form      ????????????
     * @param sectionId ??????ID
     * @return
     */
    public List<WfActivityVo> filterFlowCandiateUser(WfRuningProcessForm form, Integer sectionId) {
        Map<String, String> stringMap = getActivitiCodeMap(form);
        Map<String,  List<GeneralVo>> activiti2Users = Maps.newHashMap();
        if (!ObjectUtils.isEmpty(stringMap)) {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                List<GeneralVo> userInfo = null;
                if ("company".equals(entry.getValue()) || "department".equals(entry.getValue())) {
                    //???????????????????????????????????????????????????
                    String userId = form.getUserId();
                    userInfo = this.getUserInfo(entry.getValue(), userId);
                } else {
                    if ("section".equals(entry.getValue()) && !ObjectUtils.isEmpty(sectionId)) {
                        userInfo = sysMessageMapper.queryTeamUsers(sectionId);
                    }
                }
                activiti2Users.put(entry.getKey(), userInfo);
            }
        }
        return getWfActivityVos(form, activiti2Users);
    }

    private List<GeneralVo> getUserInfo(String code, String userId) {
        List<GeneralVo> userVos;
        GeneralVo generalVo = sysMessageMapper.selectUserInfo(userId);
        List<GeneralVo> mainOrg = sysMessageMapper.selectUserMainOrg(userId);
        if (ObjectUtils.isEmpty(generalVo) || StringHelper.isNullAndEmpty(generalVo.getCode())) {
            //??????????????????????????????
            return null;
        } else if (generalVo.getCode().contains("UVU")) {
            //???????????? ?????????????????????????????????????????????
            userVos = sysMessageMapper.queryTeamUsersOutUser(mainOrg.get(0).getId());
        } else {
            //???????????? ????????????????????????  ????????????(?????????????????????????????????), ??????????????? ==??? ????????????????????? ???????????????
            if ("department".equals(code)) {
                userVos = sysMessageMapper.queryTeamUsersOutUser(mainOrg.get(0).getId());
            } else {
                //?????????????????? ???????????? ??????????????????????????????????????????
                List<GeneralVo> generalVos = sysMessageMapper.selectCompany(mainOrg.get(0).getId());
                if (!ObjectUtils.isEmpty(generalVos)) {
                    userVos = sysMessageMapper.queryCompanyInnerUsers(generalVos.get(0).getId());
                } else {
                    //???????????????????????? ?????????????????????
                    userVos = sysMessageMapper.queryTeamUsersOutUser(mainOrg.get(0).getId());
                }
            }
        }
        return userVos;
    }

    private List<WfCandidateUserVo> filterWfCandiateUserVos(List<WfCandidateUserVo> candidateUsers, List<GeneralVo> userInfo) {
        //????????????????????????????????????????????????
        List<WfCandidateUserVo> wfCandidateUserVoList = Lists.newArrayList();
        if (ObjectUtils.isEmpty(userInfo)) {
            return candidateUsers;
        }
        List<String> userCodes = ListUtil.toValueList(userInfo, "code", String.class, true);
        for (WfCandidateUserVo wfCandidateUserVo : candidateUsers) {
            if (ListUtil.toStr(userCodes).contains(wfCandidateUserVo.getCode()))
                wfCandidateUserVoList.add(wfCandidateUserVo);
        }
        return wfCandidateUserVoList;
    }

    public String getMenuName(String menuCode){
        return menuMapper.queryMenuNameByCode(menuCode);
    }

    public <T> void approveFlowAndSendMessage(String bizType, List<T> rawPos) {
        List<Integer> initiatorIds = ListUtil.toValueList(rawPos, "initiatorId", Integer.class);
        Map<Integer, UserVo> mapByUserIds = commUserService.getUserVoMapByUserIds(initiatorIds);

        String menuName = this.getMenuName(bizType);
        for (T po : rawPos) {
            Date initTime = null;
            Integer initiatorId = null;
            try {
                initTime = (Date)po.getClass().getMethod("getInitTime").invoke(po);
                initiatorId = (Integer) po.getClass().getMethod("getInitiatorId").invoke(po);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.info("?????????????????????");
                e.printStackTrace();
            }
            if(ObjectUtils.isEmpty(initTime) || ObjectUtils.isEmpty(initiatorId)){
                logger.info("??????????????????????????????????????????????????????????????????????????????{}", menuName);
                continue;
            }

            SysMessageAddForm sysMessageAddForm = new SysMessageAddForm();
            sysMessageAddForm.setTitle("????????????????????????-??????????????????");
            sysMessageAddForm.setContent("??????" + DateUtil.getDateFormat(initTime,DateUtil.DATE_CHINA_FORMAT) +
                    "?????????:" + menuName + "??????????????????????????????????????????????????????????????????");

            List<UserVo> recvUsers = Lists.newArrayList();
            if (!ObjectUtils.isEmpty(mapByUserIds) && !ObjectUtils.isEmpty(mapByUserIds.get(initiatorId))) {
                recvUsers.add(mapByUserIds.get(initiatorId));
                sysMessageAddForm.setRecvUser(recvUsers);
                sysMessageAddForm.setClaimDealTime(new Date());
                this.sendMessageRecv(sysMessageAddForm);
            }else {
                logger.info("????????????????????????????????????????????????");
            }
        }
    }
    public static void main(String[] args) {
        new SzxmCommonUtil().loginSafeInfoPlatform("18796819004");
    }


}
