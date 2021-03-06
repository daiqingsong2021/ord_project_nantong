package com.wisdom.acm.szxm.service.rygl.impl;

import com.google.common.collect.Maps;
import com.wisdom.acm.szxm.common.DateUtil;
import com.wisdom.acm.szxm.common.StringHelper;
import com.wisdom.acm.szxm.common.SzxmCommonUtil;
import com.wisdom.acm.szxm.form.rygl.SpecialWorkCertAddForm;
import com.wisdom.acm.szxm.form.rygl.SpecialWorkCertUpdateForm;
import com.wisdom.acm.szxm.mapper.rygl.SpecialWorkCertMapper;
import com.wisdom.acm.szxm.po.rygl.SpecialWorkCertPo;
import com.wisdom.acm.szxm.po.rygl.SpecialWorkerPo;
import com.wisdom.acm.szxm.service.rygl.SpecialWorkCertService;
import com.wisdom.acm.szxm.vo.rygl.SpecialWorkCertVo;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommFileService;
import com.wisdom.base.common.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SpecialWorkCertServiceImpl extends BaseService<SpecialWorkCertMapper, SpecialWorkCertPo> implements
        SpecialWorkCertService
{

    @Autowired private CommFileService commFileService;

    @Override public List<SpecialWorkCertVo> selectSpecialWorkCert(Map<String, Object> mapWhere)
    {
        mapWhere.put("specialWorkerId", StringHelper.formattString(String.valueOf(mapWhere.get("specialWorkerId"))));
        List<SpecialWorkCertVo> specialWorkCertVoList = mapper.selectSpecialWorkCert(mapWhere);
        for(SpecialWorkCertVo specialWorkCertVo:specialWorkCertVoList)
        {
            Date warnDate=specialWorkCertVo.getWarnDate();
            Date certExpirationTime=specialWorkCertVo.getCertExpirationTime();
            Date nowDate = new Date();
            if (nowDate.equals(warnDate) || (nowDate.after(warnDate) && nowDate.before(certExpirationTime)) || nowDate.equals(certExpirationTime))
            {
                specialWorkCertVo.getWarnStatusVo().setCode("1");
                specialWorkCertVo.getWarnStatusVo().setName("??????");
            }
            else if (nowDate.after(certExpirationTime))
            {
                specialWorkCertVo.getWarnStatusVo().setCode("2");
                specialWorkCertVo.getWarnStatusVo().setName("??????");
            }
            else
            {
                specialWorkCertVo.getWarnStatusVo().setCode("0");
                specialWorkCertVo.getWarnStatusVo().setName("??????");

            }

        }
        return specialWorkCertVoList;
    }

    @Override public SpecialWorkCertVo addSpecialWorkCert(SpecialWorkCertAddForm specialWorkCertAddForm)
    {
        Example example = new Example(SpecialWorkCertPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("specialWorkerId",specialWorkCertAddForm.getSpecialWorkerId());
        criteria.andEqualTo("certGlId",specialWorkCertAddForm.getCertGlId());
        SpecialWorkCertPo existPo=this.selectOneByExample(example);
        if(!ObjectUtils.isEmpty(existPo))
            throw new BaseException("???????????????????????????????????????????????????!");
        SpecialWorkCertPo specialWorkCertPo = dozerMapper.map(specialWorkCertAddForm, SpecialWorkCertPo.class);
        this.insert(specialWorkCertPo);

        //????????????ID??????
        List<Integer> fileIds =specialWorkCertAddForm.getFileIds();
        //????????????????????????
        if(!ObjectUtils.isEmpty(fileIds))
           commFileService.addFileRelation(specialWorkCertPo.getId(),"STAFF-SPECIALTYPE-CERTIFICATE",fileIds);

        Map<String,Object> mapWhere= Maps.newHashMap();
        mapWhere.put("id",specialWorkCertPo.getId());
        SpecialWorkCertVo specialWorkCertVo =this.selectSpecialWorkCert(mapWhere).get(0);
        return specialWorkCertVo;
    }

    @Override public void deleteSpecialWorkCert(List<Integer> ids)
    {
        //????????????
        commFileService.deleteDocFileRelationByBiz(ids,"STAFF-SPECIALTYPE-CERTIFICATE");
        //????????????
        this.deleteByIds(ids);
    }

    @Override public SpecialWorkCertVo updateSpecialWorkCert(SpecialWorkCertUpdateForm specialWorkCertUpdateForm)
    {

        Example example = new Example(SpecialWorkCertPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("specialWorkerId",specialWorkCertUpdateForm.getSpecialWorkerId());
        criteria.andEqualTo("certGlId",specialWorkCertUpdateForm.getCertGlId());
        criteria.andNotEqualTo("id",specialWorkCertUpdateForm.getId());
        SpecialWorkCertPo existPo=this.selectOneByExample(example);
        if(!ObjectUtils.isEmpty(existPo))
            throw new BaseException("???????????????????????????????????????????????????!");

        SpecialWorkCertPo updatePo = dozerMapper.map(specialWorkCertUpdateForm, SpecialWorkCertPo.class);

        this.updateSelectiveById(updatePo);//??????ID??????po?????????null??????????????????????????????null??????

        //????????????ID??????
        List<Integer> fileIds =specialWorkCertUpdateForm.getFileIds();
        //????????????????????????
        if(!ObjectUtils.isEmpty(fileIds))
           commFileService.updateFileRelation(updatePo.getId(),"STAFF-SPECIALTYPE-CERTIFICATE",fileIds);

        Map<String,Object> mapWhere= Maps.newHashMap();
        mapWhere.put("id",updatePo.getId());
        SpecialWorkCertVo specialWorkCertVo =this.selectSpecialWorkCert(mapWhere).get(0);
        return specialWorkCertVo;
    }

    @Override public List<SpecialWorkCertVo> selectSwWorkWithWorker()
    {
        List<SpecialWorkCertVo> specialWorkCertVoList = mapper.selectSwWorkWithWorker();
        return specialWorkCertVoList;
    }

    @Override
    public List<SpecialWorkCertVo> getCertByUserIdList(Map<String, Object> mapWhere)
    {
        List<SpecialWorkCertVo> specialWorkCertVoList = mapper.selectWorkCertByUserId(mapWhere);
        for(SpecialWorkCertVo specialWorkCertVo:specialWorkCertVoList)
        {
            Date warnDate=specialWorkCertVo.getWarnDate();
            Date certExpirationTime=specialWorkCertVo.getCertExpirationTime();
            Date nowDate = new Date();
            if (nowDate.equals(warnDate) || (nowDate.after(warnDate) && nowDate.before(certExpirationTime)) || nowDate.equals(certExpirationTime))
            {
                specialWorkCertVo.getWarnStatusVo().setCode("1");
                specialWorkCertVo.getWarnStatusVo().setName("??????");
            }
            else if (nowDate.after(certExpirationTime))
            {
                specialWorkCertVo.getWarnStatusVo().setCode("2");
                specialWorkCertVo.getWarnStatusVo().setName("??????");
            }
            else
            {
                specialWorkCertVo.getWarnStatusVo().setCode("0");
                specialWorkCertVo.getWarnStatusVo().setName("??????");

            }

        }
        return specialWorkCertVoList;
    }
}
