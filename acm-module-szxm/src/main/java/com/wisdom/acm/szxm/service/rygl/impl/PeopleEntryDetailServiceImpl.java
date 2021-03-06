package com.wisdom.acm.szxm.service.rygl.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wisdom.acm.szxm.common.DateUtil;
import com.wisdom.acm.szxm.common.StringHelper;
import com.wisdom.acm.szxm.common.SzxmCommonUtil;
import com.wisdom.acm.szxm.common.UUIDHexGenerator;
import com.wisdom.acm.szxm.common.officeUtils.ExcelError;
import com.wisdom.acm.szxm.common.officeUtils.ExcelUtil;
import com.wisdom.acm.szxm.common.redisUtils.RedisUtil;
import com.wisdom.acm.szxm.form.rygl.PeopleEntryDetailAddForm;
import com.wisdom.acm.szxm.form.rygl.PeopleEntryDetailUpdateForm;
import com.wisdom.acm.szxm.mapper.rygl.PeopleEntryDetailMapper;
import com.wisdom.acm.szxm.po.rygl.PeopleEntryDetailPo;
import com.wisdom.acm.szxm.po.rygl.PeoplePo;
import com.wisdom.acm.szxm.service.rygl.PeopleEntryDetailService;
import com.wisdom.acm.szxm.service.rygl.PeopleEntryService;
import com.wisdom.acm.szxm.service.rygl.PeopleService;
import com.wisdom.acm.szxm.vo.rygl.PeopleEntryDetailVo;
import com.wisdom.acm.szxm.vo.rygl.PeopleEntryVo;
import com.wisdom.acm.szxm.vo.rygl.PeopleVo;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.vo.DictionaryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PeopleEntryDetailServiceImpl
        extends BaseService<PeopleEntryDetailMapper, PeopleEntryDetailPo> implements PeopleEntryDetailService {
    @Autowired
    private SzxmCommonUtil szxmCommonUtil;

    @Autowired
    private CommDictService commDictService;

    @Autowired
    private PeopleEntryService peopleEntryService;

    @Autowired
    private PeopleService peopleService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    protected org.dozer.Mapper dozerMapper;

    @Override
    public PageInfo<PeopleEntryDetailVo> selectPeopleEntryDetailList(Map<String, Object> mapWhere, Integer entryId,
                                                                     Integer pageSize, Integer currentPageNum) {
        mapWhere.put("entryId", StringHelper.formattString(String.valueOf(entryId)));
        mapWhere.put("searcher", StringHelper.formattString(String.valueOf(mapWhere.get("searcher"))));
        PageHelper.startPage(currentPageNum, pageSize);
        List<PeopleEntryDetailVo> peopleEntryDetailVoList = mapper.selectPeopleEntryDetail(mapWhere);
        PageInfo<PeopleEntryDetailVo> pageInfo = new PageInfo<PeopleEntryDetailVo>(peopleEntryDetailVoList);
        if (!ObjectUtils.isEmpty(pageInfo.getList())) {
            Map<String, DictionaryVo> psotionDictMap = commDictService.getDictMapByTypeCode("base.position.type");
            for (PeopleEntryDetailVo peopleEntryDetailVo : pageInfo.getList()) {
                peopleEntryDetailVo.getJobVo().setName(szxmCommonUtil.getDictionaryName(psotionDictMap, peopleEntryDetailVo.getJobVo().getCode()));
                peopleEntryDetailVo.setAge(szxmCommonUtil.getAge(peopleEntryDetailVo.getBornDate()));
            }
        }

        return pageInfo;
    }

    @Override
    public PeopleEntryDetailVo addPeopleEntryDetail(PeopleEntryDetailAddForm peopleEntryDetailAddForm) {
        if (ObjectUtils.isEmpty(peopleEntryDetailAddForm.getClassHour()) || ObjectUtils.isEmpty(peopleEntryDetailAddForm.getScore())) {
            throw new BaseException("???????????????????????????????????????");
        }
        List<PeopleVo> peopleVos = peopleService.selectPeopleByIDCard(peopleEntryDetailAddForm.getIdCard());
        if (!ObjectUtils.isEmpty(peopleVos)) {
            throw new BaseException("????????????"+peopleVos.get(0).getOrgName()+"????????????????????????????????????!");
        }
        Map<String, Object> mapWhere = Maps.newHashMap();
        mapWhere.put("idCard", peopleEntryDetailAddForm.getIdCard());
        List<PeopleEntryDetailVo> peopleEntryDetailVos = mapper.selectEntryDetail(mapWhere);

        if (!ObjectUtils.isEmpty(peopleEntryDetailVos) && "0".equals(peopleEntryDetailVos.get(0).getEntryType())) {
            throw new BaseException("???????????????????????????????????????" + peopleEntryDetailVos.get(0).getOrgName() + "?????????????????????!");
        }

        PeopleEntryDetailPo peopleEntryDetailPo = dozerMapper.map(peopleEntryDetailAddForm, PeopleEntryDetailPo.class);
        if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getType()))
            peopleEntryDetailPo.setType("2");
        if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getPeoType()))
            peopleEntryDetailPo.setPeoType("1");
        if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getSex()))
            peopleEntryDetailPo.setSex("1");
        if (ObjectUtils.isEmpty(peopleEntryDetailPo.getClassHour()))
            peopleEntryDetailPo.setClassHour(BigDecimal.valueOf(0));
        super.insert(peopleEntryDetailPo);
        PeopleEntryDetailVo peopleEntryDetailVo =
                dozerMapper.map(peopleEntryDetailPo, PeopleEntryDetailVo.class);//po???????????????Vo??????
        peopleEntryDetailVo.getTypeVo().setCode(peopleEntryDetailPo.getType());
        peopleEntryDetailVo.getTypeVo().setName(szxmCommonUtil.getDictionaryName("szxm.rygl.peopleType", peopleEntryDetailPo.getType()));


        peopleEntryDetailVo.getPeoTypeVo().setCode(peopleEntryDetailPo.getPeoType());
        if ("0".equals(peopleEntryDetailPo.getPeoType()))
            peopleEntryDetailVo.getPeoTypeVo().setName("??????");
        else if ("1".equals(peopleEntryDetailPo.getType()))
            peopleEntryDetailVo.getPeoTypeVo().setName("??????");
        peopleEntryDetailVo.getSexVo().setCode(peopleEntryDetailPo.getSex());
        if ("0".equals(peopleEntryDetailPo.getSex()))
            peopleEntryDetailVo.getSexVo().setName("???");
        else if ("1".equals(peopleEntryDetailPo.getSex()))
            peopleEntryDetailVo.getSexVo().setName("???");
        peopleEntryDetailVo.getJobVo().setCode(peopleEntryDetailPo.getJob());
        peopleEntryDetailVo.getJobVo().setName(szxmCommonUtil.getDictionaryName("base.position.type", peopleEntryDetailPo.getJob()));
        peopleEntryDetailVo.setAge(szxmCommonUtil.getAge(peopleEntryDetailPo.getBornDate()));
        return peopleEntryDetailVo;
    }

    @Override
    public PeopleEntryDetailVo updatePeopleEntryDetail(PeopleEntryDetailUpdateForm peopleEntryDetailUpdateForm) {
        Example example = new Example(PeopleEntryDetailPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("enTryId", peopleEntryDetailUpdateForm.getEnTryId());
        criteria.andEqualTo("idCard", peopleEntryDetailUpdateForm.getIdCard());
        criteria.andNotEqualTo("id", peopleEntryDetailUpdateForm.getId());
        PeopleEntryDetailPo existPo = this.selectOneByExample(example);
        if (!ObjectUtils.isEmpty(existPo))
            throw new BaseException("????????????????????????????????????????????????!");

        PeopleEntryDetailPo peopleEntryDetailUpdatePo =
                dozerMapper.map(peopleEntryDetailUpdateForm, PeopleEntryDetailPo.class);
        super.updateSelectiveById(peopleEntryDetailUpdatePo);//??????ID??????po?????????null??????????????????????????????null??????

        PeopleEntryDetailPo peopleEntryDetailPo = this.selectById(peopleEntryDetailUpdatePo.getId());//?????????????????????
        PeopleEntryDetailVo peopleEntryDetailVo =
                dozerMapper.map(peopleEntryDetailPo, PeopleEntryDetailVo.class);//po???????????????Vo??????
        peopleEntryDetailVo.getTypeVo().setCode(peopleEntryDetailPo.getType());
        if ("0".equals(peopleEntryDetailPo.getType()))
            peopleEntryDetailVo.getTypeVo().setName("????????????");
        else if ("1".equals(peopleEntryDetailPo.getType()))
            peopleEntryDetailVo.getTypeVo().setName("????????????");
        else if ("2".equals(peopleEntryDetailPo.getType()))
            peopleEntryDetailVo.getTypeVo().setName("????????????");

        peopleEntryDetailVo.getPeoTypeVo().setCode(peopleEntryDetailPo.getPeoType());
        if ("0".equals(peopleEntryDetailPo.getPeoType()))
            peopleEntryDetailVo.getPeoTypeVo().setName("??????");
        else if ("1".equals(peopleEntryDetailPo.getType()))
            peopleEntryDetailVo.getPeoTypeVo().setName("??????");
        peopleEntryDetailVo.getSexVo().setCode(peopleEntryDetailPo.getSex());
        if ("0".equals(peopleEntryDetailPo.getSex()))
            peopleEntryDetailVo.getSexVo().setName("???");
        else if ("1".equals(peopleEntryDetailPo.getSex()))
            peopleEntryDetailVo.getSexVo().setName("???");
        peopleEntryDetailVo.getJobVo().setCode(peopleEntryDetailPo.getJob());
        peopleEntryDetailVo.getJobVo().setName(szxmCommonUtil.getDictionaryName("base.position.type", peopleEntryDetailPo.getJob()));
        peopleEntryDetailVo.setAge(szxmCommonUtil.getAge(peopleEntryDetailPo.getBornDate()));
        return peopleEntryDetailVo;
    }

    @Override
    public void deletePeopleEntryDetail(List<Integer> ids) {
        //??????????????????
        this.deleteByIds(ids);
    }

    @Override
    public String uploadPeoEntryDetailFile(MultipartFile multipartFile, Map<String, Object> paramMap) {
        if (multipartFile.isEmpty()) {
            throw new BaseException("??????????????????!");
        }
        String fileName = multipartFile.getOriginalFilename();//?????????
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);//????????????
        if (!"xlsx".equals(ext)) {
            throw new BaseException("?????????????????????!");
        }
        Workbook wb = null;
        try {
            wb = ExcelUtil.getWorkbook(multipartFile);
            Sheet sheet = ExcelUtil.getSheet(wb, 0);//???????????????????????????
            // ??????????????????????????????
            List<Map<String, Object>> dataList = ExcelUtil.getSheetValue(sheet, 1);
            List<Map<String, Object>> columnList = ExcelUtil.getSheetValue(sheet, 0, 1);

            //??????????????????
            ExcelError excelError = new ExcelError();
            Integer enTryId = Integer.valueOf(String.valueOf(paramMap.get("enTryId")));// ???????????????ID*
            Integer projectId = ObjectUtils.isEmpty(paramMap.get("projectId")) ? null :
                    Integer.valueOf(String.valueOf(paramMap.get("projectId")));// ??????ID
            Integer sectionId = ObjectUtils.isEmpty(paramMap.get("sectionId")) ? null :
                    Integer.valueOf(String.valueOf(paramMap.get("sectionId")));// ??????ID
            String name = "";// ??????*
            String type = "";// ????????????
            String job = "";// ??????
            String sex = "";// ?????? 0 ??? 1???
            String bornDate = "";// ????????????
            String telPhone = "";// ????????????
            String idCard = "";// ????????????*
            String peoType = "";// ????????????(1 ?????? 0 ??????)
            BigDecimal classHour = BigDecimal.valueOf(0);// ??????
            BigDecimal score = BigDecimal.valueOf(0);// ????????????
            List<PeopleEntryDetailPo> insertPos = Lists.newArrayList();
            String pattern = "";
            Map<String, DictionaryVo> psotionDictMap = commDictService.getDictMapByTypeCode("base.position.type");
            Map<String, DictionaryVo> peopleTypeDictMap = commDictService.getDictMapByTypeCode("szxm.rygl.peopleType");
            //????????????
            List<String> impExistCardIds = Lists.newArrayList();
            Example example = new Example(PeopleEntryDetailPo.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("enTryId", enTryId);
            List<PeopleEntryDetailPo> peopleEntryDetailPos = this.selectByExample(example);
            //????????????
            PeopleEntryVo peopleEntryVo = peopleEntryService.selectByPeopleEntryId(enTryId);

            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> datamap = dataList.get(i);
                excelError.addRow(Integer.valueOf(String.valueOf(datamap.get("rowIndex"))) + 1);

                name = String.valueOf(datamap.get("0"));
                if (StringHelper.isNullAndEmpty(name))
                    excelError.addError(0, "??????", "????????????????????????");

                type = String.valueOf(datamap.get("1"));
                type = szxmCommonUtil.getDictionaryCode(peopleTypeDictMap, type);

                job = String.valueOf(datamap.get("2"));
                job = szxmCommonUtil.getDictionaryCode(psotionDictMap, job);

                sex = String.valueOf(datamap.get("3"));
                if ("???".equals(sex))
                    sex = "0";
                else
                    sex = "1";

                bornDate = String.valueOf(datamap.get("4"));
                if (!StringHelper.isNullAndEmpty(bornDate)) {
                    boolean isMatch = DateUtil.isValidDate(bornDate, DateUtil.DATE_DEFAULT_FORMAT);
                    if (!isMatch)
                        excelError.addError(4, "????????????", "??????????????????????????????????????????yyyy-MM-dd");
                }

                telPhone = String.valueOf(datamap.get("5"));
                if (StringHelper.isNullAndEmpty(telPhone))
                    excelError.addError(5, "????????????", "????????????????????????");
                else {
                    //pattern="^\\d+$";
                    pattern = "^1[0-9]\\d{9}$";
                    boolean isMatch = Pattern.matches(pattern, telPhone);
                    if (!isMatch)
                        excelError.addError(5, "????????????", "????????????????????????");
                }

                idCard = String.valueOf(datamap.get("6"));
                if (StringHelper.isNullAndEmpty(idCard))
                    excelError.addError(6, "????????????", "????????????????????????");
                else {
                    pattern =
                            "^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$|^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|X)$";
                    boolean isMatch = Pattern.matches(pattern, idCard);
                    if (!isMatch)
                        excelError.addError(6, "????????????", "????????????????????????");
                }

                peoType = "1";
                if ("5".equals(peopleEntryVo.getOrgCategoryVo().getCode())) {//????????????????????????
                    peoType = "0";
                }

                String classHourStr = String.valueOf(datamap.get("7"));
                if (StringHelper.isNullAndEmpty(classHourStr)) {
                    excelError.addError(7, "??????", "??????????????????");
                } else {
                    pattern = "^\\d+(.\\d+)?$";
                    boolean isMatch = Pattern.matches(pattern, classHourStr);
                    if (!isMatch)
                        excelError.addError(7, "??????", "?????????????????????");
                    classHour = new BigDecimal(classHourStr);
                }

                String scoreStr = String.valueOf(datamap.get("8"));
                if (StringHelper.isNullAndEmpty(scoreStr)) {
                    excelError.addError(8, "????????????", "????????????????????????");
                } else {
                    pattern = "^\\d+(.\\d+)?$";
                    boolean isMatch = Pattern.matches(pattern, scoreStr);
                    if (!isMatch)
                        excelError.addError(8, "????????????", "???????????????????????????");
                    score = new BigDecimal(scoreStr);
                }

                PeopleEntryDetailPo peopleEntryDetailPo = new PeopleEntryDetailPo();
                peopleEntryDetailPo.setName(name);
                peopleEntryDetailPo.setJob(job);
                if (StringHelper.isNotNullAndEmpty(bornDate))
                    peopleEntryDetailPo.setBornDate(DateUtil.formatDate(bornDate, DateUtil.DATE_DEFAULT_FORMAT));
                peopleEntryDetailPo.setTelPhone(telPhone);
                peopleEntryDetailPo.setPeoType(peoType);
                peopleEntryDetailPo.setProjectId(projectId);
                peopleEntryDetailPo.setSectionId(sectionId);
                peopleEntryDetailPo.setSex(sex);
                peopleEntryDetailPo.setType(type);
                peopleEntryDetailPo.setIdCard(idCard);
                peopleEntryDetailPo.setEnTryId(enTryId);
                peopleEntryDetailPo.setClassHour(classHour);
                peopleEntryDetailPo.setScore(score);
                //Excel??????
                if (!impExistCardIds.contains(peopleEntryDetailPo.getIdCard())) {
                    //?????????
                    boolean canInsert = true;
                    for (PeopleEntryDetailPo tabExistPo : peopleEntryDetailPos) {
                        if (tabExistPo.getIdCard().equals(peopleEntryDetailPo.getIdCard())) {
                            canInsert = false;
                            break;
                        }
                    }
                    if (canInsert)
                        insertPos.add(peopleEntryDetailPo);
                    else
                        excelError.addError(6, "????????????", "????????????????????????????????????????????????");
                    impExistCardIds.add(peopleEntryDetailPo.getIdCard());
                } else
                    excelError.addError(6, "????????????", "?????????????????????????????????????????????");

            }

            //???????????????????????????????????? ?????????
            List<PeopleVo> peopleVos = peopleService.selectPeopleByIDCard(null);
            Map<String, PeopleVo> idCardMap = ListUtil.listToMap(peopleVos, "idCard", String.class);

            // ?????????????????????????????????????????????
            Map<String, Object> mapWhere = Maps.newHashMap();
            List<String> idCards = ListUtil.toValueList(insertPos, "idCard", String.class);
            mapWhere.put("idCards", idCards);
            List<PeopleEntryDetailVo> peopleEntryDetailVos = mapper.selectEntryDetail(mapWhere);
            Map<String, List<PeopleEntryDetailVo>> idCardEntryDetailMap = Maps.newHashMap();
            if (!ObjectUtils.isEmpty(peopleEntryDetailVos)) {
                for (PeopleEntryDetailVo peopleEntryDetailVo : peopleEntryDetailVos) {
                    if (ObjectUtils.isEmpty(idCardEntryDetailMap.get(peopleEntryDetailVo.getIdCard()))) {
                        List<PeopleEntryDetailVo> entryDetailVos = Lists.newArrayList();
                        entryDetailVos.add(peopleEntryDetailVo);
                        idCardEntryDetailMap.put(peopleEntryDetailVo.getIdCard(), entryDetailVos);
                    } else {
                        idCardEntryDetailMap.get(peopleEntryDetailVo.getIdCard()).add(peopleEntryDetailVo);
                    }
                }
            }

            List<PeopleEntryDetailPo> checkPos = Lists.newArrayList();
            List<PeopleEntryDetailVo> existPos = Lists.newArrayList();
            if (!ObjectUtils.isEmpty(insertPos)) {
                for (PeopleEntryDetailPo peopleEntryDetailPo : insertPos) {
                    //????????????????????????????????????????????????????????????
                    PeopleVo peopleVo = idCardMap.get(peopleEntryDetailPo.getIdCard());
                    if (!ObjectUtils.isEmpty(peopleVo)) {
                        PeopleEntryDetailVo peopleEntryDetailVo = new PeopleEntryDetailVo();
                        peopleEntryDetailVo.setName(peopleEntryDetailPo.getName());
                        peopleEntryDetailVo.setOrgName(peopleVo.getOrgName());
                        peopleEntryDetailVo.setIdCard(peopleEntryDetailPo.getIdCard());
                        existPos.add(peopleEntryDetailVo);
                        continue;
                    }
                    //??????????????????????????????????????????????????????????????????
                    List<PeopleEntryDetailVo> entryDetailVos = idCardEntryDetailMap.get(peopleEntryDetailPo.getIdCard());
                    if (!ObjectUtils.isEmpty(entryDetailVos) && "0".equals(entryDetailVos.get(0).getEntryType())) {
                        PeopleEntryDetailVo peopleEntryDetailVo = new PeopleEntryDetailVo();
                        peopleEntryDetailVo.setName(peopleEntryDetailPo.getName());
                        peopleEntryDetailVo.setOrgName(entryDetailVos.get(0).getOrgName());
                        peopleEntryDetailVo.setIdCard(peopleEntryDetailPo.getIdCard());
                        existPos.add(peopleEntryDetailVo);
                        continue;
                    }
                    checkPos.add(peopleEntryDetailPo);
                }
            }

            if (!excelError.isHasError()) {
                this.insert(checkPos);
                if (!ObjectUtils.isEmpty(existPos)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (PeopleEntryDetailVo detailVo:existPos) {
                        stringBuilder.append("?????????");
                        stringBuilder.append(detailVo.getName());
                        stringBuilder.append("????????????");
                        stringBuilder.append(detailVo.getIdCard());
                        stringBuilder.append("???"+ detailVo.getOrgName());
                        stringBuilder.append("???????????????");
                    }
                    String nameStr = stringBuilder.toString();
                    excelError.addError(6, "????????????", nameStr + "????????????????????????????????????");
                    String errorId = UUIDHexGenerator.generator();
                    redisUtil.setxObjectValue(errorId, excelError, 120);//120????????????
                    return errorId;
                }
                return "";
            } else {//????????????Excel
                Map<String, Object> configMap = Maps.newHashMap();
                String errorId = UUIDHexGenerator.generator();
                redisUtil.setxObjectValue(errorId, excelError, 120);//120????????????
                return errorId;
            }
        } catch (Exception e) {
            throw new BaseException("????????????!");
        }

    }

    @Override
    public List<PeopleEntryDetailVo> addPeopleEntryDetail(List<PeopleEntryDetailAddForm> peopleEntryDetailAddForms) {
        //????????????
        List<PeopleEntryDetailPo> peopleEntryDetailPos = Lists.newArrayList();
        if (!ObjectUtils.isEmpty(peopleEntryDetailAddForms)) {
            Example example = new Example(PeopleEntryDetailPo.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("enTryId", peopleEntryDetailAddForms.get(0).getEnTryId());
            peopleEntryDetailPos = this.selectByExample(example);
        }

        List<PeopleEntryDetailPo> insertPos = Lists.newArrayList();
        for (PeopleEntryDetailAddForm peopleEntryDetailAddForm : peopleEntryDetailAddForms) {
            if (ObjectUtils.isEmpty(peopleEntryDetailAddForm.getPeopleId()))
                throw new BaseException("??????ID????????????");
            PeopleEntryDetailPo peopleEntryDetailPo = dozerMapper.map(peopleEntryDetailAddForm, PeopleEntryDetailPo.class);

            if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getType()))
                peopleEntryDetailPo.setType("2");
            if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getPeoType()))
                peopleEntryDetailPo.setPeoType("1");
            if (StringHelper.isNullAndEmpty(peopleEntryDetailPo.getSex()))
                peopleEntryDetailPo.setSex("1");
            if (ObjectUtils.isEmpty(peopleEntryDetailPo.getClassHour()))
                peopleEntryDetailPo.setClassHour(BigDecimal.valueOf(0));
            boolean canInsert = true;
            for (PeopleEntryDetailPo tabExistPo : peopleEntryDetailPos) {
                if (tabExistPo.getIdCard().equals(peopleEntryDetailPo.getIdCard())) {
                    canInsert = false;
                    break;
                }
            }
            if (canInsert) {
                insertPos.add(peopleEntryDetailPo);
            }
        }
        this.insert(insertPos);
        List<PeopleEntryDetailVo> peopleEntryDetailVos = Lists.newArrayList();
        Map<String, DictionaryVo> psotionDictMap = commDictService.getDictMapByTypeCode("base.position.type");
        for (PeopleEntryDetailPo peopleEntryDetailPo : insertPos) {
            PeopleEntryDetailVo peopleEntryDetailVo =
                    dozerMapper.map(peopleEntryDetailPo, PeopleEntryDetailVo.class);//po???????????????Vo??????
            peopleEntryDetailVo.getTypeVo().setCode(peopleEntryDetailPo.getType());
            if ("0".equals(peopleEntryDetailPo.getType()))
                peopleEntryDetailVo.getTypeVo().setName("????????????");
            else if ("1".equals(peopleEntryDetailPo.getType()))
                peopleEntryDetailVo.getTypeVo().setName("????????????");
            else if ("2".equals(peopleEntryDetailPo.getType()))
                peopleEntryDetailVo.getTypeVo().setName("????????????");

            peopleEntryDetailVo.getPeoTypeVo().setCode(peopleEntryDetailPo.getPeoType());
            if ("0".equals(peopleEntryDetailPo.getPeoType()))
                peopleEntryDetailVo.getPeoTypeVo().setName("??????");
            else if ("1".equals(peopleEntryDetailPo.getType()))
                peopleEntryDetailVo.getPeoTypeVo().setName("??????");
            peopleEntryDetailVo.getSexVo().setCode(peopleEntryDetailPo.getSex());
            if ("0".equals(peopleEntryDetailPo.getSex()))
                peopleEntryDetailVo.getSexVo().setName("???");
            else if ("1".equals(peopleEntryDetailPo.getSex()))
                peopleEntryDetailVo.getSexVo().setName("???");
            peopleEntryDetailVo.getJobVo().setCode(peopleEntryDetailPo.getJob());
            peopleEntryDetailVo.getJobVo().setName(szxmCommonUtil.getDictionaryName(psotionDictMap, peopleEntryDetailPo.getJob()));
            peopleEntryDetailVo.setAge(szxmCommonUtil.getAge(peopleEntryDetailPo.getBornDate()));
            peopleEntryDetailVos.add(peopleEntryDetailVo);
        }
        return peopleEntryDetailVos;
    }

    public static void main(String args[]) {
        String pattern = "^1[0-9]\\d{9}$";
        boolean isMatch = Pattern.matches(pattern, "100527583051");
        System.out.println(isMatch);
    }

}
