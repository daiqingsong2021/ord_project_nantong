package com.wisdom.acm.szxm.service.sysscore.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wisdom.acm.szxm.common.StringHelper;
import com.wisdom.acm.szxm.common.SzxmCommonUtil;
import com.wisdom.acm.szxm.common.SzxmEnumsUtil;
import com.wisdom.acm.szxm.mapper.sysscore.SysScoreMapper;
import com.wisdom.acm.szxm.po.sysscore.ObjectScoreDetailPo;
import com.wisdom.acm.szxm.po.sysscore.ObjectTemplatePo;
import com.wisdom.acm.szxm.po.sysscore.SysScorePo;
import com.wisdom.acm.szxm.service.rygl.HolidayService;
import com.wisdom.acm.szxm.service.rygl.PeopleChangeService;
import com.wisdom.acm.szxm.service.rygl.PeopleEntryService;
import com.wisdom.acm.szxm.service.rygl.ProjInfoService;
import com.wisdom.acm.szxm.service.rygl.SpecialWorkerService;
import com.wisdom.acm.szxm.service.sysscore.ObjectScoreDetailService;
import com.wisdom.acm.szxm.service.sysscore.ObjectTemplateService;
import com.wisdom.acm.szxm.service.sysscore.SubjectScoreDetailService;
import com.wisdom.acm.szxm.service.sysscore.SysScoreService;
import com.wisdom.acm.szxm.service.wtgl.QuestionService;
import com.wisdom.acm.szxm.vo.sysscore.ObjectScoreItemVo;
import com.wisdom.acm.szxm.vo.sysscore.ObjectScoreVo;
import com.wisdom.acm.szxm.vo.sysscore.SubjectScoreVo;
import com.wisdom.acm.szxm.vo.sysscore.SysScoreVo;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.plan.project.CommPlanProjectService;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.vo.ProjectTeamVo;
import com.wisdom.base.common.vo.plan.project.PlanProjectVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * Author???wqd
 * Date???2019-12-17 16:19
 * Description???<??????>
 */
@Service
@Slf4j
/**
 * ?????????????????????
 */
public class SysScoreServiceImpl extends BaseService<SysScoreMapper, SysScorePo> implements SysScoreService {
    @Autowired
    private SzxmCommonUtil szxmCommonUtil;
    @Autowired
    private CommPlanProjectService commPlanProjectService;

    //????????????
    @Autowired
    private ObjectTemplateService objectTemplateService;
    //????????????
    @Autowired
    private ObjectScoreDetailService objectScoreDetailService;

    //????????????
    @Autowired
    private SubjectScoreDetailService subjectScoreDetailService;

    //????????????
    @Autowired
    private ProjInfoService projInfoService;
    @Autowired
    private PeopleEntryService peopleEntryService;
    @Autowired
    private PeopleChangeService peopleChangeService;
    @Autowired
    private SpecialWorkerService specialWorkerService;
    @Autowired
    private HolidayService holidayService;

    //??????
    @Autowired
    private QuestionService questionService;

