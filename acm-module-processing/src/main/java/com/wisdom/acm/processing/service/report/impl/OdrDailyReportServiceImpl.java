package com.wisdom.acm.processing.service.report.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wisdom.acm.processing.common.*;
import com.wisdom.acm.processing.common.officeUtils.WordUtil;
import com.wisdom.acm.processing.controller.OdrDailyReportController;
import com.wisdom.acm.processing.mapper.report.DailyReportMapper;
import com.wisdom.acm.processing.mapper.report.DocMapper;
import com.wisdom.acm.processing.po.report.DailyReportPo;
import com.wisdom.acm.processing.service.energy.EnergyService;
import com.wisdom.acm.processing.service.report.OdrDailyReportService;
import com.wisdom.acm.processing.service.representation.DailyRepresentationService;
import com.wisdom.acm.processing.service.traffic.TrafficLineDailyService;
import com.wisdom.acm.processing.service.traffic.TrafficLineTicketDailyService;
import com.wisdom.acm.processing.service.traffic.TrafficPeakLineDailyService;
import com.wisdom.acm.processing.service.train.TrainDailyService;
import com.wisdom.acm.processing.service.weather.WeatherService;
import com.wisdom.acm.processing.vo.energy.EnergyVo;
import com.wisdom.acm.processing.vo.report.DailyReportVo;
import com.wisdom.acm.processing.vo.report.DocFileVo;
import com.wisdom.acm.processing.vo.representation.DailyRepresentationVo;
import com.wisdom.acm.processing.vo.traffic.TrafficLineDailyVo;
import com.wisdom.acm.processing.vo.traffic.TrafficLineTicketDailyVo;
import com.wisdom.acm.processing.vo.traffic.TrafficPeakLineDailyVo;
import com.wisdom.acm.processing.vo.train.TrainVo;
import com.wisdom.base.common.aspect.AddLog;
import com.wisdom.base.common.dc.feign.DcBaseService;
import com.wisdom.base.common.dc.util.EnumsUtil;
import com.wisdom.base.common.dc.vo.base.BaseBoVo;
import com.wisdom.base.common.enums.LoggerModuleEnum;
import com.wisdom.base.common.feign.CommAuthService;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.feign.HrbSysService;
import com.wisdom.base.common.feign.doc.CommDocService;
import com.wisdom.base.common.log.AcmLogger;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ResourceUtil;
import com.wisdom.base.common.vo.DictionaryVo;
import com.wisdom.base.common.vo.JwtAuthenticationRequest;
import com.wisdom.base.common.vo.LineFoundationVo;
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

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zll
 * 2020/8/11/011 15:46
 * Description:<??????>
 */
@Service
@Slf4j
public class OdrDailyReportServiceImpl extends BaseService<DailyReportMapper, DailyReportPo> implements OdrDailyReportService {
    @Autowired
    private TrafficLineDailyService trafficLineDailyService;
    @Autowired
    private EnergyService energyService;
    @Autowired
    private CommDocService commDocService;
    @Autowired
    private TrafficLineTicketDailyService trafficLineTicketDailyService;
    @Autowired
    private TrafficPeakLineDailyService trafficPeakLineDailyService;
    @Autowired
    private DcBaseService dcBaseService;
    @Autowired
    private CommAuthService commAuthService;
    @Autowired
    private DocMapper docMapper;
    @Autowired
    private CommDictService commDictService;
    @Autowired
    private DcCommonUtil dcCommonUtil;
    @Autowired
    private TrainDailyService trainDailyService;
    @Autowired
    private HrbSysService hrbSysService;
    @Autowired
    private DailyRepresentationService dailyRepresentationService;
    @Autowired
    private WeatherService weatherService;

