 package com.wisdom.acm.processing.service.report.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wisdom.acm.processing.common.*;
import com.wisdom.acm.processing.common.officeUtils.WordUtil;
import com.wisdom.acm.processing.mapper.report.DailyReportMapper;
import com.wisdom.acm.processing.mapper.report.DocMapper;
import com.wisdom.acm.processing.po.construction.ConstructionDailyPo;
import com.wisdom.acm.processing.po.report.DailyReportPo;
import com.wisdom.acm.processing.service.construction.ConstructionService;
import com.wisdom.acm.processing.service.fault.FaultProblemService;
import com.wisdom.acm.processing.service.fault.FaultService;
import com.wisdom.acm.processing.service.report.OdrCCDailyReportService;
import com.wisdom.acm.processing.service.representation.DailyRepresentationService;
import com.wisdom.acm.processing.service.train.ScheduleService;
import com.wisdom.acm.processing.service.train.TrainDailyService;
import com.wisdom.acm.processing.service.weather.WeatherService;
import com.wisdom.acm.processing.vo.fault.FaultDailyProblemDealVo;
import com.wisdom.acm.processing.vo.fault.FaultDailyProblemVo;
import com.wisdom.acm.processing.vo.fault.FaultVo;
import com.wisdom.acm.processing.vo.report.DailyReportVo;
import com.wisdom.acm.processing.vo.report.DocFileVo;
import com.wisdom.acm.processing.vo.representation.DailyRepresentationVo;
import com.wisdom.acm.processing.vo.train.ScheduleVo;
import com.wisdom.acm.processing.vo.train.TrainDailyVo;
import com.wisdom.base.common.aspect.AddLog;
import com.wisdom.base.common.dc.feign.DcBaseService;
import com.wisdom.base.common.dc.util.EnumsUtil;
import com.wisdom.base.common.enums.LoggerModuleEnum;
import com.wisdom.base.common.feign.CommAuthService;
import com.wisdom.base.common.feign.HrbSysService;
import com.wisdom.base.common.feign.doc.CommDocService;
import com.wisdom.base.common.log.AcmLogger;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ResourceUtil;
import com.wisdom.base.common.vo.JwtAuthenticationRequest;
import com.wisdom.base.common.vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.mapper.entity.Example;

import java.io.*;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zll
 * 2020/8/15/014 16:51
 * Description:<??????>
 */
@Service
@Slf4j
public class OdrCCDailyReportServiceImpl extends BaseService<DailyReportMapper, DailyReportPo> implements OdrCCDailyReportService {
    @Autowired
    private DcBaseService dcBaseService;

    @Autowired
    private DcCommonUtil dcCommonUtil;

    @Autowired
    private CommAuthService commAuthService;

    @Autowired
    private CommDocService commDocService;

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private TrainDailyService trainDailyService;

    @Autowired
    private ConstructionService constructionService;

    @Autowired
    private FaultService faultService;

    @Autowired
    private FaultProblemService faultProblemService;

    @Autowired
    private DailyRepresentationService dailyRepresentationService;

    @Autowired
    private HrbSysService hrbSysService;
    @Autowired
    private WeatherService weatherService;