    @Override
    public ObjectScoreVo getSysObjectScores(Map<String, Object> mapWhere) {
        List<ObjectScoreItemVo> objectScoreItemVos = objectTemplateService.selectMainItemObjectTemplates();
        if (ObjectUtils.isEmpty(objectScoreItemVos)) {
            log.error("??????????????????????????????");
            return null;
        }
        String projectId = StringHelper.formattString(mapWhere.get("projectId"));
        String sectionId = StringHelper.formattString(mapWhere.get("sectionId"));
        if (StringHelper.isNullAndEmpty(projectId) || StringHelper.isNullAndEmpty(sectionId)) {
            throw new BaseException("??????id?????????id????????????");
        }
        String year = StringHelper.formattString(mapWhere.get("year"));
        String month = StringHelper.formattString(mapWhere.get("month"));
        if (StringHelper.isNullAndEmpty(year) || StringHelper.isNullAndEmpty(month)) {
            throw new BaseException("???????????????????????????????????????");
        }

        Example example = new Example(ObjectScoreDetailPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("projectId", projectId);
        List<ObjectScoreDetailPo> scoreDetailPos = objectScoreDetailService.selectByExample(example);
        if (ObjectUtils.isEmpty(scoreDetailPos)) {
            log.info("??????????????????id???[{}],{}???{}???????????????????????????", sectionId, year, month);
            throw new BaseException("????????????" + year + "???" + month + "???????????????????????????");
        }
        Map<String, ObjectScoreDetailPo> detailPoMap = ListUtil.listToMap(scoreDetailPos, "itemCode", String.class);

        ObjectScoreVo objectScoreVo = getObjectScoreVo(objectScoreItemVos, detailPoMap);
        objectScoreVo.setSectionId(Integer.parseInt(sectionId));
        objectScoreVo.setProjectId(Integer.parseInt(projectId));
        objectScoreVo.setCreatTime(scoreDetailPos.get(0).getCreatTime());

        //????????????
        PlanProjectVo projectVo = commPlanProjectService.getProject(Integer.parseInt(projectId));
        objectScoreVo.setProjectName(projectVo.getName());
        //????????????
        Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
        ProjectTeamVo sectionVo = sectionMap.get(sectionId);
        if (!ObjectUtils.isEmpty(sectionVo)) {
            objectScoreVo.setSectionCode(sectionVo.getCode());
            objectScoreVo.setSectionName(sectionVo.getName());

            objectScoreVo.setSgdw("");
            if (!ObjectUtils.isEmpty(sectionVo.getCuList())) {
                objectScoreVo.setSgdw(sectionVo.getCuList().get(0).getName());
            }
        }
        return objectScoreVo;
    }

    @Override
    public PageInfo<SysScoreVo>  getSysScores(Map<String, Object> mapWhere, List<String> sectionList, Integer pageSize, Integer currentPageNum) {
        String projectId = StringHelper.formattString(mapWhere.get("projectId"));
        mapWhere.put("sectionList", sectionList);

        PageHelper.startPage(currentPageNum, pageSize);
        List<SysScoreVo> sysScoreVos = mapper.selectSysScoreList(mapWhere);
        //Collections.sort(sysScoreVos);
        PageInfo<SysScoreVo> pageInfo = new PageInfo<>(sysScoreVos);

        if (ObjectUtils.isEmpty(pageInfo.getList())) {
            log.info("??????????????????id???[{}],??????????????????", sectionList);
            return pageInfo;
        }
        getSysScoreVo(projectId, pageInfo.getList());
        return pageInfo;
    }

    private void getSysScoreVo(String projectId, List<SysScoreVo> sysScoreVos) {
        getSubjectScore(projectId, sysScoreVos);

        PlanProjectVo projectVo = commPlanProjectService.getProject(Integer.parseInt(projectId));
        Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
        for (SysScoreVo sysScoreVo : sysScoreVos) {
            //????????????
            sysScoreVo.setProjectName(projectVo.getName());
            //????????????
            ProjectTeamVo sectionVo = sectionMap.get(sysScoreVo.getSectionId());
            if (!ObjectUtils.isEmpty(sectionVo)) {
                sysScoreVo.setSectionCode(sectionVo.getCode());
                sysScoreVo.setSectionName(sectionVo.getName());

                sysScoreVo.setSgdw("");
                if (!ObjectUtils.isEmpty(sectionVo.getCuList())) {
                    sysScoreVo.setSgdw(sectionVo.getCuList().get(0).getName());
                }
            }
            //????????????
            sysScoreVo.getStatusVo().setName(SzxmEnumsUtil.StatusEnum.valueOf(sysScoreVo.getStatusVo().getCode()).getName());
        }
    }

    @Override
    public List<SysScoreVo> getAllSysScores(Map<String, Object> mapWhere, List<String> sectionList) {
        String projectId = StringHelper.formattString(mapWhere.get("projectId"));
        mapWhere.put("sectionList", sectionList);

        List<SysScoreVo> sysScoreVos = mapper.selectSysScoreList(mapWhere);
        // Collections.sort(sysScoreVos);
        if (ObjectUtils.isEmpty(sysScoreVos)) {
            log.info("??????????????????id???[{}],??????????????????", sectionList);
            return sysScoreVos;
        }
        getSysScoreVo(projectId, sysScoreVos);
        return sysScoreVos;
    }

    @Override
    public List<SysScoreVo> getFlowSysScores(Map<String, Object> mapWhere) {
        String[] idsArray = String.valueOf(mapWhere.get("ids")).split(",");
        List<String> ids = new ArrayList<String>(Arrays.asList(idsArray));
        mapWhere.put("ids", ids);

        List<SysScoreVo> sysScoreVos = mapper.selectSysScoreList(mapWhere);
        // Collections.sort(sysScoreVos); TODO
        if (ObjectUtils.isEmpty(sysScoreVos)) {
            log.info("????????????id???[{}]??????????????????", ids);
            throw new BaseException("??????????????????????????????");
        }
        Integer projectId = sysScoreVos.get(0).getProjectId();
        PlanProjectVo projectVo = commPlanProjectService.getProject(projectId);
        Map<Integer, ProjectTeamVo> sectionMap = szxmCommonUtil.getSectionMap(projectVo.getId());
        for (SysScoreVo sysScoreVo : sysScoreVos) {
            //????????????
            sysScoreVo.setProjectName(projectVo.getName());
            //????????????
            ProjectTeamVo sectionVo = sectionMap.get(sysScoreVo.getSectionId());
            if (!ObjectUtils.isEmpty(sectionVo)) {
                sysScoreVo.setSectionCode(sectionVo.getCode());
                sysScoreVo.setSectionName(sectionVo.getName());

                sysScoreVo.setSgdw("");
                if (!ObjectUtils.isEmpty(sectionVo.getCuList())) {
                    sysScoreVo.setSgdw(sectionVo.getCuList().get(0).getName());
                }
            }
            //????????????
            sysScoreVo.getStatusVo().setName(SzxmEnumsUtil.StatusEnum.valueOf(sysScoreVo.getStatusVo().getCode()).getName());
        }
        return sysScoreVos;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param projectId
     * @param sysScoreVos
     */
    private void getSubjectScore(String projectId, List<SysScoreVo> sysScoreVos) {
        for (SysScoreVo sysScoreVo : sysScoreVos) {
            // ?????????????????????????????????????????????
            if(!SzxmEnumsUtil.StatusEnum.INIT.getCode().equals(sysScoreVo.getStatusVo().getCode())){
                continue;
            }
            Integer sectionId = sysScoreVo.getSectionId();
            Integer year = sysScoreVo.getYear();
            Integer month = sysScoreVo.getMonth();
            //??????????????????
            Map<String, Object> subjectMap = Maps.newHashMap();
            subjectMap.put("projectId", projectId);
            subjectMap.put("sectionId", sectionId);
            subjectMap.put("year", year);
            subjectMap.put("month", month);
            SubjectScoreVo subjectScoreVo = subjectScoreDetailService.selectSubjectScore(subjectMap, 10, 1);
            BigDecimal subjectiveScore;
            if (!ObjectUtils.isEmpty(subjectScoreVo)) {
                subjectiveScore = subjectScoreVo.getActualScore();
            } else {
                subjectiveScore = new BigDecimal(80);
                log.info("{}???{}?????????id???{} ?????????????????????????????????80???", year, month, sectionId);
            }
            if (subjectiveScore.compareTo(sysScoreVo.getSubjectiveScore()) != 0) {
                sysScoreVo.setSubjectiveScore(subjectiveScore);
                BigDecimal totalScore = sysScoreVo.getObjectiveScore().multiply(new BigDecimal(0.7)).add(subjectiveScore.multiply(new BigDecimal(0.3)));
                totalScore = totalScore.setScale(1, BigDecimal.ROUND_HALF_UP);
                sysScoreVo.setTotalScore(totalScore);
                if (totalScore.compareTo(new BigDecimal(80)) == -1) {
                    sysScoreVo.setIsPass("1");
                }else{
                    sysScoreVo.setIsPass("0");
                }

                SysScorePo sysScorePo = new SysScorePo();
                sysScorePo.setId(sysScoreVo.getId());
                sysScorePo.setSubjectiveScore(subjectiveScore);
                sysScorePo.setTotalScore(totalScore);
                sysScorePo.setIsPass(sysScoreVo.getIsPass());
                super.updateSelectiveById(sysScorePo);
                log.info("?????????????????????id???{}??????????????????{}??????????????????{}", sysScoreVo.getId(), sysScoreVo.getSubjectiveScore(),
                        sysScoreVo.getTotalScore());
            }
        }
    }

    @Override
    public List<String> selectEvaluationSections(Integer projectId) {
        return mapper.selectEvaluationSections(projectId);
    }

    /**
     * ???????????????????????????
     *
     * @param objectScoreItemVoList ??????????????????
     * @param detailPoMap           ???????????????
     * @return
     */
    private ObjectScoreVo getObjectScoreVo(List<ObjectScoreItemVo> objectScoreItemVoList, Map<String, ObjectScoreDetailPo> detailPoMap) {
        List<ObjectScoreItemVo> scoreItemVos = Lists.newArrayList();
        //??????
        BigDecimal totalScore = new BigDecimal(0);
        //????????????
        BigDecimal actualScore = new BigDecimal(0);

        for (ObjectScoreItemVo itemVo : objectScoreItemVoList) {
            Integer maxScore = itemVo.getMaxScore();
            totalScore = totalScore.add(new BigDecimal(maxScore));
            List<ObjectScoreItemVo> objectDetailItemVos = objectTemplateService.selectDetailItemObjectTemplates(itemVo.getId());
            if (ObjectUtils.isEmpty(objectDetailItemVos)) {
                log.error("??????????????????{} ????????????????????????", itemVo.getCheckTitle());
                return null;
            }
            BigDecimal score = new BigDecimal(0);
            for (ObjectScoreItemVo objectScoreItemVo : objectDetailItemVos) {
                ObjectScoreDetailPo objectScoreDetailPo = detailPoMap.get(objectScoreItemVo.getItemCode());
                if (!ObjectUtils.isEmpty(objectScoreDetailPo)) {
                    score = score.add(objectScoreDetailPo.getDeduction());
                    objectScoreItemVo.setViolateCount(objectScoreDetailPo.getDeductionCount());
                    objectScoreItemVo.setScore(objectScoreDetailPo.getDeduction());
                }
            }
            ObjectTemplatePo templatePo = new ObjectTemplatePo();
            templatePo.setMaxScore(itemVo.getMaxScore());
            templatePo.setMinScore(itemVo.getMinScore());
            BigDecimal realScore = getRealScore(score, templatePo);
            actualScore = actualScore.add(realScore);
            itemVo.setScore(realScore);
            scoreItemVos.add(itemVo);
            scoreItemVos.addAll(objectDetailItemVos);
        }
        if (ObjectUtils.isEmpty(scoreItemVos)) {
            throw new BaseException("???????????????????????????");
        }

        ObjectScoreVo objectScoreVo = new ObjectScoreVo();
        objectScoreVo.setTotalScore(totalScore);
        objectScoreVo.setActualScore(actualScore);
        objectScoreVo.setScoreItemVos(scoreItemVos);
        return objectScoreVo;
    }

    @Override
    public void createSysScores(Integer projectId, String sectionId) {
        BigDecimal objectScore = new BigDecimal(0);
        BigDecimal peoManageScores = getPeoManageScores(projectId, sectionId);
        BigDecimal deviceManageScores = getDeviceManageScores(projectId, sectionId);
        BigDecimal materialManageScores = getMaterialManageScores(projectId, sectionId);
        BigDecimal planManageScores = getPlanManageScores(projectId, sectionId);
        BigDecimal qualityManageScores = getQualityManageScores(projectId, sectionId);
        BigDecimal securityManageScores = getSecurityManageScores(projectId, sectionId);
        BigDecimal problemManageScores = getProblemManageScores(projectId, sectionId);

        objectScore = objectScore.add(deviceManageScores);
        objectScore = objectScore.add(peoManageScores);
        objectScore = objectScore.add(materialManageScores);
        objectScore = objectScore.add(planManageScores);
        objectScore = objectScore.add(qualityManageScores);
        objectScore = objectScore.add(securityManageScores);
        objectScore = objectScore.add(problemManageScores);

        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;
        //??????????????????
        Map<String, Object> mapWhere = Maps.newHashMap();
        mapWhere.put("projectId", projectId);
        mapWhere.put("sectionId", sectionId);
        mapWhere.put("year", year);
        mapWhere.put("month", month);
        SubjectScoreVo subjectScoreVo = subjectScoreDetailService.selectSubjectScore(mapWhere, 10, 1);
        BigDecimal subjectiveScore;
        if (!ObjectUtils.isEmpty(subjectScoreVo)) {
            subjectiveScore = subjectScoreVo.getActualScore();
        } else {
            subjectiveScore = new BigDecimal(80);
            log.info("{}???{}?????????id???{} ?????????????????????????????????80???", year, month, sectionId);
        }

        BigDecimal totalScore;
        //????????? 0.7*????????? + 0.3 * ?????????
        totalScore = objectScore.multiply(new BigDecimal(0.7)).add(subjectiveScore.multiply(new BigDecimal(0.3)));

        Example example = new Example(SysScorePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        List<SysScorePo> sysScorePos = this.selectByExample(example);
        if (!ObjectUtils.isEmpty(sysScorePos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????", projectId, sectionId, year, month);
            return;
        }

        SysScorePo sysScorePo = new SysScorePo();
        sysScorePo.setProjectId(projectId);
        sysScorePo.setSectionId(Integer.parseInt(sectionId));
        sysScorePo.setYear(year);
        sysScorePo.setMonth(month);
        sysScorePo.setObjectiveScore(objectScore);
        sysScorePo.setSubjectiveScore(subjectiveScore);
        totalScore = totalScore.setScale(1, BigDecimal.ROUND_HALF_UP);
        sysScorePo.setTotalScore(totalScore);
        sysScorePo.setStatus(SzxmEnumsUtil.StatusEnum.INIT.getCode());
        log.info("??????id:[{}],??????id:[{}]???{}???{}???????????????????????????{}", projectId, sectionId, year, month, totalScore);
        // 0 ?????? 1 ?????????
        sysScorePo.setIsPass("0");
        //??????80????????????
        if (totalScore.compareTo(new BigDecimal(80)) == -1) {
            sysScorePo.setIsPass("1");
        }
        super.insert(sysScorePo);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getPeoManageScores(Integer projectId, String sectionId) {
        Integer projCount = projInfoService.queryProjInfoNotUpdate(sectionId);
        //????????????????????????????????? ??????????????????
        int projScore = ObjectUtils.isEmpty(projCount) ? 0 : 1;
        int peopleEntryCount = peopleEntryService.queryPeopleEntryCount(sectionId);
        int peopleChangeCount = peopleChangeService.queryPeopleChangeCount(sectionId);
        int specialWorkerCount = specialWorkerService.querySpecialWorkerCount(sectionId);
        int holidayCount = holidayService.queryHolidayCount(sectionId);

        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "RY%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();
        //??????????????????
        itemCodeCount.put("RYPROJ", projScore);
        //???????????????
        itemCodeCount.put("RYENTRY", peopleEntryCount);
        //????????????
        itemCodeCount.put("RYBG", peopleChangeCount);
        //????????????
        itemCodeCount.put("RYTSGZ", specialWorkerCount);
        //????????????
        itemCodeCount.put("RYQJ", holidayCount);

        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "RY%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }

        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("RY");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    private BigDecimal getRealScore(BigDecimal score, ObjectTemplatePo templatePo) {
        BigDecimal maxScore = new BigDecimal(templatePo.getMaxScore());
        BigDecimal minScore = new BigDecimal(templatePo.getMinScore());
        BigDecimal realScore = maxScore.subtract(score);
        if (realScore.compareTo(minScore) == -1) {
            realScore = minScore;
        }
        return realScore;
    }

    /**
     * ????????????????????????
     *
     * @param projectId
     * @param sectionId
     * @param year
     * @param month
     * @param objectTemplatePos
     * @param itemCodeCount
     */
    private BigDecimal insertObjectScore(Integer projectId, String sectionId, int year, int month,
                                         List<ObjectTemplatePo> objectTemplatePos, Map<String, Integer> itemCodeCount) {
        if (ObjectUtils.isEmpty(objectTemplatePos)) {
            log.info("??????????????????????????????");
            throw new BaseException("??????????????????????????????");
        }
        BigDecimal score = new BigDecimal(0);
        List<ObjectScoreDetailPo> scoreDetailPos = Lists.newArrayList();
        for (ObjectTemplatePo templatePo : objectTemplatePos) {
            ObjectScoreDetailPo objectScoreDetailPo = new ObjectScoreDetailPo();
            objectScoreDetailPo.setProjectId(projectId);
            objectScoreDetailPo.setSectionId(Integer.valueOf(sectionId));
            objectScoreDetailPo.setYear(year);
            objectScoreDetailPo.setMonth(month);
            for (Map.Entry<String, Integer> entry : itemCodeCount.entrySet()) {
                if (entry.getKey().equals(templatePo.getItemCode())) {
                    objectScoreDetailPo.setItemCode(entry.getKey());
                    objectScoreDetailPo.setDeductionCount(entry.getValue());
                    BigDecimal multiply = templatePo.getDeductionStandard().multiply(BigDecimal.valueOf(entry.getValue()));
                    score = score.add(multiply);
                    objectScoreDetailPo.setDeduction(multiply);
                    scoreDetailPos.add(objectScoreDetailPo);
                    break;
                }
            }
        }

        objectScoreDetailService.insert(scoreDetailPos);
        return score;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getDeviceManageScores(Integer projectId, String sectionId) {

        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "SB%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();

        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "SB%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("SB");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getMaterialManageScores(Integer projectId, String sectionId) {
        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "WL%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();
        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "WL%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);

        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("WL");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getPlanManageScores(Integer projectId, String sectionId) {
        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "JH%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();
        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "JH%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("JH");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getQualityManageScores(Integer projectId, String sectionId) {
        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "ZL%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();

        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "ZL%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("ZL");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getSecurityManageScores(Integer projectId, String sectionId) {
        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "AQ%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();
        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "AQ%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("AQ");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private BigDecimal getProblemManageScores(Integer projectId, String sectionId) {
        int questionCount = questionService.queryQuestionCount(sectionId);

        Calendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        int year = gregorianCalendar.get(Calendar.YEAR);
        //?????????0?????? ?????????0-11
        int month = gregorianCalendar.get(Calendar.MONTH) + 1;

        Example example = new Example(ObjectTemplatePo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLike("itemCode", "WT%");
        List<ObjectTemplatePo> objectTemplatePos = objectTemplateService.selectByExample(example);
        //???????????? --??? ??????
        Map<String, Integer> itemCodeCount = Maps.newHashMap();
        //????????????
        itemCodeCount.put("WTXY", questionCount);

        example = new Example(ObjectScoreDetailPo.class);
        criteria = example.createCriteria();
        criteria.andEqualTo("projectId", projectId);
        criteria.andEqualTo("sectionId", sectionId);
        criteria.andEqualTo("year", year);
        criteria.andEqualTo("month", month);
        criteria.andLike("itemCode", "WT%");
        List<ObjectScoreDetailPo> detailPos = objectScoreDetailService.selectByExample(example);
        if (!ObjectUtils.isEmpty(detailPos)) {
            log.error("??????id:[{}],??????id:[{}]???{}???{}????????????????????????????????????", projectId, sectionId, year, month);
            throw new BaseException("????????????????????????????????????");
        }
        BigDecimal score = insertObjectScore(projectId, sectionId, year, month, objectTemplatePos, itemCodeCount);
        Map<String, ObjectTemplatePo> templatePoMap = ListUtil.listToMap(objectTemplatePos, "itemCode", String.class);
        ObjectTemplatePo templatePo = templatePoMap.get("WT");
        BigDecimal realScore = getRealScore(score, templatePo);
        log.info("??????????????????????????????,?????????{}???{}???{}???", realScore, year, month);
        return realScore;
    }

    @Override
    public void approveQuaConceFlow(String bizType, List<Integer> ids) {
        //1 ??????????????????????????????????????????.
        SysScorePo sysScorePo = new SysScorePo();
        sysScorePo.setStatus(SzxmEnumsUtil.StatusEnum.APPROVED.getCode());
        this.updateSelectiveByIds(sysScorePo, ids);

        //????????????
        List<SysScorePo> sysScorePos = super.selectByIds(ids);
        szxmCommonUtil.approveFlowAndSendMessage(bizType, sysScorePos);
    }

    @Override
    public void deleteSysObjectScore(List<Integer> ids) {
        List<SysScorePo> sysScorePos = this.selectByIds(ids);
        if(!ObjectUtils.isEmpty(sysScorePos)){
            for (SysScorePo sysScorePo:sysScorePos) {
                Integer projectId = sysScorePo.getProjectId();
                Integer sectionId = sysScorePo.getSectionId();
                Integer year = sysScorePo.getYear();
                Integer month = sysScorePo.getMonth();
                if(ObjectUtils.isEmpty(projectId) || ObjectUtils.isEmpty(sectionId) || ObjectUtils.isEmpty(year) || ObjectUtils.isEmpty(month)){
                    log.info("??????????????????id??????{}???{}???{}?????????????????????????????????id?????????id???????????????????????????", sectionId, year, month);
                    continue;
                }
                objectScoreDetailService.deleteObjectDetailScore(projectId, sectionId, year, month);
                log.info("????????????id??????{}???{}???{}????????????????????????", sectionId, year, month);
            }
        }
        this.deleteByIds(ids);
        log.info("????????????id??????{}???????????????????????????", ids);
    }

    public static void main(String[] args) throws ParseException {
        Calendar gregorianCalendar = new GregorianCalendar();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        gregorianCalendar.setTime(simpleDateFormat.parse("2019-12-11"));
        int year = gregorianCalendar.get(Calendar.YEAR);
        int month = gregorianCalendar.get(Calendar.MONTH);

    }
}
