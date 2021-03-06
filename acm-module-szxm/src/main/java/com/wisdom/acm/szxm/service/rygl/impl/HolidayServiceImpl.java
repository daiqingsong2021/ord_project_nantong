package com.wisdom.acm.szxm.service.rygl.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.wisdom.acm.szxm.common.DateUtil;
import com.wisdom.acm.szxm.common.StringHelper;
import com.wisdom.acm.szxm.common.SzxmCommonUtil;
import com.wisdom.acm.szxm.common.SzxmEnumsUtil;
import com.wisdom.acm.szxm.common.officeUtils.DocTransformUtil;
import com.wisdom.acm.szxm.common.officeUtils.WordUtil;
import com.wisdom.acm.szxm.form.rygl.HolidayAddForm;
import com.wisdom.acm.szxm.form.rygl.HolidayUpdateForm;
import com.wisdom.acm.szxm.mapper.rygl.HolidayMapper;
import com.wisdom.acm.szxm.po.rygl.HolidayPo;
import com.wisdom.acm.szxm.po.rygl.KqConfigPo;
import com.wisdom.acm.szxm.po.rygl.PeoplePo;
import com.wisdom.acm.szxm.po.rygl.ProjInfoPo;
import com.wisdom.acm.szxm.service.rygl.HolidayService;
import com.wisdom.acm.szxm.service.rygl.KqConfigService;
import com.wisdom.acm.szxm.service.rygl.PeopleService;
import com.wisdom.acm.szxm.service.rygl.ProjInfoService;
import com.wisdom.acm.szxm.vo.rygl.HolidayVo;
import com.wisdom.acm.szxm.vo.rygl.LwryHolidayVo;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.feign.CommOrgService;
import com.wisdom.base.common.feign.CommProjectTeamService;
import com.wisdom.base.common.feign.CommUserService;
import com.wisdom.base.common.feign.plan.project.CommPlanProjectService;
import com.wisdom.base.common.feign.sys.CommRoleService;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.ResourceUtil;
import com.wisdom.base.common.util.calc.calendar.CalendarUtil;
import com.wisdom.base.common.util.calc.calendar.PmCalendar;
import com.wisdom.base.common.vo.DictionaryVo;
import com.wisdom.base.common.vo.GeneralVo;
import com.wisdom.base.common.vo.ProjectTeamVo;
import com.wisdom.base.common.vo.RoleVo;
import com.wisdom.base.common.vo.UserInfo;
import com.wisdom.base.common.vo.plan.project.PlanProjectVo;
import com.wisdom.webservice.contract.ContractServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.mapper.entity.Example;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HolidayServiceImpl extends BaseService<HolidayMapper, HolidayPo> implements HolidayService {

    @Autowired
    private CommProjectTeamService commProjectTeamService;

    @Autowired
    private SzxmCommonUtil szxmCommonUtil;

    @Autowired
    private DocTransformUtil docTransformUtil;

    @Autowired
    private KqConfigService kqConfigService;

    @Autowired
    private CommPlanProjectService commPlanProjectService;


    @Autowired
    private CommDictService commDictService;

    @Autowired
    private CommUserService commUserService;

    @Autowired
    private PeopleService peopleService;

    @Autowired
    private CommRoleService commRoleService;

    @Autowired
    private CommOrgService commOrgService;

    @Autowired
    private ProjInfoService projInfoService;

    @Autowired
    private HttpServletRequest request;

    @Override
    public PageInfo<HolidayVo> selectHolidayList(Map<String, Object> mapWhere, List<String> sectionList,
                                                 Integer pageSize, Integer currentPageNum) {
        mapWhere.put("sectionList", sectionList);
        mapWhere.put("searcher", StringHelper.formattString(String.valueOf(mapWhere.get("searcher"))));
        PageHelper.startPage(currentPageNum, pageSize);
        List<HolidayVo> holidayVoList = mapper.selectHolidayList(mapWhere);
        PageInfo<HolidayVo> pageInfo = new PageInfo<HolidayVo>(holidayVoList);

        if (!ObjectUtils.isEmpty(pageInfo.getList())) {
            PlanProjectVo projectVo = commPlanProjectService.getProject(Integer.valueOf(String.valueOf(mapWhere.get("projectId"))));
            Map<String, DictionaryVo> holiDictMap = commDictService.getDictMapByTypeCode("szxm.rygl.holitype");
            Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
            for (HolidayVo holidayVo : pageInfo.getList()) {
                holidayVo.getTypeVo().setName(szxmCommonUtil.getDictionaryName(holiDictMap, holidayVo.getTypeVo().getCode()));
                ProjectTeamVo sectionVo = sectionMap.get(holidayVo.getSectionId());
                holidayVo.setSectionCode(sectionVo.getCode());
                holidayVo.setSectionName(sectionVo.getName());
                holidayVo.setProjectName(projectVo.getName());

                //????????????
                holidayVo.getStatusVo().setName(SzxmEnumsUtil.StatusEnum.valueOf(holidayVo.getStatusVo().getCode()).getName());
            }
        }
        return pageInfo;
    }


    @Override
    public List<HolidayVo> selectFlowHolidayList(Map<String, Object> mapWhere, List<String> sectionList) {
        if (StringHelper.isNotNullAndEmpty(String.valueOf(mapWhere.get("ids")))) {//????????????Ids ?????????
            String[] idsArray = String.valueOf(mapWhere.get("ids")).split(",");
            List<String> ids = new ArrayList<String>(Arrays.asList(idsArray));
            mapWhere.put("ids", ids);
        } else {
            mapWhere.put("sectionList", sectionList);
            mapWhere.put("searcher", StringHelper.formattString(String.valueOf(mapWhere.get("searcher"))));
        }
        List<HolidayVo> holidayVoList = mapper.selectHolidayList(mapWhere);
        if (!ObjectUtils.isEmpty(holidayVoList)) {
            PlanProjectVo projectVo = commPlanProjectService.getProject(holidayVoList.get(0).getProjectId());
            Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
            Map<String, DictionaryVo> holiDictMap = commDictService.getDictMapByTypeCode("szxm.rygl.holitype");
            for (HolidayVo holidayVo : holidayVoList) {
                holidayVo.getTypeVo().setName(szxmCommonUtil.getDictionaryName(holiDictMap, holidayVo.getTypeVo().getCode()));
                ProjectTeamVo sectionVo = sectionMap.get(holidayVo.getSectionId());
                holidayVo.setSectionCode(sectionVo.getCode());
                holidayVo.setSectionName(sectionVo.getName());
                holidayVo.setProjectName(projectVo.getName());

                //????????????
                holidayVo.getStatusVo().setName(SzxmEnumsUtil.StatusEnum.valueOf(holidayVo.getStatusVo().getCode()).getName());
            }
        }
        return holidayVoList;
    }

    @Override
    public List<HolidayVo> selectInOneDate(Map<String, Object> holiQueryMap) {
        return mapper.selectInOneDate(holiQueryMap);
    }

    @Override
    public void approveHolidayFlow(String bizType, List<Integer> ids) {
        //1 ??????????????????????????????????????????.
        HolidayPo updatePo = new HolidayPo();
        updatePo.setStatus(SzxmEnumsUtil.StatusEnum.APPROVED.getCode());
        this.updateSelectiveByIds(updatePo, ids);
        //??????????????????


        //????????????
        List<HolidayPo> holidayPos = super.selectByIds(ids);
        szxmCommonUtil.approveFlowAndSendMessage(bizType, holidayPos);
    }

    @Override
    public String getHolidayWord(Integer id) {
        HolidayPo holidayPo = mapper.selectByPrimaryKey(id);
        PeoplePo peoplePo = peopleService.selectById(holidayPo.getPeopleId());
        PeoplePo jqPerson = peopleService.selectById(holidayPo.getAgentId());
        ProjInfoPo projInfoPo = projInfoService.selectById(peoplePo.getProjInfoId());
        String orgType = projInfoPo.getOrgType();

        InputStream fileInputStrem = null;
        // ????????????3 ??? ???????????? ?????????????????????  2 ??? ???????????? ?????? ???????????????
        if ("3".equals(orgType)) {
            fileInputStrem = ResourceUtil.findResoureFile("template/jianlbryqj.docx");
        } else if ("2".equals(orgType)) {
            fileInputStrem = ResourceUtil.findResoureFile("template/jinglbryqj.docx");
        }
        if (null == fileInputStrem) {
            throw new BaseException("????????????????????????");
        }

        ProjectTeamVo projectTeamVo = commProjectTeamService.getProjectTeamById(holidayPo.getSectionId());
        String segCode = projectTeamVo.getCode();
        String hth = ContractServiceUtils.getInstance().getContractNo(segCode);

        String cbdw = "";
        if (!ObjectUtils.isEmpty(projectTeamVo.getCuList())) {//???????????????????????????
            for (GeneralVo cdbwVo : projectTeamVo.getCuList()) {
                cbdw += cdbwVo.getName() + ",";
            }
        }
        if (StringHelper.isNotNullAndEmpty(cbdw)) {
            cbdw = cbdw.substring(0, cbdw.length() - 1);
        }

        Map<String, Object> params = getStringObjectMap(holidayPo, jqPerson, projectTeamVo, hth, cbdw);

        File newFile = null;
        try {
            WordUtil wordUtil = new WordUtil();
            //??????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            File tempFlolder = new File(tempFlolderStr);
            if (!tempFlolder.isDirectory()) {
                tempFlolder.mkdirs();
            }
            String fileName = "/qjsq" + System.currentTimeMillis() + ".docx";
            String outFileName = tempFlolderStr + fileName;
            wordUtil.generateWord(fileInputStrem, params, outFileName);

            //??????????????????
            newFile = new File(outFileName);
            FileInputStream fileInputStream = new FileInputStream(outFileName);
            String pdfName = tempFlolder + "/qjsq" + System.currentTimeMillis() + ".pdf";
            docTransformUtil.docToPDF(fileInputStream, pdfName);
            return pdfName;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            newFile.delete();
        }
        return null;
    }

    @Override
    public int queryHolidayCount(String sectionId) {
        Integer count = mapper.queryHolidayCount(sectionId);
        return ObjectUtils.isEmpty(count) ? 0 : count;
    }

    /**
     * ?????????????????????????????????
     *
     * @param holidayPo     holidayPo
     * @param jqPerson      ???????????????????????????
     * @param projectTeamVo projectTeamVo
     * @param hth           ?????????
     * @param cbdw          ????????????
     * @return
     */
    private Map<String, Object> getStringObjectMap(HolidayPo holidayPo, PeoplePo jqPerson, ProjectTeamVo projectTeamVo, String hth, String cbdw) {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> styleMap = new HashMap<String, Object>();
        styleMap.put("underline", UnderlinePatterns.SINGLE);

        params.put("cbdw", cbdw);
        params.put("cbdw_style", styleMap);

        String jldw = "";
        if (!ObjectUtils.isEmpty(projectTeamVo.getCcuList())) {//???????????????????????????
            for (GeneralVo jlbwVo : projectTeamVo.getCcuList()) {
                jldw += jlbwVo.getName() + ",";
            }
        }
        if (StringHelper.isNotNullAndEmpty(jldw)) {
            jldw = jldw.substring(0, jldw.length() - 1);
        }
        params.put("jldw", jldw);
        params.put("jldw_style", styleMap);
        params.put("hth", hth);
        params.put("hth_style", styleMap);
        params.put("bh", StringHelper.formattString(holidayPo.getSerialId()));
        params.put("bh_style", styleMap);
        params.put("jsdw", "??????????????????");
        params.put("jsdw_style", styleMap);
        params.put("reason", holidayPo.getReason());
        params.put("reason_style", styleMap);
        params.put("startday", DateUtil.getDateFormat(holidayPo.getStartTime()));
        params.put("startday_style", styleMap);
        params.put("endday", DateUtil.getDateFormat(holidayPo.getEndTime()));
        params.put("endday_style", styleMap);
        params.put("other", jqPerson.getName());
        params.put("other_style", styleMap);
        params.put("person", holidayPo.getPeopleName());
        params.put("person_style", styleMap);
        params.put("job", holidayPo.getRyZw());
        params.put("job_style", styleMap);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(holidayPo.getCreatTime());
        // ???????????????int ?????? ????????????????????????
        params.put("year", calendar.get(Calendar.YEAR) + "");
        params.put("year_style", styleMap);
        params.put("month", calendar.get(Calendar.MONTH) + "");
        params.put("month_style", styleMap);
        params.put("day", calendar.get(Calendar.DATE) + "");
        params.put("day_style", styleMap);
        return params;
    }

    @Override
    public HolidayVo addHoliday(HolidayAddForm holidayAddForm) {
        UserInfo userInfo = commUserService.getLoginUser();
        Integer bbmId = commOrgService.getUserOrgInfo(userInfo.getId()).getId();//???????????????????????????ID
        List<RoleVo> roleVos = commRoleService.queryRoleList(bbmId, userInfo.getId());
        String roleCode = ListUtil.listToNames(roleVos, "code");
        //????????????????????????????????????????????????????????????????????????????????????????????????
        if (roleCode.indexOf("YZ_KQGLY") <= -1) {//???????????????????????????
            if(StringUtils.equalsIgnoreCase(holidayAddForm.getPeopleType(),"lwry")){
                throw new BaseException("??????????????????????????????????????????????????????????????????????????????????????????");
            }
            //??????peopleId ??????????????? ????????????????????????ID.???????????????????????????????????????????????????????????????????????????????????????????????????????????????
            Example example = new Example(PeoplePo.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("telPhone", userInfo.getPhone());//???????????????
            criteria.andEqualTo("sectionId", holidayAddForm.getSectionId());//??????ID
            criteria.andEqualTo("status", "1");//???????????????
            PeoplePo exisPeoplePo = peopleService.selectOneByExample(example);
            if (ObjectUtils.isEmpty(exisPeoplePo)) {//???????????????????????????????????????????????????
                throw new BaseException("???????????????????????????????????????????????????");
            }
            holidayAddForm.setPeopleId(exisPeoplePo.getId());//??????????????????ID

            if (StringHelper.isNullAndEmpty(holidayAddForm.getRyZw())) {
                String jobName = szxmCommonUtil.getDictionaryName("base.position.type", exisPeoplePo.getJob());
                holidayAddForm.setRyZw(jobName);
            }
        }

        //??????????????????????????????????????????????????????????????????????????????????????????????????????
        Map<String, Object> mapWhere = Maps.newHashMap();
        mapWhere = Maps.newHashMap();
        mapWhere.put("peopleId", holidayAddForm.getPeopleId());
        mapWhere.put("startTime", DateUtil.getDateFormat(holidayAddForm.getStartTime()));
        mapWhere.put("endTime", DateUtil.getDateFormat(holidayAddForm.getEndTime()));
        List<HolidayVo> holidayVoList = mapper.queryHisTory(mapWhere);
        if (!ObjectUtils.isEmpty(holidayVoList) && holidayVoList.size() > 0) {
            throw new BaseException("???????????????????????????????????????????????????");
        }
        HolidayPo holidayPo = dozerMapper.map(holidayAddForm, HolidayPo.class);
        holidayPo.setStatus(SzxmEnumsUtil.StatusEnum.INIT.toString());
        holidayPo.setSerialId("QJ" + DateUtil.getDateFormat(new Date(), "yyyyMMddHHmmss"));
        if(StringUtils.isBlank(holidayAddForm.getPeopleType())){
            holidayPo.setRyType("glry");
        }else{
            holidayPo.setRyType(StringUtils.lowerCase(holidayAddForm.getPeopleType()));
            holidayPo.setLastUpdUser(userInfo.getId());
            holidayPo.setInitiatorId(userInfo.getId());
            holidayPo.setInitiator(userInfo.getActuName());
            holidayPo.setInitTime(new Date());
        }
        super.insert(holidayPo);
        return selectByHolidayId(holidayPo.getId());
    }

    @Override
    public void deleteHoliday(List<Integer> ids) {
        this.deleteByIds(ids);
    }

    @Override
    public HolidayVo updateHoliday(HolidayUpdateForm holidayUpdateForm) {
        HolidayPo updateConfigPo = dozerMapper.map(holidayUpdateForm, HolidayPo.class);
        //?????????????????????????????????????????????????????????????????????????????????
        Map<String, Object> mapWhere = new HashMap<>();
        mapWhere.put("peopleId", holidayUpdateForm.getPeopleId());
        mapWhere.put("startTime", DateUtil.getDateFormat(holidayUpdateForm.getStartTime()));
        mapWhere.put("endTime", DateUtil.getDateFormat(holidayUpdateForm.getEndTime()));
        List<HolidayVo> holidayVoList = mapper.queryHisTory(mapWhere);
        //?????????????????????????????????????????????
        holidayVoList = holidayVoList.stream().filter(item->item.getId().intValue() != holidayUpdateForm.getId().intValue()).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(holidayVoList) && holidayVoList.size() > 0) {
            throw new BaseException("???????????????????????????????????????????????????");
        }
        super.updateSelectiveById(updateConfigPo);//??????ID??????po?????????null??????????????????????????????null??????
        return selectByHolidayId(updateConfigPo.getId());
    }

    @Override
    public HolidayVo selectByHolidayId(Integer id) {
        Map<String, Object> mapWhere = Maps.newHashMap();
        mapWhere.put("id", id);
        List<HolidayVo> holidayVoList = mapper.selectHolidayList(mapWhere);
        HolidayVo holidayVo = holidayVoList.get(0);
        ProjectTeamVo sectionVo = commProjectTeamService.getProjectTeamById(holidayVo.getSectionId());
        holidayVo.setSectionCode(sectionVo.getCode());
        holidayVo.setSectionName(sectionVo.getName());
        PlanProjectVo projectVo = commPlanProjectService.getProject(holidayVo.getProjectId());
        holidayVo.setProjectName(projectVo.getName());

        holidayVo.getTypeVo().setName(szxmCommonUtil.getDictionaryName("szxm.rygl.holitype", holidayVo.getTypeVo().getCode()));
        holidayVo.getStatusVo().setName(SzxmEnumsUtil.StatusEnum.valueOf(holidayVo.getStatusVo().getCode()).getName());
        return holidayVo;
    }

    /**
     * ????????????ID,??????ID?????????????????????????????? ???????????????????????????????????????,??????????????????????????????
     *
     * @param projectId
     * @param sectionId
     * @param startTime
     * @param endTime
     * @return
     */
    @Override
    public int getDaysBySIdAndTime(Integer projectId, Integer sectionId, Date startTime, Date endTime) {
        Example example = new Example(KqConfigPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("type", "1");
        KqConfigPo SectionKqConfig = kqConfigService.selectOneByExample(example);
        Integer calenderId;
        if (ObjectUtils.isEmpty(SectionKqConfig)) {//??????????????????????????????
            example.clear();
            criteria = example.createCriteria();
            criteria.andEqualTo("projectId", projectId);
            criteria.andEqualTo("type", "0");
            KqConfigPo allKqConfig = kqConfigService.selectOneByExample(example);
            if (ObjectUtils.isEmpty(allKqConfig)) {//????????????????????? ??????????????????ID
                calenderId = Integer.valueOf(szxmCommonUtil.getDefaultPmCalendar().getId());
            } else calenderId = allKqConfig.getCalenderId();
        } else {
            calenderId = SectionKqConfig.getCalenderId();
        }
        //????????????ID??????????????????????????????days+1
        int days = 0;
        PmCalendar pmCalendar = szxmCommonUtil.getPmCalendar(calenderId);

        List<Date> dateList = DateUtil.getEveryDay(startTime, endTime);
        for (Date date : dateList) {
            boolean isWorkDay = CalendarUtil.isWorkDate(date, pmCalendar);//?????????????????????
            if (isWorkDay) days++;
        }
        return days;
    }

    /**
     * ????????????
     *
     * @param fileSystemResource
     * @return
     */
    public Map<String, Object> uploadFile(FileSystemResource fileSystemResource) {
        String token = request.getHeader("Authorization");//????????????Token
        String gateWayHost = request.getHeader("gateway_host");//????????????????????????

        RestTemplate restTemplate = new RestTemplate();
        // ????????????
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        headers.setContentType(type);
        headers.set("Authorization", token);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileSystemResource);
        form.add("filename", fileSystemResource.getFilename());
        HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);
        Map<String, Object> returnMap = restTemplate.postForObject("http://" + gateWayHost + "/api/doc/file/upload", files, Map.class);

        return returnMap;
    }

    @Override
    public List<HolidayVo> queryLwryHolidayRecord(Map<String, Object> mapWhere) {
        return mapper.queryLwryHolidayRecord(mapWhere);
    }

    @Override
    public PageInfo<LwryHolidayVo> selectSectionLwryHolidayList(Map<String, Object> mapWhere, Integer pageSize,
                                                                Integer currentPageNum) {
        PageHelper.startPage(currentPageNum, pageSize);
        List<LwryHolidayVo> holidayVoList = mapper.selectSectionLwryHolidayList(mapWhere);
        PageInfo<LwryHolidayVo> pageInfo = new PageInfo<LwryHolidayVo>(holidayVoList);

        if (!ObjectUtils.isEmpty(pageInfo.getList())) {
            PlanProjectVo projectVo = commPlanProjectService.getProject(Integer.valueOf(String.valueOf(mapWhere.get("projectId"))));
            Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
            for (LwryHolidayVo holidayVo : pageInfo.getList()) {
                ProjectTeamVo sectionVo = sectionMap.get(holidayVo.getSectionId());
                holidayVo.setSectionCode(sectionVo.getCode());
                holidayVo.setSectionName(sectionVo.getName());
                holidayVo.setProjectName(projectVo.getName());
                holidayVo.setStatusDesc(StringUtils.equalsIgnoreCase(holidayVo.getStatus(),"APPROVED")?"?????????":"??????");
            }
        }
        return pageInfo;
    }
}