    private static final Logger logger = LoggerFactory.getLogger(OdrDailyReportServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    //@Async("odr_report")
    @AddLog(title = "??????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateDailyWorkReportFile(Map<String, Object> mapWhere
            ,UserInfo user) {
        System.out.println("????????????????????????:  "+Thread.currentThread().getName());
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
            this.setAcmLogger(new AcmLogger("??????1?????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line1").toString(),
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
            this.setAcmLogger(new AcmLogger("??????3?????????????????????????????? "+DateUtil.getDateFormat(new Date()),mapWhere.get("date").toString(),mapWhere.get("line3").toString(),
                        dailyReportVo.getId().toString(),"INIT"));
            return dailyReportVo;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Async("odr_report")
    @AddLog(title = "??????????????????1??????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateDailyWorkReportFile1(Map<String, Object> maps){
        System.out.println("????????????????????????:  "+Thread.currentThread().getName());
        //1????????????
        Map<String,Object>map=Maps.newHashMap();
        map.put("date",maps.get("date"));
        map.put("line1",maps.get("line1"));
        map.put("description",maps.get("description"));
        map.put("keys",maps.get("keys"));
        map.put("token",maps.get("token"));
        map.put("gateWayHost",maps.get("gateWayHost"));
        DailyReportVo dailyReportVo= main(map);
        this.setAcmLogger(new AcmLogger("?????????????????????--1?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line1").toString(),
                    dailyReportVo.getId().toString(),"INIT"));
        return dailyReportVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Async("odr_report")
    @AddLog(title = "??????????????????3??????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
    public DailyReportVo generateDailyWorkReportFile3(Map<String, Object> maps) {
        System.out.println("????????????????????????:  "+Thread.currentThread().getName());
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
        this.setAcmLogger(new AcmLogger("?????????????????????--3?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line3").toString(),
                    dailyReportVo.getId().toString(),"INIT"));
        return dailyReportVo;
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
//    @Async("odr_report")
//    @AddLog(title = "??????????????????????????????????????????",module = LoggerModuleEnum.ODR_REPORT,initContent = true)
//    public List<DailyReportVo> generateDailyWorkReportFile(Map<String, Object> maps) throws IOException {
//        System.out.println("????????????????????????:  "+Thread.currentThread().getName());
//        List<DailyReportVo> dailyReportVos=Lists.newArrayList();
//        //1????????????
//        if(!ObjectUtils.isEmpty(maps.get("line1"))){
//            Map<String,Object>map=Maps.newHashMap();
//            map.put("date",maps.get("date"));
//            map.put("line1",maps.get("line1"));
//            map.put("description",maps.get("description"));
//            map.put("keys",maps.get("keys"));
//            map.put("token",maps.get("token"));
//            map.put("gateWayHost",maps.get("gateWayHost"));
//            DailyReportVo dailyReportVo= main(map);
//            dailyReportVos.add(dailyReportVo);
//            if("???????????????".equals(maps.get("description").toString())){
//                this.setAcmLogger(new AcmLogger("?????????????????????--1?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line1").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }else{
//                this.setAcmLogger(new AcmLogger("??????1?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line1").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }
//        }
//
//        //2????????????????????????
//        //3????????????
//        if(!ObjectUtils.isEmpty(maps.get("line3"))){
//            Map<String,Object>map=Maps.newHashMap();
//            map.put("date",maps.get("date"));
//            map.put("line3",maps.get("line3"));
//            map.put("description",maps.get("description"));
//            map.put("keys",maps.get("keys"));
//            map.put("token",maps.get("token"));
//            map.put("gateWayHost",maps.get("gateWayHost"));
//            DailyReportVo dailyReportVo= main(map);
//            dailyReportVos.add(dailyReportVo);
//            if("???????????????".equals(maps.get("description").toString())){
//                this.setAcmLogger(new AcmLogger("?????????????????????--3?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line3").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }else{
//                this.setAcmLogger(new AcmLogger("??????3?????????????????????????????? "+DateUtil.getDateFormat(new Date()),maps.get("date").toString(),maps.get("line3").toString(),
//                        dailyReportVo.getId().toString(),"INIT"));
//            }
//        }
//        return dailyReportVos;
//    }

    private DailyReportVo main(Map<String, Object> mapWhere){
        String str="??????????????????";
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
        List<String> keyAll=Arrays.asList("1","2","3","4","5");
        List<String> keyContains= (List<String>) mapWhere.get("keys");
        List<String> keyNoContains=Lists.newArrayList();
        for(String strs:keyAll){
            if(!keyContains.contains(strs)){
                keyNoContains.add(strs);
            }
        }
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
        //??????????????????
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("line", line);
        params.put("ts", ts);
        params.put("drtq", drtq);
        params.put("rq", date+"  ("+DateUtil.getWeek(DateUtil.getDateFormat(date))+")");
        params.put("tbdw", "????????????");
        params.put("tbr", str);
        //params.put("shr", "?????????");
        params.put("tbrq", DateUtil.formateNyrsfm(new Date()));
        //??????keys???????????????????????????word????????????
        for(String key: keyContains){
            //?????????
            if("1".equals(key)){
                generateTrafficWord(date,line,params);
            }
            //??????????????????
            if("2".equals(key)){
                generateTrainWord(date,line,params);
            }
            //??????????????????
            if("3".equals(key)){
                generateTrainZDWord(date,line,params);
            }
            //????????????
            if("4".equals(key)){
                generateEnegyWord(date,line,params);
            }
        }
        //??????word??????
        Integer fileId_ = null;
        Integer fileId = null;
        File newFile1 = null;
        File newFile2 = null;
        Map<String,String>fileMap=Maps.newHashMap();
        try {
            InputStream fileInputStrem= ResourceUtil.findResoureFile("template/yyrbmb.docx");
            //??????word???????????????
            XWPFDocument document= new XWPFDocument(fileInputStrem);
            //?????????????????????????????????word???fileInputStrem?????????????????????????????????????????????????????????????????????word
            //??????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            System.out.println("user.dir : "+System.getProperty("user.dir"));
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
                file = "/????????????????????????" +line+"??????-"+ DateUtil.getNowTimeByChina(new Date()) + ".docx";
            }else{
                file = "/????????????" +line+"??????-"+ DateUtil.getNowTimeByChina(new Date()) + ".docx";
            }
            String newFileName=file.substring(1);
            //???????????????????????????????????????????????????
            if(getDocFileNames(DateUtil.getDateFormat(date)).stream().filter(e->e.getFileName().
                    equals(newFileName)).distinct().collect(Collectors.toList()).size()!=0){
                Thread.sleep(1000);
                if(((String)mapWhere.get("description")).equals("???????????????")){
                    file = "/????????????????????????" +line+"??????-"+ DateUtil.getNowTimeByChina(new Date()) + ".docx";
                }else{
                    file = "/????????????" +line+"??????-"+ DateUtil.getNowTimeByChina(new Date()) + ".docx";
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
                    maps.put("docTitle","????????????_1???????????????????????????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","????????????_2???????????????????????????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","????????????_3???????????????????????????");
                }
            }else{
                if("1".equals(line)){
                    maps.put("docTitle","????????????_1??????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","????????????_2??????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","????????????_3??????");
                }
            }
            //maps.put("docNum",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+System.currentTimeMillis());
            maps.put("docNum",System.currentTimeMillis());
            //?????????????????????
            if("1".equals(line)){
                maps.put("folderId",1121588);
            }
            if("2".equals(line)){
                maps.put("folderId",1121590);
            }
            if("3".equals(line)){
                maps.put("folderId",1121592);
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
        //??????????????????????????????
        DailyReportPo dailyReportPo = new DailyReportPo();
        if (ObjectUtils.isEmpty(dailyReportPo.getSort())) {//??????????????????,??????????????????????????????????????????+1?????????
            dailyReportPo.setSort(mapper.selectNextSort());
        }
        dailyReportPo.setReviewStatus("INIT");
        dailyReportPo.setReportType("0");
        dailyReportPo.setReportName(DateUtil.getDateFormat(date));
        dailyReportPo.setPdfFile(fileMap.get("pdfFile"));
        dailyReportPo.setWordFile(fileMap.get("wordFile"));
        dailyReportPo.setLine(line);
        dailyReportPo.setDescription((String) mapWhere.get("description"));
        dailyReportPo.setCreator(206);
        dailyReportPo.setFileId(fileId_);
        dailyReportPo.setLoadId(fileId);
        this.insert(dailyReportPo);
        DailyReportVo dailyReportVo=dozerMapper.map(dailyReportPo, DailyReportVo.class);
        return dailyReportVo;
    }

    private DailyReportVo main(Map<String, Object> mapWhere,UserInfo user) {
        Integer userId=user.getId();
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
        List<String> keyAll=Arrays.asList("1","2","3","4","5");
        List<String> keyContains= (List<String>) mapWhere.get("keys");
        List<String> keyNoContains=Lists.newArrayList();
        for(String strs:keyAll){
            if(!keyContains.contains(strs)){
                keyNoContains.add(strs);
            }
        }
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
        //??????????????????
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("line", line);
        params.put("ts", ts);
        params.put("drtq", drtq);
        params.put("rq", date+"  ("+DateUtil.getWeek(DateUtil.getDateFormat(date))+")");
        params.put("tbdw", "????????????");
        params.put("tbr", user.getActuName());
        //params.put("shr", "?????????");
        params.put("tbrq", DateUtil.formateNyrsfm(new Date()));
        //??????keys???????????????????????????word????????????
        for(String key: keyContains){
            //?????????
            if("1".equals(key)){
                generateTrafficWord(date,line,params);
            }
            //??????????????????
            if("2".equals(key)){
                generateTrainWord(date,line,params);
            }
            //??????????????????
            if("3".equals(key)){
                generateTrainZDWord(date,line,params);
            }
            //????????????
            if("4".equals(key)){
                generateEnegyWord(date,line,params);
            }
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
        //??????word??????
        Integer fileId_ = null;
        Integer fileId = null;
        File newFile1 = null;
        File newFile2 = null;
        Map<String,String>fileMap=Maps.newHashMap();
        try {
            InputStream fileInputStrem= ResourceUtil.findResoureFile("template/yyrbmb.docx");
            //??????word???????????????
            XWPFDocument document= new XWPFDocument(fileInputStrem);
            //?????????????????????????????????word???fileInputStrem?????????????????????????????????????????????????????????????????????word
            //??????????????????
            String tempFlolderStr = System.getProperty("user.dir") + "/temporary";
            System.out.println("user.dir : "+System.getProperty("user.dir"));
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
                file = "/????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date()) + ".docx";
            }else{
                file = "/????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
            }
            String newFileName=file.substring(1);
            //???????????????????????????????????????????????????
            if(getDocFileNames(DateUtil.getDateFormat(date)).stream().filter(e->e.getFileName().
                    equals(newFileName)).distinct().collect(Collectors.toList()).size()!=0){
                Thread.sleep(1000);
                if(((String)mapWhere.get("description")).equals("???????????????")){
                    file = "/????????????????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date()) + ".docx";
                }else{
                    file = "/????????????" +line+"??????-" +DateUtil.getNowTimeByChina(new Date())+ ".docx";
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
            logger.info("======mapWhere:"+mapWhere);
            Map<String,Object> returnMap=uploadFile(fileSystemResource,description,mapWhere);
            Map<String,Object> map= (Map<String, Object>) returnMap.get("data");
            fileId= (Integer) map.get("id");
            Map<String,Object> maps= Maps.newHashMap();
            maps.put("fileId",fileId);
            maps.put("author",user.getActuName());
            if(description.equals("???????????????")){
                if("1".equals(line)){
                    maps.put("docTitle","????????????_1???????????????????????????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","????????????_2???????????????????????????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","????????????_3???????????????????????????");
                }
            }else{
                if("1".equals(line)){
                    maps.put("docTitle","????????????_1??????");
                }
                if("2".equals(line)){
                    maps.put("docTitle","????????????_2??????");
                }
                if("3".equals(line)){
                    maps.put("docTitle","????????????_3??????");
                }
            }
            //maps.put("docNum",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+System.currentTimeMillis());
            maps.put("docNum",System.currentTimeMillis());
            //?????????????????????
            if("1".equals(line)){
                maps.put("folderId",1121588);
            }
            if("2".equals(line)){
                maps.put("folderId",1121590);
            }
            if("3".equals(line)){
                maps.put("folderId",1121592);
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
        //??????????????????????????????
        DailyReportPo dailyReportPo = new DailyReportPo();
        if (ObjectUtils.isEmpty(dailyReportPo.getSort())) {//??????????????????,??????????????????????????????????????????+1?????????
            dailyReportPo.setSort(mapper.selectNextSort());
        }
        dailyReportPo.setReviewStatus("INIT");
        dailyReportPo.setReportType("0");
        dailyReportPo.setReportName(DateUtil.getDateFormat(date));
        dailyReportPo.setPdfFile(fileMap.get("pdfFile"));
        dailyReportPo.setWordFile(fileMap.get("wordFile"));
        dailyReportPo.setLine(line);
        dailyReportPo.setDescription((String) mapWhere.get("description"));
        dailyReportPo.setCreator(userId);
        dailyReportPo.setFileId(fileId_);
        dailyReportPo.setLoadId(fileId);
        this.insert(dailyReportPo);
        DailyReportVo dailyReportVo=dozerMapper.map(dailyReportPo, DailyReportVo.class);
        return dailyReportVo;
    }

    private void generateEnegyWord(String date, String line, Map<String, Object> params) {
        Map<String,Object>map=Maps.newHashMap();
        map.put("line",line);
        map.put("recordTime",date);
        //??????????????????????????????????????????????????????????????????????????????????????????????????????0???
        Date dailyDate = DateUtil.formatDate(date, "yyyy-MM-dd");
        //????????????
        Calendar calender = Calendar.getInstance();
        calender.setTime(dailyDate);
        int year = calender.get(Calendar.YEAR);
        int month = calender.get(Calendar.MONTH)+1;
        String monthlyTime = year+"-"+month+"-"+"01"+" 00:00:00";
        map.put("monthlyTime", monthlyTime);
        EnergyVo dailyVo=energyService.queryEnergyDailyVo(map);
        if(!ObjectUtils.isEmpty(dailyVo)){
            if (!ObjectUtils.isEmpty(dailyVo.getDrdz())) {
                params.put("drdz",dailyVo.getDrdz());
            }else{
                params.put("drdz","0");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getDrqy())) {
                params.put("drqy",dailyVo.getDrqy());
            }else{
                params.put("drqy","0");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getDrsh())) {
                params.put("drsh",dailyVo.getDrsh());
            }else{
                params.put("drsh","0");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getDzbl())) {
                params.put("dzbl",String.valueOf(new BigDecimal(dailyVo.getDzbl()).setScale
                        (2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            }else{
                params.put("dzbl","0.00");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getQybl())) {
                params.put("qybl",String.valueOf(new BigDecimal(dailyVo.getQybl()).setScale
                        (2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            }else{
                params.put("qybl","0.00");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getShbl())) {
                params.put("shbl",String.valueOf(new BigDecimal(dailyVo.getShbl()).setScale
                        (2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            }else{
                params.put("shbl","0.00");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getDzdl())) {
                params.put("dzdl",dailyVo.getDzdl());
            }else{
                params.put("dzdl","0");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getMrdqydhzklc())) {
                BigDecimal mrdqydhzklc=new BigDecimal(dailyVo.getMrdqydhzklc());
                params.put("mrdqydhzklc",String.valueOf(mrdqydhzklc.setScale
                        (2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            }else{
                params.put("mrdqydhzklc","0");
            }
            if (!ObjectUtils.isEmpty(dailyVo.getQydhyylc())) {
                BigDecimal qydhyylc=new BigDecimal(dailyVo.getQydhyylc());
                params.put("qydhyylc",String.valueOf(qydhyylc.setScale
                        (2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            }else{
                params.put("qydhyylc","0");
            }
        }else{
            params.put("drdz","--");
            params.put("drqy","--");
            params.put("drsh","--");
            params.put("dzbl","--");
            params.put("qybl","--");
            params.put("shbl","--");
            params.put("dzdl","--");
            params.put("qydhyylc","--");
            params.put("mrdqydhzklc","--");
        }
        EnergyVo monthVo=energyService.queryEnergyMonthlyVo(map);
        if(!ObjectUtils.isEmpty(monthVo)){
        	if (!ObjectUtils.isEmpty(monthVo.getBydzzdh())) {
                params.put("bydzzdh",monthVo.getBydzzdh());
            }else{
                params.put("bydzzdh","0");
            }
            if (!ObjectUtils.isEmpty(monthVo.getByqyzdh())) {
                params.put("byqyzdh",monthVo.getByqyzdh());
            }else{
                params.put("byqyzdh","0");
            }
            if (!ObjectUtils.isEmpty(monthVo.getByljsh())) {
                params.put("byljsh",monthVo.getByljsh());
            }else{
                params.put("byljsh","0");
            }
            if (!ObjectUtils.isEmpty(monthVo.getYljzdl())) {
                params.put("yljzdl",monthVo.getYljzdl());
            }else{
                params.put("yljzdl","0");
            }
        }else{
        	params.put("bydzzdh","--");
            params.put("byqyzdh","--");
            params.put("byljsh","--");
            params.put("yljzdl","--");
        }
    }

    private void generateTrainZDWord(String date, String line, Map<String, Object> params) {
        Map<String, Object> map=Maps.newHashMap();
        String year=date.substring(0,4);
        map.put("year",year);
        map.put("recordTime",date);
        map.put("line",line);
        List<TrainVo>  trainVos=trainDailyService.selectByParams(map);
        if(!ObjectUtils.isEmpty(trainVos)){
            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrsf25())) {
                params.put("brsf25",trainVos.get(0).getBrsf25());
            }else{
                params.put("brsf25","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrzd25())) {
                params.put("brzd25",trainVos.get(0).getBrzd25());
            }else{
                params.put("brzd25","0");
            }

            BigDecimal hjbr25=new BigDecimal(trainVos.get(0).getBrsf25()).
                    add(new BigDecimal(trainVos.get(0).getBrzd25()));
            params.put("hjbr25",hjbr25.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrsf510())) {
                params.put("brsf510",trainVos.get(0).getBrsf510());
            }else{
                params.put("brsf510","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrzd510())) {
                params.put("brzd510",trainVos.get(0).getBrzd510());
            }else{
                params.put("brzd510","0");
            }

            BigDecimal hjbr510=new BigDecimal(trainVos.get(0).getBrsf510()).
                    add(new BigDecimal(trainVos.get(0).getBrzd510()));
            params.put("hjbr510",hjbr510.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrsf1015())) {
                params.put("brsf1015",trainVos.get(0).getBrsf1015());
            }else{
                params.put("brsf1015","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrzd1015())) {
                params.put("brzd1015",trainVos.get(0).getBrzd1015());
            }else{
                params.put("brzd1015","0");
            }

            BigDecimal hjbr1015=new BigDecimal(trainVos.get(0).getBrsf1015()).
                    add(new BigDecimal(trainVos.get(0).getBrzd1015()));
            params.put("hjbr1015",hjbr1015.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrsf15())) {
                params.put("brsf15",trainVos.get(0).getBrsf15());
            }else{
                params.put("brsf15","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getBrzd15())) {
                params.put("brzd15",trainVos.get(0).getBrzd15());
            }else{
                params.put("brzd15","0");
            }

            BigDecimal hjbr15=new BigDecimal(trainVos.get(0).getBrsf15()).
                    add(new BigDecimal(trainVos.get(0).getBrzd15()));
            params.put("hjbr15",hjbr15.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getYsf25())) {
                params.put("ysf25",trainVos.get(0).getYsf25());
            }else{
                params.put("ysf25","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYwd25())) {
                params.put("ywd25",trainVos.get(0).getYwd25());
            }else{
                params.put("ywd25","0");
            }

            BigDecimal hjy25=new BigDecimal(trainVos.get(0).getYsf25()).
                    add(new BigDecimal(trainVos.get(0).getYwd25()));
            params.put("hjy25",hjy25.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getYsf510())) {
                params.put("ysf510",trainVos.get(0).getYsf510());
            }else{
                params.put("ysf510","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYwd510())) {
                params.put("ywd510",trainVos.get(0).getYwd510());
            }else{
                params.put("ywd510","0");
            }

            BigDecimal hjy510=new BigDecimal(trainVos.get(0).getYsf510()).
                    add(new BigDecimal(trainVos.get(0).getYwd510()));
            params.put("hjy510",hjy510.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getYsf1015())) {
                params.put("ysf1015",trainVos.get(0).getYsf1015());
            }else{
                params.put("ysf1015","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYwd1015())) {
                params.put("ywd1015",trainVos.get(0).getYwd1015());
            }else{
                params.put("ywd1015","0");
            }

            BigDecimal hjy1015=new BigDecimal(trainVos.get(0).getYsf1015()).
                    add(new BigDecimal(trainVos.get(0).getYwd1015()));
            params.put("hjy1015",hjy1015.toString());

            if (!ObjectUtils.isEmpty(trainVos.get(0).getYsf15())) {
                params.put("ysf15",trainVos.get(0).getYsf15());
            }else{
                params.put("ysf15","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYwd15())) {
                params.put("ywd15",trainVos.get(0).getYwd15());
            }else{
                params.put("ywd15","0");
            }

            BigDecimal hjy15=new BigDecimal(trainVos.get(0).getYsf15()).
                    add(new BigDecimal(trainVos.get(0).getYwd15()));
            params.put("hjy15",hjy15.toString());

            BigDecimal sfbr=new BigDecimal(trainVos.get(0).getBrsf25()).
                    add(new BigDecimal(trainVos.get(0).getBrsf510())).add(new BigDecimal(trainVos.get(0).
                    getBrsf1015())).add(new BigDecimal(trainVos.get(0).getBrsf15()));
            params.put("sfbr",sfbr.toString());
            BigDecimal sfby=new BigDecimal(trainVos.get(0).getYsf25()).
                    add(new BigDecimal(trainVos.get(0).getYsf510())).add(new BigDecimal(trainVos.get(0).
                    getYsf1015())).add(new BigDecimal(trainVos.get(0).getYsf15()));
            params.put("sfby",sfby.toString());
            BigDecimal zdbr=new BigDecimal(trainVos.get(0).getBrzd25()).
                    add(new BigDecimal(trainVos.get(0).getBrzd510())).add(new BigDecimal(trainVos.get(0).
                    getBrzd1015())).add(new BigDecimal(trainVos.get(0).getBrzd15()));
            params.put("zdbr",zdbr.toString());
            BigDecimal zdby=new BigDecimal(trainVos.get(0).getYwd25()).
                    add(new BigDecimal(trainVos.get(0).getYwd510())).add(new BigDecimal(trainVos.get(0).
                    getYwd1015())).add(new BigDecimal(trainVos.get(0).getYwd15()));
            params.put("zdby",zdby.toString());

            BigDecimal hjbr=sfbr.add(zdbr);
            params.put("hjbr",hjbr.toString());
            BigDecimal hjby=sfby.add(zdby);
            params.put("hjby",hjby.toString());
        }else{
            params.put("brsf25","--");
            params.put("brzd25","--");
            params.put("hjbr25","--");
            params.put("brsf510","--");
            params.put("brzd510","--");
            params.put("hjbr510","--");
            params.put("brsf1015","--");
            params.put("brzd1015","--");
            params.put("hjbr1015","--");
            params.put("brsf15","--");
            params.put("brzd15","--");
            params.put("hjbr15","--");

            params.put("ysf25","--");
            params.put("ywd25","--");
            params.put("hjy25","--");
            params.put("ysf510","--");
            params.put("ywd510","--");
            params.put("hjy510","--");
            params.put("ysf1015","--");
            params.put("ywd1015","--");
            params.put("hjy1015","--");
            params.put("ysf15","--");
            params.put("ywd15","--");
            params.put("hjy15","--");

            params.put("sfbr","--");
            params.put("sfby","--");
            params.put("zdbr","--");
            params.put("zdby","--");
            params.put("hjbr","--");
            params.put("hjby","--");
        }
    }

    private void generateTrainWord(String date, String line, Map<String, Object> params) {
        Map<String, Object> map=Maps.newHashMap();
        String year=date.substring(0,4);
        map.put("year",year);
        map.put("recordTime",date);
        map.put("line",line);
        List<TrainVo> trainVos=trainDailyService.selectByParams(map);
        if(!ObjectUtils.isEmpty(trainVos)){
            params.put("drskb",trainVos.get(0).getDrskb());
            params.put("zdsxlcs",trainVos.get(0).getZdsxlcs());
            params.put("zxhcjj",trainVos.get(0).getZxhcjj());
            params.put("bycs",trainVos.get(0).getBycs());
            //daily
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdwlbr())) {
                params.put("zdwlbr",trainVos.get(0).getZdwlbr());
            }else{
                params.put("zdwlbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJhklbr())) {
                params.put("jhklbr",trainVos.get(0).getJhklbr());
            }else{
                params.put("jhklbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getSjklbr())) {
                params.put("sjklbr",trainVos.get(0).getSjklbr());
            }else{
                params.put("sjklbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getDxlbr())) {
                params.put("dxlbr",trainVos.get(0).getDxlbr());
            }else{
                params.put("dxlbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdlbr())) {
                params.put("zdlbr",trainVos.get(0).getZdlbr());
            }else{
                params.put("zdlbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzlbr())) {
                params.put("zdzlbr",trainVos.get(0).getZdzlbr());
            }else{
                params.put("zdzlbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdwdlbr())) {
                params.put("zdwdlbr",trainVos.get(0).getZdwdlbr());
            }else{
                params.put("zdwdlbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzdbr())) {
                params.put("zdzdbr",trainVos.get(0).getZdzdbr());
            }else{
                params.put("zdzdbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getKslcbr())) {
                params.put("kslcbr",trainVos.get(0).getKslcbr());
            }else{
                params.put("kslcbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZzlcbr())) {
                params.put("zzlcbr",trainVos.get(0).getZzlcbr());
            }else{
                params.put("zzlcbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYylcbr())) {
                params.put("yylcbr",trainVos.get(0).getYylcbr());
            }else{
                params.put("yylcbr","0");
            }

            if (!ObjectUtils.isEmpty(trainVos.get(0).getJkbr())) {
                params.put("jkbr",trainVos.get(0).getJkbr());
            }else{
                params.put("jkbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJybr())) {
                params.put("jybr",trainVos.get(0).getJybr());
            }else{
                params.put("jybr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getTtbr())) {
                params.put("ttbr",trainVos.get(0).getTtbr());
            }else{
                params.put("ttbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getCxbr())) {
                params.put("cxbr",trainVos.get(0).getCxbr());
            }else{
                params.put("cxbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getXxbr())) {
                params.put("xxbr",trainVos.get(0).getXxbr());
            }else{
                params.put("xxbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getGgqkbr())) {
                params.put("ggqkbr",trainVos.get(0).getGgqkbr());
            }else{
                params.put("ggqkbr","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYytzbr())) {
                params.put("yytzbr",trainVos.get(0).getYytzbr());
            }else{
                params.put("yytzbr","0");
            }
            //monthly
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJhklylj())) {
                params.put("jhklylj",trainVos.get(0).getJhklylj());
            }else{
                params.put("jhklylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getSjklylj())) {
                params.put("sjklylj",trainVos.get(0).getSjklylj());
            }else{
                params.put("sjklylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getDxlylj())) {
                params.put("dxlylj",trainVos.get(0).getDxlylj());
            }else{
                params.put("dxlylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdlylj())) {
                params.put("zdlylj",trainVos.get(0).getZdlylj());
            }else{
                params.put("zdlylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzlylj())) {
                params.put("zdzlylj",trainVos.get(0).getZdzlylj());
            }else{
                params.put("zdzlylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdwlylj())) {
                params.put("zdwlylj",trainVos.get(0).getZdwlylj());
            }else{
                params.put("zdwlylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzdylj())) {
                params.put("zdzdylj",trainVos.get(0).getZdzdylj());
            }else{
                params.put("zdzdylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getKslcylj())) {
                params.put("kslcylj",trainVos.get(0).getKslcylj());
            }else{
                params.put("kslcylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZzlcylj())) {
                params.put("zzlcylj",trainVos.get(0).getZzlcylj());
            }else{
                params.put("zzlcylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYylcylj())) {
                params.put("yylcylj",trainVos.get(0).getYylcylj());
            }else{
                params.put("yylcylj","0");
            }

            if (!ObjectUtils.isEmpty(trainVos.get(0).getJkylj())) {
                params.put("jkylj",trainVos.get(0).getJkylj());
            }else{
                params.put("jkylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJyylj())) {
                params.put("jyylj",trainVos.get(0).getJyylj());
            }else{
                params.put("jyylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getTtylj())) {
                params.put("ttylj",trainVos.get(0).getTtylj());
            }else{
                params.put("ttylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getCxylj())) {
                params.put("cxylj",trainVos.get(0).getCxylj());
            }else{
                params.put("cxylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getXxylj())) {
                params.put("xxylj",trainVos.get(0).getXxylj());
            }else{
                params.put("xxylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getGgqkylj())) {
                params.put("ggqkylj",trainVos.get(0).getGgqkylj());
            }else{
                params.put("ggqkylj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYytzylj())) {
                params.put("yytzylj",trainVos.get(0).getYytzylj());
            }else{
                params.put("yytzylj","0");
            }
            //year
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJhklnlj())) {
                params.put("jhklnlj",trainVos.get(0).getJhklnlj());
            }else{
                params.put("jhklnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getSjklnlj())) {
                params.put("sjklnlj",trainVos.get(0).getSjklnlj());
            }else{
                params.put("sjklnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getDxlnlj())) {
                params.put("dxlnlj",trainVos.get(0).getDxlnlj());
            }else{
                params.put("dxlnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdlnlj())) {
                params.put("zdlnlj",trainVos.get(0).getZdlnlj());
            }else{
                params.put("zdlnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzlnlj())) {
                params.put("zdzlnlj",trainVos.get(0).getZdzlnlj());
            }else{
                params.put("zdzlnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdwlnlj())) {
                params.put("zdwlnlj",trainVos.get(0).getZdwlnlj());
            }else{
                params.put("zdwlnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZdzdnlj())) {
                params.put("zdzdnlj",trainVos.get(0).getZdzdnlj());
            }else{
                params.put("zdzdnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getKslcnlj())) {
                params.put("kslcnlj",trainVos.get(0).getKslcnlj());
            }else{
                params.put("kslcnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getZzlcnlj())) {
                params.put("zzlcnlj",trainVos.get(0).getZzlcnlj());
            }else{
                params.put("zzlcnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYylcnlj())) {
                params.put("yylcnlj",trainVos.get(0).getYylcnlj());
            }else{
                params.put("yylcnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJknlj())) {
                params.put("jknlj",trainVos.get(0).getJknlj());
            }else{
                params.put("jknlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getJynlj())) {
                params.put("jynlj",trainVos.get(0).getJynlj());
            }else{
                params.put("jynlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getTtnlj())) {
                params.put("ttnlj",trainVos.get(0).getTtnlj());
            }else{
                params.put("ttnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getCxnlj())) {
                params.put("cxnlj",trainVos.get(0).getCxnlj());
            }else{
                params.put("cxnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getXxnlj())) {
                params.put("xxnlj",trainVos.get(0).getXxnlj());
            }else{
                params.put("xxnlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getGgqknlj())) {
                params.put("ggqknlj",trainVos.get(0).getGgqknlj());
            }else{
                params.put("ggqknlj","0");
            }
            if (!ObjectUtils.isEmpty(trainVos.get(0).getYytznlj())) {
                params.put("yytznlj",trainVos.get(0).getYytznlj());
            }else{
                params.put("yytznlj","0");
            }
        }else{
            //schedule
            params.put("zdwlbr","--");
            params.put("drskb","--");
            params.put("zdsxlcs","--");
            params.put("zxhcjj","--");
            params.put("bycs","--");
            params.put("jhklbr","--");
            params.put("sjklbr","--");
            params.put("dxlbr","--");
            params.put("zdlbr","--");
            params.put("zdzlbr","--");
            params.put("zdwdlbr","--");
            params.put("zdzdbr","--");
            params.put("kslcbr","--");
            params.put("zzlcbr","--");
            params.put("yylcbr","--");
            params.put("jkbr","--");
            params.put("jybr","--");
            params.put("ttbr","--");
            params.put("cxbr","--");
            params.put("xxbr","--");
            params.put("ggqkbr","--");
            params.put("yytzbr","--");
            params.put("jhklylj","--");
            params.put("sjklylj","--");
            params.put("dxlylj","--");
            params.put("zdlylj","--");
            params.put("zdzlylj","--");
            params.put("zdwlylj","--");
            params.put("zdzdylj","--");
            params.put("kslcylj","--");
            params.put("zzlcylj","--");
            params.put("yylcylj","--");
            params.put("jkylj","--");
            params.put("jyylj","--");
            params.put("ttylj","--");
            params.put("cxylj","--");
            params.put("xxylj","--");
            params.put("ggqkylj","--");
            params.put("yytzylj","--");
            params.put("jhklnlj","--");
            params.put("sjklnlj","--");
            params.put("dxlnlj","--");
            params.put("zdlnlj","--");
            params.put("zdzlnlj","--");
            params.put("zdwlnlj","--");
            params.put("zdzdnlj","--");
            params.put("kslcnlj","--");
            params.put("zzlcnlj","--");
            params.put("yylcnlj","--");
            params.put("jknlj","--");
            params.put("jynlj","--");
            params.put("ttnlj","--");
            params.put("cxnlj","--");
            params.put("xxnlj","--");
            params.put("ggqknlj","--");
            params.put("yytznlj","--");
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
            String gateWayHost=mapWhere.get("gateWayHost").toString();//????????????????????????
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
        System.out.println(map);
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
            logger.info("==========url:" +dcCommonUtil.getDocUrl()+"/file/upload");
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
            System.out.println(s);
            Map<String,Object>s1= (Map<String, Object>) s.get("data");
            Integer fId= (Integer) s1.get("id");
            docMapper.updateFileStatus(fId);
            return fId;
        }else {
            HttpHeaders headers=docFileUser(map);
            HttpEntity<Map<String, Object>> files = new HttpEntity<>(map, headers);
            RestTemplate restTemplate = new RestTemplate();
            String gateWayHost=map.get("gateWayHost").toString();//????????????????????????
            Map<String,Object> s =restTemplate.postForObject(dcCommonUtil.getDocUrl()+"/corp/add", files, Map.class);
            System.out.println(s);
            Map<String,Object>s1= (Map<String, Object>) s.get("data");
            Integer fId= (Integer) s1.get("id");
            return fId;
        }
    }

    private void generateTrafficWord(String date, String line, Map<String, Object> params) {
        List<TrafficLineDailyVo>trafficLineDailyVos=trafficLineDailyService.trafficLineDailyVoList(date,line);
        List<TrafficPeakLineDailyVo>trafficPeakLineDailyVos=trafficPeakLineDailyService.queryPeakLineForReport(date,line);
        List<TrafficLineTicketDailyVo>trafficLineTicketDailyVos=trafficLineTicketDailyService.queryTicketDailyForReport(date,line);
        //???1.3??????????????????
        //??????????????????
        if (trafficLineDailyVos.size()>1){
            for(TrafficLineDailyVo vo:trafficLineDailyVos){
                    if(line.equals(vo.getLine())){
                        //?????????????????????
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolume())) {
                            params.put("kyzl",vo.getTrafficVolume().toString());
                        }else{
                            params.put("kyzl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getArrivalVolume())) {
                            params.put("jz",vo.getArrivalVolume().toString());
                        }else{
                            params.put("jz","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTransferVolume())) {
                            params.put("hc",vo.getTransferVolume().toString());
                        }else{
                            params.put("hc","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeMonth())) {
                            params.put("yljkyl",vo.getTrafficVolumeMonth().toString());
                        }else{
                            params.put("yljkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeMonthAverage())) {
                            params.put("byrjkyl",vo.getTrafficVolumeMonthAverage().toString());
                        }else{
                            params.put("byrjkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeYear())) {
                            params.put("nljkyl",vo.getTrafficVolumeYear().toString());
                        }else{
                            params.put("nljkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeYearAverage())) {
                            params.put("nljrjkyl",vo.getTrafficVolumeYearAverage().toString());
                        }else{
                            params.put("nljrjkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeOpen())) {
                            params.put("ktzjlj",vo.getTrafficVolumeOpen().toString());
                        }else{
                            params.put("ktzjlj","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeOpenAverage())) {
                            params.put("ktzjljrj",vo.getTrafficVolumeOpenAverage().toString());
                        }else{
                            params.put("ktzjljrj","0");
                        }
                    }else{
                        //???????????????,?????????????????????
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolume())) {
                            params.put("zkyzl",vo.getTrafficVolume().toString());
                        }else{
                            params.put("zkyzl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getArrivalVolume())) {
                            params.put("zjz",vo.getArrivalVolume().toString());
                        }else{
                            params.put("zjz","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTransferVolume())) {
                            params.put("zhc", vo.getTransferVolume().toString());
                        }else{
                            params.put("zhc","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeMonth())) {
                            params.put("zyljkyl", vo.getTrafficVolumeMonth().toString());
                        }else{
                            params.put("zyljkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeMonthAverage())) {
                            params.put("zbyrjkyl", vo.getTrafficVolumeMonthAverage().toString());
                        }else{
                            params.put("zbyrjkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeYear())) {
                            params.put("znljkyl", vo.getTrafficVolumeYear().toString());
                        }else{
                            params.put("znljkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeYearAverage())) {
                            params.put("znljrjkyl", vo.getTrafficVolumeYearAverage().toString());
                        }else{
                            params.put("znljrjkyl","0");
                        }
                        if (!ObjectUtils.isEmpty(vo.getTrafficVolumeYearAverage())) {
                            params.put("znljrjkyl", vo.getTrafficVolumeYearAverage().toString());
                        }else{
                            params.put("znljrjkyl","0");
                        }
                        params.put("zktzjlj", "0.00");
                        params.put("zktzjljrj", "0.00");
                    }
            }
        }else{
            params.put("kyzl","--");
            params.put("jz", "--");
            params.put("hc", "--");
            params.put("yljkyl", "--");
            params.put("byrjkyl", "--");
            params.put("nljkyl", "--");
            params.put("nljrjkyl", "--");
            params.put("ktzjlj", "--");
            params.put("ktzjljrj", "--");
            params.put("zkyzl", "--");
            params.put("zjz", "--");
            params.put("zhc", "--");
            params.put("zyljkyl", "--");
            params.put("zbyrjkyl", "--");
            params.put("znljkyl", "--");
            params.put("znljrjkyl", "--");
            params.put("zktzjlj", "--");
            params.put("zktzjljrj", "--");
        }
        //???????????????????????????
        if(trafficPeakLineDailyVos.size()!=0){
            for(TrafficPeakLineDailyVo vo:trafficPeakLineDailyVos){
                if (!ObjectUtils.isEmpty(vo.getTransectUpVolume())) {
                    params.put("sx", vo.getTransectUpVolume().toString());
                }else{
                    params.put("sx","0");
                }
                if (!ObjectUtils.isEmpty(vo.getTransectDownVolume())) {
                    params.put("xx", vo.getTransectDownVolume().toString());
                }else{
                    params.put("xx","0");
                }
                if (!ObjectUtils.isEmpty(vo.getMorningPeakTrafficVolume())) {
                    params.put("zgf", vo.getMorningPeakTrafficVolume().toString());
                }else{
                    params.put("zgf","0");
                }
                if (!ObjectUtils.isEmpty(vo.getEveningPeakTrafficVolume())) {
                    params.put("wgf", vo.getEveningPeakTrafficVolume().toString());
                }else{
                    params.put("wgf","0");
                }
            }
        }else{
            params.put("sx", "--");
            params.put("xx","--");
            params.put("zgf", "--");
            params.put("wgf", "--");
        }

        //???????????????????????????
        if(trafficLineTicketDailyVos.size()!=0){
            for(TrafficLineTicketDailyVo vo:trafficLineTicketDailyVos){
                if("0".equals(vo.getTicketType().toString())){
                    params.put("dcp", vo.getTrafficVolume().toString());
                    params.put("dcpbl", vo.getRate()+"%");
                }
                if("1".equals(vo.getTicketType().toString())){
                    params.put("cst", vo.getTrafficVolume().toString());
                    params.put("cstbl", vo.getRate()+"%");
                }
                if("2".equals(vo.getTicketType().toString())){
                    params.put("gzp", vo.getTrafficVolume().toString());
                    params.put("gzpbl", vo.getRate()+"%");
                }
                if("3".equals(vo.getTicketType().toString())){
                    params.put("jcp", vo.getTrafficVolume().toString());
                    params.put("jcpbl", vo.getRate()+"%");
                }
                if("4".equals(vo.getTicketType().toString())){
                    params.put("cxp", vo.getTrafficVolume().toString());
                    params.put("cxpbl", vo.getRate()+"%");
                    System.out.println();
                }
                if("5".equals(vo.getTicketType().toString())){
                    params.put("rlzf", vo.getTrafficVolume().toString());
                    params.put("rlzfbl", vo.getRate()+"%");
                }
            }
        }else{
            params.put("dcp", "--");
            params.put("dcpbl","--");

            params.put("cst", "--");
            params.put("cstbl", "--");

            params.put("gzp", "--");
            params.put("gzpbl", "--");

            params.put("jcp", "--");
            params.put("jcpbl", "--");

            params.put("cxp", "--");
            params.put("cxpbl", "--");

            params.put("rlzf", "--");
            params.put("rlzfbl", "--");
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    @AddLog(title = "??????????????????",module = LoggerModuleEnum.ODR_REPORT)
    public void deleteReport(List<Map<String,Integer>> mapIds) {
        //??????
        List<Integer>ips=mapIds.stream().map(m->m.get("id")).collect(Collectors.toList());
        List<DailyReportPo>pos=this.selectByIds(ips);
        for(DailyReportPo po :pos){
            if("???????????????".equals(po.getDescription())){
                if("0".equals(po.getReportType())){//????????????
                    this.addDeleteLogger(new AcmLogger("???????????????????????????????????????????????????????????????"
                            ,DateUtil.getDateFormat(po.getReportName()),po.getLine(),"","INIT"));
                }else{//??????????????????
                    this.addDeleteLogger(new AcmLogger("?????????????????????????????????????????????????????????????????????"
                            ,DateUtil.getDateFormat(po.getReportName()),po.getLine(),"","INIT"));
                }
            }else{//????????????
                if("0".equals(po.getReportType())){
                    this.addDeleteLogger(new AcmLogger("????????????????????????????????????????????????"
                            ,DateUtil.getDateFormat(po.getReportName()),po.getLine(),"","INIT"));
                }else{
                    this.addDeleteLogger(new AcmLogger("??????????????????????????????????????????????????????"
                            ,DateUtil.getDateFormat(po.getReportName()),po.getLine(),"","INIT"));
                }
            }
        }
        List<Integer> ids=Lists.newArrayList();
        List<Integer> fileIds_=Lists.newArrayList();
        List<Map<String,Object>>mapList=Lists.newArrayList();
        for(Map<String,Integer>map:mapIds){
            Map<String,Object>m=Maps.newHashMap();
            Integer id=map.get("id");
            Integer fileId_=map.get("fileId");
            ids.add(id);
            fileIds_.add(fileId_);
            m.put("type","doc");
            m.put("id",fileId_);
            mapList.add(m);
        }
        //????????????
        commDocService.deleteByFileIds(fileIds_);
        commDocService.deleteFileByFileIds(mapList);
        //??????????????????
        if(ids.contains(0)){
            return;
        }
        this.deleteByIds(ids);
    }

    @Override
    public PageInfo<DailyReportVo> queryDailyReportList(Map<String, Object> mapWhere, Integer pageSize, Integer currentPageNum) {
        mapWhere.put("reportType", StringHelper.formattString(String.valueOf(mapWhere.get("reportType"))));
        mapWhere.put("reviewStatus", StringHelper.formattString(String.valueOf(mapWhere.get("reviewStatus"))));
        List<String>line=Lists.newArrayList();
        if(!ObjectUtils.isEmpty(mapWhere.get("line"))){
            String[] keys = mapWhere.get("line").toString().split(",");
            for(String str: keys){
                line.add(str);
            }
        }
        mapWhere.put("lines",line);
        if(line.size()==0){
            line.add("1");line.add("2");line.add("3");
        }
        mapWhere.put("startTime", StringHelper.formattString(String.valueOf(mapWhere.get("startTime"))));
        mapWhere.put("endTime", StringHelper.formattString(String.valueOf(mapWhere.get("endTime"))));
        PageHelper.startPage(currentPageNum, pageSize);
        List<DailyReportVo> dailyReportVos= mapper.queryDailyReportList(mapWhere);
        PageInfo<DailyReportVo> pageInfo = new PageInfo<DailyReportVo>(dailyReportVos);
        if (!ObjectUtils.isEmpty(pageInfo.getList()))
        {
            Map<String, DictionaryVo> typeDictMap=commDictService.getDictMapByTypeCode("report.type");
            for(DailyReportVo dailyReportVo : pageInfo.getList()){
                if(dailyReportVo.getDescription().equals("???????????????")){
                    dailyReportVo.setInitMan("????????????");
                }
                //????????????????????????
                dailyReportVo.getReviewStatusVo().setName(EnumsUtil.StatusEnum.valueOf(dailyReportVo.getReviewStatusVo()
                        .getCode()).getName());
                //??????????????????
                dailyReportVo.getReportTypeVo().setName(dcCommonUtil.getDictionaryName(typeDictMap, dailyReportVo.getReportType()));;
            }
        }
        return pageInfo;
    }

    @Override
    public List<DailyReportVo> queryFlowDailyReportList(Map<String, Object> mapWhere) {
        mapWhere.put("reportType", StringHelper.formattString(String.valueOf(mapWhere.get("reportType"))));
        mapWhere.put("reviewStatus", "INIT");
        List<String>line=Lists.newArrayList();
        if(!ObjectUtils.isEmpty(mapWhere.get("line"))){
            String[] keys = mapWhere.get("line").toString().split(",");
            for(String str: keys){
                line.add(str);
            }
        }
        mapWhere.put("lines",line);
        if(line.size()==0){
            line.add("1");line.add("2");line.add("3");
        }
        mapWhere.put("startTime", StringHelper.formattString(String.valueOf(mapWhere.get("startTime"))));
        mapWhere.put("endTime", StringHelper.formattString(String.valueOf(mapWhere.get("endTime"))));
        List<DailyReportVo> dailyReportVos= mapper.queryDailyReportList(mapWhere);
        if(!ObjectUtils.isEmpty(dailyReportVos)){
            Map<String, DictionaryVo> typeDictMap=commDictService.getDictMapByTypeCode("report.type");
            for(DailyReportVo dailyReportVo : dailyReportVos){
                if(dailyReportVo.getDescription().equals("???????????????")){
                    dailyReportVo.setInitMan("????????????");
                }
                //????????????????????????
                dailyReportVo.getReviewStatusVo().setName(EnumsUtil.StatusEnum.valueOf(dailyReportVo.getReviewStatusVo()
                        .getCode()).getName());
                //??????????????????
                dailyReportVo.getReportTypeVo().setName(dcCommonUtil.getDictionaryName(typeDictMap, dailyReportVo.getReportType()));;
            }
        }
        return dailyReportVos;
    }

    @Override
    public List<DailyReportVo> getFlowDailyReportList(Map<String, Object> mapWhere) {
        List<DailyReportVo> dailyReportVos= mapper.getFlowDailyReportList(mapWhere);
        if (!ObjectUtils.isEmpty(dailyReportVos))
        {
            Map<String, DictionaryVo> typeDictMap=commDictService.getDictMapByTypeCode("report.type");
            for(DailyReportVo dailyReportVo : dailyReportVos){
                if(dailyReportVo.getDescription().equals("???????????????")){
                    dailyReportVo.setInitMan("????????????");
                }
                //????????????????????????
                dailyReportVo.getReviewStatusVo().setName(EnumsUtil.StatusEnum.valueOf(dailyReportVo.getReviewStatusVo()
                        .getCode()).getName());
                //??????????????????
                dailyReportVo.getReportTypeVo().setName(dcCommonUtil.getDictionaryName(typeDictMap, dailyReportVo.getReportType()));;
            }
        }
        return dailyReportVos;
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
    private String getOperateTimes(String line,Map<String, Object>mapWhere) throws Exception {
        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        calendar.setTime(sdf.parse(mapWhere.get("date").toString()));
        calendar.set(Calendar.DATE,calendar.get(Calendar.DATE)-1);
        Date date=sdf.parse(DateUtil.getDateFormat(hrbSysService.queryLineFoundationList().stream().filter(e-> StringUtils.equalsIgnoreCase(e.getLine(),line)).collect(Collectors.toList()).get(0).getOperationTime()));
        long timeMillion= DateUtil.getDateFormat(sdf.format(calendar.getTime())).getTime()-date.getTime();
        return String.valueOf((timeMillion/(24l*60*60*1000)));
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
}