    private static Logger logger = LoggerFactory.getLogger(OdrCCDailyReportServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    //@Async("odr_report")
    @AddLog(title = "????????????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateCCDailyWorkReportFile(Map<String, Object> mapWhere,UserInfo user){
        logger.info("????????????????????????:  " + Thread.currentThread().getName());
        //1????????????
        if(!ObjectUtils.isEmpty(mapWhere.get("line1"))){
            Map<String,Object>map=Maps.newHashMap();
            map.put("date",mapWhere.get("date"));
            map.put("line1",mapWhere.get("line1"));
            map.put("description",mapWhere.get("description"));
            map.put("keys",mapWhere.get("keys"));
            map.put("token",mapWhere.get("token"));
            map.put("gateWayHost",mapWhere.get("gateWayHost"));
            DailyReportVo dailyReportVo= main(map,user);
            this.setAcmLogger(new AcmLogger("??????1???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line1").toString(),
                        dailyReportVo.getId().toString(),"INIT"));
            return dailyReportVo;
        }
        //2????????????????????????
        //3????????????
        if(!ObjectUtils.isEmpty(mapWhere.get("line3"))){
            Map<String,Object>map=Maps.newHashMap();
            map.put("date",mapWhere.get("date"));
            map.put("line3",mapWhere.get("line3"));
            map.put("description",mapWhere.get("description"));
            map.put("keys",mapWhere.get("keys"));
            map.put("token",mapWhere.get("token"));
            map.put("gateWayHost",mapWhere.get("gateWayHost"));
            DailyReportVo dailyReportVo= main(map,user);
            this.setAcmLogger(new AcmLogger("??????3???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line3").toString(),
                        dailyReportVo.getId().toString(),"INIT"));
            return dailyReportVo;
        }
        return null;
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
//    @Async("odr_report")
//    @AddLog(title = "??????????????????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
//    public List<DailyReportVo> generateCCDailyWorkReportFile(Map<String, Object> mapWhere) throws IOException {
//        System.out.println("????????????????????????:  "+Thread.currentThread().getName());
//        List<DailyReportVo> dailyReportVos=Lists.newArrayList();
//        //1????????????
//        if(!ObjectUtils.isEmpty(mapWhere.get("line1"))){
//            Map<String,Object>map=Maps.newHashMap();
//            map.put("date",mapWhere.get("date"));
//            map.put("line1",mapWhere.get("line1"));
//            map.put("description",mapWhere.get("description"));
//            map.put("keys",mapWhere.get("keys"));
//            map.put("token",mapWhere.get("token"));
//            map.put("gateWayHost",mapWhere.get("gateWayHost"));
//            DailyReportVo dailyReportVo= main(map);
//            dailyReportVos.add(dailyReportVo);
//            if("???????????????".equals(mapWhere.get("description").toString())){
//                this.setAcmLogger(new AcmLogger("?????????????????????--1???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line1").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }else{
//                this.setAcmLogger(new AcmLogger("??????1???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line1").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }
//        }
//        //2????????????????????????
//        //3????????????
//        if(!ObjectUtils.isEmpty(mapWhere.get("line3"))){
//            Map<String,Object>map=Maps.newHashMap();
//            map.put("date",mapWhere.get("date"));
//            map.put("line3",mapWhere.get("line3"));
//            map.put("description",mapWhere.get("description"));
//            map.put("keys",mapWhere.get("keys"));
//            map.put("token",mapWhere.get("token"));
//            map.put("gateWayHost",mapWhere.get("gateWayHost"));
//            DailyReportVo dailyReportVo= main(map);
//            dailyReportVos.add(dailyReportVo);
//            if("???????????????".equals(mapWhere.get("description").toString())){
//                this.setAcmLogger(new AcmLogger("?????????????????????--3???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line3").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }else{
//                this.setAcmLogger(new AcmLogger("??????3???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line3").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }
//        }
//        return dailyReportVos;
//    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Async("odr_report")
    @AddLog(title = "????????????1????????????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateCCDailyWorkReportFile1(Map<String, Object> maps){
        logger.info("????????????????????????:  " + Thread.currentThread().getName());
        //1????????????
        Map<String,Object>map=Maps.newHashMap();
        map.put("date",maps.get("date"));
        map.put("line1",maps.get("line1"));
        map.put("description",maps.get("description"));
        map.put("keys",maps.get("keys"));
        map.put("token",maps.get("token"));
        map.put("gateWayHost",maps.get("gateWayHost"));
        DailyReportVo dailyReportVo= main(map);
        this.setAcmLogger(new AcmLogger("?????????????????????--1???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line1").toString(),
                    dailyReportVo.getId().toString(),"INIT"));
        return dailyReportVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Async("odr_report")
    @AddLog(title = "????????????3????????????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateCCDailyWorkReportFile3(Map<String, Object> maps){
        logger.info("????????????????????????:  " + Thread.currentThread().getName());
        //2????????????????????????
        //3????????????
        Map<String,Object>map=Maps.newHashMap();
        map.put("date",maps.get("date"));
        map.put("line3",maps.get("line3"));
        map.put("description",maps.get("description"));
        map.put("keys",maps.get("keys"));
        map.put("token",maps.get("token"));
        map.put("gateWayHost",maps.get("gateWayHost"));
        DailyReportVo dailyReportVo= main(map);
        this.setAcmLogger(new AcmLogger("?????????????????????--3???????????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line3").toString(),
                    dailyReportVo.getId().toString(),"INIT"));
        return dailyReportVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void approveDailyReportFlow(String bizType, List<Integer> ids,UserInfo user,Map<String,Object> mp) {
        Example example = new Example(DailyReportPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", ids);
        List<DailyReportPo> list = this.mapper.selectByExample(example);
        for(DailyReportPo po: list){
            String description= po.getDescription();
            po.setReviewer(user.getId());
            po.setReviewStatus(EnumsUtil.StatusEnum.APPROVED.getCode());
            //word??????????????????????????????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            String fileUrl=po.getWordFile();
            String newFileUrl=System.getProperty("user.dir") + "/temporary"+"/yyrb_" + System.currentTimeMillis() + ".docx";
            File newFile1=new File(newFileUrl);
            File newFile2=null;
            Integer fileId = null;
            Integer fileId_ = null;
            Map<String,String>fileMap=Maps.newHashMap();
            try {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("shr", user.getActuName());
                //??????word??????
                FileDownLoad.downloadFile(fileUrl,newFileUrl);
                //????????????doc???????????????pdf???word
                List<Integer> fileIds_=Lists.newArrayList();
                List<Map<String,Object>>mapList=Lists.newArrayList();
                Map<String,Object>m=Maps.newHashMap();
                m.put("type","doc");
                m.put("id",po.getFileId());
                mapList.add(m);
                fileIds_.add(po.getFileId());
                commDocService.deleteByFileIds(fileIds_);
                commDocService.deleteFileByFileIds(mapList);
                InputStream fileInputStream = new FileInputStream(newFileUrl);
                WordUtil wordUtil = new WordUtil();
                String file="";
                if("0".equals(po.getReportType())){//????????????0
                    if(description.equals("???????????????")){
                        file = "/?????????????????????????????????" +po.getLine()+"??????-" +DateUtil.getNowTimeByChina(new Date()) + ".docx";
                    }else{
                        file = "/?????????????????????" +po.getLine()+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                    }
                }else{
                    if(description.equals("???????????????")){
                        file = "/???????????????????????????????????????" +po.getLine()+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                    }else{
                        file = "/???????????????????????????" +po.getLine()+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                    }
                }
                String outFile= tempFlolderStr + file;
                //??????????????????
                newFile2 = new File(outFile);
                wordUtil.generateWord(fileInputStream, params, outFile);
                FileInputStream outPutStream = new FileInputStream(outFile);
                //?????????
                outPutStream.close();
                FileSystemResource fileSystemResource = new FileSystemResource(outFile);
                Map<String,Object> returnMap=uploadFile(fileSystemResource,description,mp);
                Map<String,Object> map= (Map<String, Object>) returnMap.get("data");
                fileId= (Integer) map.get("id");
                Map<String,Object> maps= Maps.newHashMap();
                maps.put("fileId",fileId);
                maps.put("author",user.getActuName());
                if("0".equals(po.getReportType())){//????????????0
                    if(description.equals("???????????????")){
                        if("1".equals(po.getLine())){
                            maps.put("docTitle","????????????_1???????????????????????????");
                        }
                        if("2".equals(po.getLine())){
                            maps.put("docTitle","????????????_2???????????????????????????");
                        }
                        if("3".equals(po.getLine())){
                            maps.put("docTitle","????????????_3???????????????????????????");
                        }
                    }else{
                        if("1".equals(po.getLine())){
                            maps.put("docTitle","????????????_1??????");
                        }
                        if("2".equals(po.getLine())){
                            maps.put("docTitle","????????????_2??????");
                        }
                        if("3".equals(po.getLine())){
                            maps.put("docTitle","????????????_3??????");
                        }
                    }
                }else{
                    if(description.equals("???????????????")){
                        if("1".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_1???????????????????????????");
                        }
                        if("2".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_2???????????????????????????");
                        }
                        if("3".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_3???????????????????????????");
                        }
                    }else{
                        if("1".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_1??????");
                        }
                        if("2".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_2??????");
                        }
                        if("3".equals(po.getLine())){
                            maps.put("docTitle","??????????????????_3??????");
                        }
                    }
                }
                //maps.put("docNum",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                maps.put("docNum",System.currentTimeMillis());
                if("0".equals(po.getReportType())) {//????????????0
                    //?????????????????????
                    if("1".equals(po.getLine())){
                        maps.put("folderId",1121588);
                    }
                    if("2".equals(po.getLine())){
                        maps.put("folderId",1121590);
                    }
                    if("3".equals(po.getLine())){
                        maps.put("folderId",1121592);
                    }
                }else{
                    //???????????????????????????
                    if("1".equals(po.getLine())){
                        maps.put("folderId",1121594);
                    }
                    if("2".equals(po.getLine())){
                        maps.put("folderId",1121594);
                    }
                    if("3".equals(po.getLine())){
                        maps.put("folderId",1121594);
                    }
                }
                maps.put("secutyLevel",1);
                maps.put("version","1.0");
                maps.put("scope",Lists.newArrayList());
                maps.put("description",description);
                maps.put("token",mp.get("token"));
                maps.put("gateWayHost",mp.get("gateWayHost"));
                fileId_=addFile(maps);
                fileMap=queryFile(fileId,description,mp,fileId_);
            } catch (Exception e) {
                logger.info("e.getMessage(): " + e.getMessage());
            }finally {
                newFile1.delete();
                newFile2.delete();
            }
            po.setPdfFile(fileMap.get("pdfFile"));
            po.setWordFile(fileMap.get("wordFile"));
            po.setFileId(fileId_);
            po.setLoadId(fileId);
            this.updateById(po);
        }
        //????????????
        List<DailyReportPo> trafficMainPos = super.selectByIds(ids);
        dcCommonUtil.approveFlowAndSendMessage(bizType, trafficMainPos);
    }

    private DailyReportVo main(Map<String, Object> mapWhere){
        String date= (String) mapWhere.get("date");
        String line="";
        if(!ObjectUtils.isEmpty(mapWhere.get("line1"))){
            line= (String) mapWhere.get("line1");
        }
        if(!ObjectUtils.isEmpty(mapWhere.get("line2"))){
            line= (String) mapWhere.get("line2");
        }
        if(!ObjectUtils.isEmpty(mapWhere.get("line3"))){
            line= (String) mapWhere.get("line3");
        }
        String ts="";
        try {
            ts=getOperateTimes(line,mapWhere);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> keyAll= Arrays.asList("1","2","3","4","5");
        List<String> keyContains= (List<String>) mapWhere.get("keys");
        List<String> keyNoContains= Lists.newArrayList();
        for(String strs:keyAll){
            if(!keyContains.contains(strs)){
                keyNoContains.add(strs);
            }
        }
        String str="??????????????????";
        //??????????????????
        String drtq="--";
//        String weatherUrl = dcCommonUtil.getWeatherUrl();
//        try {
//            if(!ObjectUtils.isEmpty(weatherUrl))
//            {
//                Map<String, Object> info= WeatherUtil.getTodayWeather(weatherUrl);
//                if(!ObjectUtils.isEmpty(info)){
//                    drtq=info.get("condTxt").toString()+", ?????? "+info.get("tmpMin").toString()+"???"+" - "+info.get("tmpMax").toString()+"???";
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //????????????????????????
        String result = weatherService.query(DateUtil.formatDate(date + " 00:00:00", "yyyy-MM-dd HH:mm:ss"));
        if(!ObjectUtils.isEmpty(result) && !"--".equals(result)){//??????????????????????????????????????????--???
            drtq = result;
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("line", line);
        params.put("ts", ts);
        params.put("drtq", drtq);
        params.put("rq", date+"  ("+ DateUtil.getWeek(DateUtil.getDateFormat(date))+")");
        params.put("tbdw", "????????????");
        params.put("tbr",str);
        //params.put("shr", "?????????");
        params.put("tbrq", DateUtil.formateNyrsfm(new Date()));
        for(String key: keyContains){
            //????????????
            if("1".equals(key)){
                generateTrainWord(date,line,params);
            }
            //??????????????????
            if("2".equals(key)){
                generateOperateWord(date,line,params);
            }
            //????????????
            if("3".equals(key)){
                generateConstructionWord(date,line,params);
            }
            //??????
            if("4".equals(key)){
                generateFaultWord(date,line,params);
            }
            //??????????????????
            if("5".equals(key)){
                generateTipsWord(date,line,params);
            }
        }
        Integer fileId_ = null;
        Integer fileId = null;
        //??????word??????
        File newFile1 = null;
        File newFile2 = null;
        Map<String,String>fileMap=Maps.newHashMap();
        try {
            InputStream fileInputStrem= ResourceUtil.findResoureFile("template/zhzxrbmb.docx");
            //??????word???????????????
            XWPFDocument document= new XWPFDocument(fileInputStrem);
            //?????????????????????????????????word???fileInputStrem?????????????????????????????????????????????????????????????????????word
            //??????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            logger.info("user.dir : " + System.getProperty("user.dir"));
            File tempFlolder = new File(tempFlolderStr);
            if (!tempFlolder.isDirectory())
            {
                tempFlolder.mkdirs();
            }
            String fileName = "/yyrb_" + System.currentTimeMillis() + ".docx";
            String outFileName = tempFlolderStr + fileName;
            //??????????????????
            newFile1 = new File(outFileName);
            FileOutputStream output = new FileOutputStream(outFileName);
            document.write(output);
            //?????????
            fileInputStrem.close();
            output.close();
            InputStream fileInputStream = new FileInputStream(outFileName);
            WordUtil wordUtil = new WordUtil();
            String file="";
            if(((String)mapWhere.get("description")).equals("???????????????")){
                file = "/??????????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
            }else{
                file = "/??????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
            }
            String newFileName=file.substring(1);
            //???????????????????????????????????????????????????
            if(getDocFileNames(DateUtil.getDateFormat(date)).stream().filter(e->e.getFileName().
                    equals(newFileName)).distinct().collect(Collectors.toList()).size()!=0){
                Thread.sleep(1000);
                if(((String)mapWhere.get("description")).equals("???????????????")){
                    file = "/??????????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                }else{
                    file = "/??????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date()) + ".docx";
                }
            }
            String outFile= tempFlolderStr + file;
            //??????????????????
            newFile2 = new File(outFile);
            wordUtil.generateWordZll(fileInputStream, params, outFile,keyNoContains);
            FileInputStream outPutStream = new FileInputStream(outFile);
            //?????????
            outPutStream.close();
            FileSystemResource fileSystemResource = new FileSystemResource(outFile);
            String description= (String) mapWhere.get("description");
            Map<String,Object> returnMap=uploadFile(fileSystemResource,description,mapWhere);
            Map<String,Object> map= (Map<String, Object>) returnMap.get("data");
            fileId= (Integer) map.get("id");
            Map<String,Object> maps= Maps.newHashMap();
            maps.put("fileId",fileId);
            maps.put("author",str);
            if(description.equals("???????????????")){
                if("1".equals(line)){
                    maps.put("docTitle","??????????????????_1???????????????????????????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","??????????????????_2???????????????????????????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","??????????????????_3???????????????????????????");
                }
            }else{
                if("1".equals(line)){
                    maps.put("docTitle","??????????????????_1??????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","??????????????????_2??????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","??????????????????_3??????");
                }
            }
            //maps.put("docNum",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+System.currentTimeMillis());
            maps.put("docNum",System.currentTimeMillis());
            //???????????????????????????
            if("1".equals(line)){
                maps.put("folderId",1121594);
            }
            if("2".equals(line)){
                maps.put("folderId",1121594);
            }
            if("3".equals(line)){
                maps.put("folderId",1121594);
            }
            maps.put("secutyLevel",1);
            maps.put("version","1.0");
            maps.put("scope",Lists.newArrayList());
            maps.put("description",description);
            maps.put("token",mapWhere.get("token"));
            maps.put("gateWayHost",mapWhere.get("gateWayHost"));
            fileId_=addFile(maps);
            fileMap=queryFile(fileId,description,mapWhere,fileId_);
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            newFile1.delete();
            newFile2.delete();
        }
        //????????????????????????????????????
        DailyReportPo dailyReportPo = new DailyReportPo();
        if (ObjectUtils.isEmpty(dailyReportPo.getSort())) {//??????????????????,??????????????????????????????????????????+1?????????
            dailyReportPo.setSort(mapper.selectNextSort());
        }
        dailyReportPo.setReviewStatus("INIT");
        dailyReportPo.setPdfFile(fileMap.get("pdfFile"));
        dailyReportPo.setWordFile(fileMap.get("wordFile"));
        dailyReportPo.setReportType("1");
        dailyReportPo.setReportName(DateUtil.getDateFormat(date));
        dailyReportPo.setLine(line);
        dailyReportPo.setDescription((String) mapWhere.get("description"));
        dailyReportPo.setCreator(206);
        dailyReportPo.setFileId(fileId_);
        dailyReportPo.setLoadId(fileId);
        this.insert(dailyReportPo);
        DailyReportVo dailyReportVo=dozerMapper.map(dailyReportPo, DailyReportVo.class);
        return dailyReportVo;
    }

    private DailyReportVo main(Map<String, Object> mapWhere,UserInfo user){
        String date= (String) mapWhere.get("date");
        String line="";
        if(!ObjectUtils.isEmpty(mapWhere.get("line1"))){
            line= (String) mapWhere.get("line1");
        }
        if(!ObjectUtils.isEmpty(mapWhere.get("line2"))){
            line= (String) mapWhere.get("line2");
        }
        if(!ObjectUtils.isEmpty(mapWhere.get("line3"))){
            line= (String) mapWhere.get("line3");
        }
        String ts="";
        try {
            ts=getOperateTimes(line,mapWhere);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> keyAll= Arrays.asList("1","2","3","4","5");
        List<String> keyContains= (List<String>) mapWhere.get("keys");
        List<String> keyNoContains= Lists.newArrayList();
        for(String strs:keyAll){
            if(!keyContains.contains(strs)){
                keyNoContains.add(strs);
            }
        }
        Integer userId=user.getId();
        //??????????????????
        String drtq="--";
//        String weatherUrl = dcCommonUtil.getWeatherUrl();
//        try {
//            if(!ObjectUtils.isEmpty(weatherUrl))
//            {
//                Map<String, Object> info= WeatherUtil.getTodayWeather(weatherUrl);
//                if(!ObjectUtils.isEmpty(info)){
//                    drtq=info.get("condTxt").toString()+", ?????? "+info.get("tmpMin").toString()+"???"+" - "+info.get("tmpMax").toString()+"???";
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //????????????????????????
        String result = weatherService.query(DateUtil.formatDate(date + " 00:00:00", "yyyy-MM-dd HH:mm:ss"));
        if(!ObjectUtils.isEmpty(result) && !"--".equals(result)){//??????????????????????????????????????????--???
            drtq = result;
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("line", line);
        params.put("ts", ts);
        params.put("drtq", drtq);
        params.put("rq", date+"  ("+ DateUtil.getWeek(DateUtil.getDateFormat(date))+")");
        params.put("tbdw", "????????????");
        params.put("tbr",user.getActuName());
        //params.put("shr", "?????????");
        params.put("tbrq", DateUtil.formateNyrsfm(new Date()));
        for(String key: keyContains){
            //????????????
            if("1".equals(key)){
                generateTrainWord(date,line,params);
            }
            //??????????????????
            if("2".equals(key)){
                generateOperateWord(date,line,params);
            }
            //????????????
            if("3".equals(key)){
                generateConstructionWord(date,line,params);
            }
            //??????
            if("4".equals(key)){
                generateFaultWord(date,line,params);
            }
            //??????????????????
            if("5".equals(key)){
                generateTipsWord(date,line,params);
            }
        }
        //????????????????????????????????????????????????
        if(keyNoContains.contains("5")){
            params.put("qtqksmbt","??????????????????");
            params.put("qtqksmms", " ");//??????????????????????????????????????????????????????
            keyNoContains.remove("5");
        }
        Integer fileId_ = null;
        Integer fileId = null;
        //??????word??????
        File newFile1 = null;
        File newFile2 = null;
        Map<String,String>fileMap=Maps.newHashMap();
        try {
            InputStream fileInputStrem= ResourceUtil.findResoureFile("template/zhzxrbmb.docx");
            System.out.println(fileInputStrem);
            //??????word???????????????
            XWPFDocument document= new XWPFDocument(fileInputStrem);
            //?????????????????????????????????word???fileInputStrem?????????????????????????????????????????????????????????????????????word
            //??????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            logger.info("user.dir : " + System.getProperty("user.dir"));
            File tempFlolder = new File(tempFlolderStr);
            if (!tempFlolder.isDirectory())
            {
                tempFlolder.mkdirs();
            }
            String fileName = "/yyrb_" + System.currentTimeMillis() + ".docx";
            String outFileName = tempFlolderStr + fileName;
            //??????????????????
            newFile1 = new File(outFileName);
            FileOutputStream output = new FileOutputStream(outFileName);
            document.write(output);
            //?????????
            fileInputStrem.close();
            output.close();
            InputStream fileInputStream = new FileInputStream(outFileName);
            WordUtil wordUtil = new WordUtil();
            String file="";
            if(((String)mapWhere.get("description")).equals("???????????????")){
                file = "/??????????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
            }else{
                file = "/??????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
            }
            String newFileName=file.substring(1);
            //???????????????????????????????????????????????????
            if(getDocFileNames(DateUtil.getDateFormat(date)).stream().filter(e->e.getFileName().
                    equals(newFileName)).distinct().collect(Collectors.toList()).size()!=0){
                Thread.sleep(1000);
                if(((String)mapWhere.get("description")).equals("???????????????")){
                    file = "/??????????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                }else{
                    file = "/??????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
                }
            }
            String outFile= tempFlolderStr + file;
            //??????????????????
            newFile2 = new File(outFile);
            wordUtil.generateWordZll(fileInputStream, params, outFile,keyNoContains);
            FileInputStream outPutStream = new FileInputStream(outFile);
            //?????????
            outPutStream.close();
            FileSystemResource fileSystemResource = new FileSystemResource(outFile);
            String description= (String) mapWhere.get("description");
            Map<String,Object> returnMap=uploadFile(fileSystemResource,description,mapWhere);
            Map<String,Object> map= (Map<String, Object>) returnMap.get("data");
            fileId= (Integer) map.get("id");
            Map<String,Object> maps= Maps.newHashMap();
            maps.put("fileId",fileId);
            maps.put("author",user.getActuName());
            if(description.equals("???????????????")){
                if("1".equals(line)){
                    maps.put("docTitle","??????????????????_1???????????????????????????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","??????????????????_2???????????????????????????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","??????????????????_3???????????????????????????");
                }
            }else{
                if("1".equals(line)){
                    maps.put("docTitle","??????????????????_1??????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","??????????????????_2??????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","??????????????????_3??????");
                }
            }
            maps.put("docNum",System.currentTimeMillis());
            //???????????????????????????
            if("1".equals(line)){
                maps.put("folderId",1121594);
            }
            if("2".equals(line)){
                maps.put("folderId",1121594);
            }
            if("3".equals(line)){
                maps.put("folderId",1121594);
            }
            maps.put("secutyLevel",1);
            maps.put("version","1.0");
            maps.put("scope",Lists.newArrayList());
            maps.put("description",description);
            maps.put("token",mapWhere.get("token"));
            maps.put("gateWayHost",mapWhere.get("gateWayHost"));
            fileId_=addFile(maps);
            fileMap=queryFile(fileId,description,mapWhere,fileId_);
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            newFile1.delete();
            newFile2.delete();
        }
        //????????????????????????????????????
        DailyReportPo dailyReportPo = new DailyReportPo();
        if (ObjectUtils.isEmpty(dailyReportPo.getSort())) {//??????????????????,??????????????????????????????????????????+1?????????
            dailyReportPo.setSort(mapper.selectNextSort());
        }
        dailyReportPo.setReviewStatus("INIT");
        dailyReportPo.setPdfFile(fileMap.get("pdfFile"));
        dailyReportPo.setWordFile(fileMap.get("wordFile"));
        dailyReportPo.setReportType("1");
        dailyReportPo.setReportName(DateUtil.getDateFormat(date));
        dailyReportPo.setLine(line);
        dailyReportPo.setDescription((String) mapWhere.get("description"));
        dailyReportPo.setCreator(userId);
        dailyReportPo.setFileId(fileId_);
        dailyReportPo.setLoadId(fileId);
        this.insert(dailyReportPo);
        DailyReportVo dailyReportVo=dozerMapper.map(dailyReportPo, DailyReportVo.class);
        return dailyReportVo;
    }

    private void generateFaultWord(String date, String line, Map<String, Object> params) {
        Map<String,Object>map=Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        map.put("yearMonth",date.substring(0,7));
        FaultVo faultVo=faultService.queryFault(map);
        if(!ObjectUtils.isEmpty(faultVo)){
            if (!ObjectUtils.isEmpty(faultVo.getGzbrcl())) {
                params.put("gzbrcl",faultVo.getGzbrcl().toString());
            }else{
                params.put("gzbrcl","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrgd())) {
                params.put("gzbrgd",faultVo.getGzbrgd().toString());
            }else{
                params.put("gzbrgd","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrxh())) {
                params.put("gzbrxh",faultVo.getGzbrxh().toString());
            }else{
                params.put("gzbrxh","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrtx())) {
                params.put("gzbrtx",faultVo.getGzbrtx().toString());
            }else{
                params.put("gzbrtx","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrgj())) {
                params.put("gzbrgj",faultVo.getGzbrgj().toString());
            }else{
                params.put("gzbrgj","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrjd())) {
                params.put("gzbrjd",faultVo.getGzbrjd().toString());
            }else{
                params.put("gzbrjd","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrafx())) {
                params.put("gzbrafx",faultVo.getGzbrafx().toString());
            }else{
                params.put("gzbrafx","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrqt())) {
                params.put("gzbrqt",faultVo.getGzbrqt().toString());
            }else{
                params.put("gzbrqt","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbrhj())) {
                params.put("gzbrhj",faultVo.getGzbrhj().toString());
            }else{
                params.put("gzbrhj","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbycl())) {
                params.put("gzbycl",faultVo.getGzbycl().toString());
            }else{
                params.put("gzbycl","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbygd())) {
                params.put("gzbygd",faultVo.getGzbygd().toString());
            }else{
                params.put("gzbygd","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbyxh())) {
                params.put("gzbyxh",faultVo.getGzbyxh().toString());
            }else{
                params.put("gzbyxh","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbytx())) {
                params.put("gzbytx",faultVo.getGzbytx().toString());
            }else{
                params.put("gzbytx","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbygj())) {
                params.put("gzbygj",faultVo.getGzbygj().toString());
            }else{
                params.put("gzbygj","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbyjd())) {
                params.put("gzbyjd",faultVo.getGzbyjd().toString());
            }else{
                params.put("gzbyjd","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbyafx())) {
                params.put("gzbyafx",faultVo.getGzbyafx().toString());
            }else{
                params.put("gzbyafx","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbyqt())) {
                params.put("gzbyqt",faultVo.getGzbyqt().toString());
            }else{
                params.put("gzbyqt","0");
            }
            if (!ObjectUtils.isEmpty(faultVo.getGzbyhj())) {
                params.put("gzbyhj",faultVo.getGzbyhj().toString());
            }else{
                params.put("gzbyhj","0");
            }
        }else{
            params.put("gzbrcl","--");
            params.put("gzbrgd","--");
            params.put("gzbrxh","--");
            params.put("gzbrtx","--");
            params.put("gzbrgj","--");
            params.put("gzbrjd","--");
            params.put("gzbrafx","--");
            params.put("gzbrqt","--");
            params.put("gzbrhj","--");

            params.put("gzbycl","--");
            params.put("gzbygd","--");
            params.put("gzbyxh","--");
            params.put("gzbytx","--");
            params.put("gzbygj","--");
            params.put("gzbyjd","--");
            params.put("gzbyafx","--");
            params.put("gzbyqt","--");
            params.put("gzbyhj","--");
        }

        //?????????????????????????????????
        //?????????????????????7???-??????7???????????????????????????????????????
        //?????????????????????7???????????????????????????????????????
        List<Integer> faultIds = Lists.newArrayList();
        if(ObjectUtils.isEmpty(faultVo))
        {
            faultIds.add(0);
        }
        else
        {
            faultIds.add(faultVo.getId());
        }
        map.put("faultIds",faultIds);
        List<FaultDailyProblemVo> faultDailyProblemVos = faultProblemService.queryFaultDailyProblem(map);
        StringBuilder StringBuilder = new StringBuilder();
        if(faultDailyProblemVos.size() == 0){
            params.put("gzxzwt","???????????????????????????");
        }else{
            for(int i=0;i < faultDailyProblemVos.size();i++){
                FaultDailyProblemVo vo = faultDailyProblemVos.get(i);
                StringBuilder.append((i+1)+"???"+ vo.getProblemDesc());//1???????????????
                StringBuilder.append("\n");
                List<FaultDailyProblemDealVo> faultDailyProblemDealVos = faultProblemService.queryFaultDailyProblemDealVo(vo.getId());
                if(faultDailyProblemDealVos.size() == 0){
                    StringBuilder.append("?????????????????????");
                    StringBuilder.append("\n");
                }else{
                    for(int j=0;j < faultDailyProblemDealVos.size();j++){
                        FaultDailyProblemDealVo deal = faultDailyProblemDealVos.get(j);
                        StringBuilder.append(deal.getDealDetail());//??????+????????????
                        //StringBuilder.append(deal.getRecordTime() + " " + deal.getDealDetail());//??????+????????????
                        StringBuilder.append("\n");
                    }
                }
                //????????????+????????????
                StringBuilder.append(SzxmEnumsUtil.ProblemStatusEnum.getNameByCode(vo.getProblemStatus()))
                        .append("??????????????????");
                if(!ObjectUtils.isEmpty(vo.getProblemReason())){
                    StringBuilder.append(vo.getProblemReason());
                } else {
                    StringBuilder.append("--");
                }
                StringBuilder.append("\n");
                if(!ObjectUtils.isEmpty(vo.getProblemRemark())){
                    StringBuilder.append("?????????" + vo.getProblemRemark());//????????????
                }
                //StringBuilder.append("\n");
                StringBuilder.append("---------------------------------------------------------------------------------------------------------------------------------------");
                StringBuilder.append("\n");
            }
            params.put("gzxzwt",StringBuilder.toString());
            logger.info("gzxzwt:" + StringBuilder.toString());
        }

        //????????????????????????????????????????????????????????????????????????????????????????????????
        //?????????5.31???????????????????????????????????????5.30???????????????????????????????????????????????????????????????
        List<FaultVo> faultVoList = faultService.queryLeftOverVo(map);//??????????????????????????????????????????????????????????????????
        if(faultVoList == null || faultVoList.size() == 0){
            params.put("gzylwt","??????????????????");
        } else {
            List<Integer> faultIdArray = Lists.newArrayList();
            for(FaultVo vo : faultVoList){
                faultIdArray.add(vo.getId());
            }
            map.put("faultIds",faultIdArray);
            List<FaultDailyProblemVo> faultLeftOverVoList = faultProblemService.queryLeftOverProblem(map);
            StringBuilder StringBuilders = new StringBuilder();
            if(faultLeftOverVoList.size() == 0){
                params.put("gzxzwt","??????????????????");
            }else{
                for(int i=0;i < faultLeftOverVoList.size();i++){
                    FaultDailyProblemVo vo = faultLeftOverVoList.get(i);
                    StringBuilders.append((i+1)+"???"+ vo.getProblemDesc());//1???????????????
                    StringBuilders.append("\n");
                    List<FaultDailyProblemDealVo> faultLeftOverDealVoList = faultProblemService.queryFaultDailyProblemDealVo(vo.getId());
                    if(faultLeftOverDealVoList.size() == 0){
                        StringBuilders.append("?????????????????????");
                        StringBuilders.append("\n");
                    }else{
                        for(int j=0;j < faultLeftOverDealVoList.size();j++){
                            FaultDailyProblemDealVo deal = faultLeftOverDealVoList.get(j);
                            StringBuilders.append(deal.getDealDetail());//??????+????????????
                            //StringBuilders.append(deal.getRecordTime() + " " + deal.getDealDetail());//??????+????????????
                            StringBuilders.append("\n");
                        }
                    }
                    //????????????+????????????
                    StringBuilders.append(SzxmEnumsUtil.ProblemStatusEnum.getNameByCode(vo.getProblemStatus()))
                            .append("??????????????????");
                    if(!ObjectUtils.isEmpty(vo.getProblemReason())){
                        StringBuilders.append(vo.getProblemReason());
                    } else {
                        StringBuilders.append("--");
                    }
                    StringBuilders.append("\n");
                    if(!ObjectUtils.isEmpty(vo.getProblemRemark())){
                        StringBuilders.append("?????????" + vo.getProblemRemark());//????????????
                    }
                    //StringBuilders.append("\n");
                    StringBuilders.append("---------------------------------------------------------------------------------------------------------------------------------------");
                    StringBuilders.append("\n");
                }
                params.put("gzylwt",StringBuilders.toString());
                logger.info("gzylwt:" + StringBuilders.toString());
            }
        }
    }

    private void generateConstructionWord(String date, String line, Map<String, Object> params) {
        Map<String,Object>map=Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        ConstructionDailyPo vo=constructionService.queryConstructionDailyPo(map);
        if(!ObjectUtils.isEmpty(vo)){
            if (!ObjectUtils.isEmpty(vo.getWeeklyPlanConstructionQuantity())) {
                params.put("zjhsgs",vo.getWeeklyPlanConstructionQuantity().toString());
            }else{
                params.put("zjhsgs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getWeeklyActualConstructionQuantity())) {
                params.put("zjhsjwcs",vo.getWeeklyActualConstructionQuantity().toString());
            }else{
                params.put("zjhsjwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getDailyPlanConstructionQuantity())) {
                params.put("rbcjhsgs",vo.getDailyPlanConstructionQuantity().toString());
            }else{
                params.put("rbcjhsgs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getDailyActualConstructionQuantity())) {
                params.put("rbcjhsjwcs",vo.getDailyActualConstructionQuantity().toString());
            }else{
                params.put("rbcjhsjwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getTempPlanConstructionQuantity())) {
                params.put("lsbxjhwcs",vo.getTempPlanConstructionQuantity().toString());
            }else{
                params.put("lsbxjhwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getTempActualConstructionQuantity())) {
                params.put("lsbxsjwcs",vo.getTempActualConstructionQuantity().toString());
            }else{
                params.put("lsbxsjwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getRepairPlanConstructionQuantity())) {
                params.put("jxshd",vo.getRepairPlanConstructionQuantity().toString());
            }else{
                params.put("jxshd","0");
            }
            if (!ObjectUtils.isEmpty(vo.getRepairActualConstructionQuantity())) {
                params.put("jxsgdsjwcs",vo.getRepairActualConstructionQuantity().toString());
            }else{
                params.put("jxsgdsjwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getTotalPlanConstructionQuantity())) {
                params.put("jhzs",vo.getTotalPlanConstructionQuantity().toString());
            }else{
                params.put("jhzs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getTotalActualConstructionQuantity())) {
                params.put("jhzssjwcs",vo.getTotalActualConstructionQuantity().toString());
            }else{
                params.put("jhzssjwcs","0");
            }
            if (!ObjectUtils.isEmpty(vo.getDescription())) {
                params.put("shqksm",vo.getDescription());
            }else{
                params.put("shqksm","???????????????");
            }
        }else{
            params.put("zjhsgs","--");
            params.put("zjhsjwcs","--");
            params.put("rbcjhsgs","--");
            params.put("rbcjhsjwcs","--");
            params.put("lsbxjhwcs","--");
            params.put("lsbxsjwcs","--");
            params.put("jxshd","--");
            params.put("jxsgdsjwcs","--");
            params.put("jhzs","--");
            params.put("jhzssjwcs","--");
            params.put("shqksm","???????????????");
        }
    }

    private void generateOperateWord(String date, String line, Map<String, Object> params) {
        Map<String,Object>map=Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        TrainDailyVo vo=trainDailyService.queryTrain(map);
        if(!ObjectUtils.isEmpty(vo)){
            Comparator<Object> comparator = Collator.getInstance(java.util.Locale.CHINA);
            if (StringHelper.isNotNullAndEmpty(vo.getLcsyqkyyc())) {
                String result = getSortResult(vo.getLcsyqkyyc(), comparator);
                params.put("lcsyqkyyc",result);
            }else{
                params.put("lcsyqkyyc","--");
            }
            if (StringHelper.isNotNullAndEmpty(vo.getLcsyqkbyc())) {
                String result = getSortResult(vo.getLcsyqkbyc(), comparator);
                params.put("lcsyqkbyc",result);
            }else{
                params.put("lcsyqkbyc","--");
            }
            if (StringHelper.isNotNullAndEmpty(vo.getLcsyqktsc())) {
                String result = getSortResult(vo.getLcsyqktsc(), comparator);
                params.put("lcsyqktsc",result);
            }else{
                params.put("lcsyqktsc","--");
            }
            if (StringHelper.isNotNullAndEmpty(vo.getLcsyqkjxc())) {
                String result = getSortResult(vo.getLcsyqkjxc(), comparator);
                params.put("lcsyqkjxc",result);
            }else{
                params.put("lcsyqkjxc","--");
            }
            if (StringHelper.isNotNullAndEmpty(vo.getLcsyqkqt())) {
                String result = getSortResult(vo.getLcsyqkqt(), comparator);
                params.put("lcsyqkqt",result);
            }else{
                params.put("lcsyqkqt","--");
            }
        }else{
            params.put("lcsyqkyyc","--");
            params.put("lcsyqkbyc","--");
            params.put("lcsyqktsc","--");
            params.put("lcsyqkjxc","--");
            params.put("lcsyqkqt","--");
        }
    }

    private void generateTrainWord(String date, String line, Map<String, Object> params) {
        Map<String,Object> map=Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        ScheduleVo vo=scheduleService.queryScheduleVo(map);
        if(!ObjectUtils.isEmpty(vo)){
            if (!ObjectUtils.isEmpty(vo.getDrskbbh())) {
                params.put("drskbbh",vo.getDrskbbh());
            }else{
                params.put("drskbbh","0");
            }
            if (!ObjectUtils.isEmpty(vo.getSfz())) {
                params.put("sfz",vo.getSfz());
            }else{
                params.put("sfz","0");
            }
            if (!ObjectUtils.isEmpty(vo.getZdz())) {
                params.put("zdz",vo.getZdz());
            }else{
                params.put("zdz","0");
            }
            if (!ObjectUtils.isEmpty(vo.getSfzsj())) {
                params.put("sfzsjd",DateUtil.getTimeFormat_(vo.getSfzsj()));
            }else{
                params.put("sfzsjd","0");
            }

            if (!ObjectUtils.isEmpty(vo.getZdzsj())) {
                params.put("zdzsj",DateUtil.getTimeFormat_(vo.getZdzsj()));
            }else{
                params.put("zdzsj","0");
            }
            if (!ObjectUtils.isEmpty(vo.getDrskbsxlcs().toString())) {
                params.put("drskbsxlcs",vo.getDrskbsxlcs());
            }else{
                params.put("drskbsxlcs","0");
            }
        }else{
            params.put("drskbbh","--");
            params.put("sfz","--");
            params.put("zdz","--");
            params.put("sfzsjd","--");
            params.put("zdzsj","--");
            params.put("drskbsxlcs","--");
        }
    }

    //??????pdf???word
    private Map<String, String> queryFile(Integer fileId,String description,Map<String, Object> mapWhere,
                                          Integer fileId_) throws IOException {
        Map<String, String> map=Maps.newHashMap();
        if(description.equals("???????????????")){
            HttpHeaders headers =docFileQuartz();
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            RestTemplate restTemplate=new RestTemplate();
            ResponseEntity<CommonResult> responseEntity = restTemplate.exchange(dcCommonUtil.getDocUrl()+"/file/" +fileId+"/info?startContent=".toString(), HttpMethod.GET, requestEntity, CommonResult.class);
            if (HttpStatus.OK == responseEntity.getStatusCode()) {
                CommonResult commonResult = responseEntity.getBody();
                Map<String,Object>m= (Map<String, Object>) commonResult.getData();
                //???????????????
//                if(m.get("fileViewUrl").toString().equals(dcCommonUtil.getDocPdfError())){
//                    logger.info("??????????????????????????????????????????????????????pdf???????????????????????????????????????????????????????????????????????????");
//                    System.out.println("??????????????????????????????????????????????????????pdf???????????????????????????????????????????????????????????????????????????");
//                    List<Map<String,Integer>> mapIds=Lists.newArrayList();
//                    Map<String,Integer>p=Maps.newHashMap();
//                    p.put("id",0);p.put("fileId",fileId_);
//                    mapIds.add(p);
//                    this.deleteReport(mapIds);
//                    this.generateDailyWorkReportFile(mapWhere);
//                    return null ;
//                }
                map.put("pdfFile",m.get("fileViewUrl").toString());
                map.put("wordFile",m.get("fileUrl").toString());
            }
        }else {
            HttpHeaders headers=docFileUser(mapWhere);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<CommonResult> responseEntity = restTemplate.exchange(dcCommonUtil.getDocUrl() + "/file/"+fileId+"/info?startContent=".toString(), HttpMethod.GET, requestEntity, CommonResult.class);
            if (HttpStatus.OK == responseEntity.getStatusCode()) {
                CommonResult commonResult = responseEntity.getBody();
                Map<String,Object>m= (Map<String, Object>) commonResult.getData();
                //???????????????
//                if(m.get("fileViewUrl").toString().equals(dcCommonUtil.getDocPdfError())){
//                    logger.info("??????????????????????????????????????????????????????pdf???????????????????????????????????????????????????????????????????????????");
//                    System.out.println("??????????????????????????????????????????????????????pdf???????????????????????????????????????????????????????????????????????????");
//                    List<Map<String,Integer>> mapIds=Lists.newArrayList();
//                    Map<String,Integer>p=Maps.newHashMap();
//                    p.put("id",0);p.put("fileId",fileId_);
//                    mapIds.add(p);
//                    this.deleteReport(mapIds);
//                    this.generateDailyWorkReportFile(mapWhere);
//                   return null;
//                }
                map.put("pdfFile",m.get("fileViewUrl").toString());
                map.put("wordFile",m.get("fileUrl").toString());
            }
        }
        logger.info("=======map:" + map);
        return  map;
    }

    private Map<String,Object> uploadFile(FileSystemResource fileSystemResource,String description,Map<String, Object> mp)
    {
        if(description.equals("???????????????")){
            HttpHeaders headers =docFileQuartzLoad();
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", fileSystemResource);
            form.add("filename",fileSystemResource.getFilename());
            HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form,headers);
            RestTemplate restTemplate=new RestTemplate();
            Map<String,Object> returnMap = restTemplate.postForObject(dcCommonUtil.getDocUrl()+"/file/upload", files, Map.class);
            return returnMap;
        }else {
            RestTemplate restTemplate=new RestTemplate();
            String gateWayHost=mp.get("gateWayHost").toString();//????????????????????????
            // ????????????
            HttpHeaders headers=docFileUserLoad(mp);
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", fileSystemResource);
            form.add("filename",fileSystemResource.getFilename());
            HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);
            Map<String,Object> returnMap = restTemplate.postForObject(dcCommonUtil.getDocUrl()+"/file/upload", files, Map.class);
            return returnMap;
        }
    }

    private HttpHeaders docFileQuartzLoad(){
        JwtAuthenticationRequest userVo=new JwtAuthenticationRequest();
        userVo.setUserName("weihuyuan");
        userVo.setPassword("123456");
        String token= commAuthService.createAuthenticationToken(userVo).getData();
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.set("Authorization",token);
        return headers;
    }

    private HttpHeaders docFileUserLoad(Map<String ,Object> map){
        String token=map.get("token").toString();//????????????Token
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        headers.setContentType(type);
        headers.set("Authorization", token);
        return headers;
    }

    private HttpHeaders docFileQuartz(){
        JwtAuthenticationRequest userVo=new JwtAuthenticationRequest();
        userVo.setUserName("weihuyuan");
        userVo.setPassword("123456");
        String token= commAuthService.createAuthenticationToken(userVo).getData();
        MediaType type = MediaType.parseMediaType("application/json");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.set("Authorization",token);
        return headers;
    }

    private HttpHeaders docFileUser(Map<String ,Object> map){
        String token=map.get("token").toString();//????????????Token
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json");
        headers.setContentType(type);
        headers.set("Authorization", token);
        return headers;
    }

    private Integer addFile(Map<String ,Object> map){
        if(((String)map.get("description")).equals("???????????????")){
            HttpHeaders headers =docFileQuartz();
            map.put("status","RELEASE");
            HttpEntity<Map<String, Object>> files = new HttpEntity<>(map, headers);
            RestTemplate restTemplate=new RestTemplate();
            Map<String,Object> s =restTemplate.postForObject(dcCommonUtil.getDocUrl()+"/corp/add", files, Map.class);
            logger.info("=====s:" + s);
            Map<String,Object>s1= (Map<String, Object>) s.get("data");
            Integer fId= (Integer) s1.get("id");
            docMapper.updateFileStatus(fId);
            return fId;
        }else {
            HttpHeaders headers=docFileUser(map);
            HttpEntity<Map<String, Object>> files = new HttpEntity<>(map, headers);
            RestTemplate restTemplate = new RestTemplate();
            Map<String,Object> s =restTemplate.postForObject(dcCommonUtil.getDocUrl()+"/corp/add", files, Map.class);
            logger.info("++++++s:" + s);
            Map<String,Object>s1= (Map<String, Object>) s.get("data");
            Integer fId= (Integer) s1.get("id");
            return fId;
        }
    }

    /**
     * ???????????????????????????????????????
     * @param time
     * @return
     */
    public List<DocFileVo> getDocFileNames(Date time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        //???????????????????????????.???????????????,??????????????????
        calendar.add(calendar.DATE,1);

        //????????????????????????????????????????????????
        time=calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String tomorrowStr = formatter.format(time);
        return mapper.getDocFileNames(DateUtil.getDateFormat(time),tomorrowStr);
    }

    /**
     * ????????????????????????????????????????????????
     * @param param
     * @param comparator
     * @return
     */
    private String getSortResult(String param, Comparator<Object> comparator){
        String result = "";
        String array[] = param.split(",");//?????????????????????????????????????????????????????????String????????????
        if(array.length > 1){//??????????????????????????????1???????????????????????????????????????
            List<String> list = Arrays.asList(array);
            list.sort((o1, o2) -> ((Collator) comparator).compare(o1, o2));
            for(String s : list){
                result += "/" + s;
            }
            result = result.substring(1);
        } else{
            result = param;
        }
        return result;
    }

    /**
     * ??????????????????
     * @param date
     * @param line
     * @param params
     */
    private void generateTipsWord(String date, String line, Map<String, Object> params) {
        Map<String,Object> map = Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        List<DailyRepresentationVo> dailyRepresentationVoList = dailyRepresentationService.queryDailyRepresentationList(map);
        if(dailyRepresentationVoList == null || dailyRepresentationVoList.size() == 0){
            params.put("qtqksmbt","??????????????????");
            params.put("qtqksmms", " ");//??????????????????????????????????????????????????????
        } else {
            DailyRepresentationVo vo = dailyRepresentationVoList.get(0);
            if(!ObjectUtils.isEmpty(vo)){
                if(!ObjectUtils.isEmpty(vo.getTitle())){
                    params.put("qtqksmbt",vo.getTitle());
                } else {
                    params.put("qtqksmbt","??????????????????");
                }
                if(!ObjectUtils.isEmpty(vo.getTitle())){
                    params.put("qtqksmms",vo.getDescribe());
                } else {
                    params.put("qtqksmms"," ");//??????????????????????????????????????????????????????
                }
            } else {
                params.put("qtqksmbt","??????????????????");
                params.put("qtqksmms", " ");
            }
        }
    }
    private String getOperateTimes(String line,Map<String, Object>mapWhere) throws Exception {
        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        calendar.setTime(sdf.parse(mapWhere.get("date").toString()));
        calendar.set(Calendar.DATE,calendar.get(Calendar.DATE)-1);
        Date date=sdf.parse(DateUtil.getDateFormat(hrbSysService.queryLineFoundationList().stream().filter(e-> StringUtils.equalsIgnoreCase(e.getLine(),line)).collect(Collectors.toList()).get(0).getOperationTime()));
        long timeMillion= DateUtil.getDateFormat(sdf.format(calendar.getTime())).getTime()-date.getTime();
        return String.valueOf((timeMillion/(24l*60*60*1000)));
    }
}
