package com.wisdom.acm.sys.service.impl;

import com.wisdom.acm.sys.form.SysIptAddForm;
import com.wisdom.acm.sys.form.SysIptUpdateForm;
import com.wisdom.acm.sys.form.SysSearchIptForm;
import com.wisdom.acm.sys.form.SysUserIptAddForm;
import com.wisdom.acm.sys.mapper.*;
import com.wisdom.acm.sys.po.SysIptPo;
import com.wisdom.acm.sys.po.SysUserIptPo;
import com.wisdom.acm.sys.po.SysUserIptRolePo;
import com.wisdom.acm.sys.service.SysIptService;
import com.wisdom.acm.sys.service.SysUserIptRoleService;
import com.wisdom.acm.sys.service.SysUserIptService;
import com.wisdom.acm.sys.service.SysUserService;
import com.wisdom.acm.sys.vo.*;
import com.wisdom.base.common.aspect.AddLog;
import com.wisdom.base.common.enums.LoggerModuleEnum;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.log.AcmLogger;
import com.wisdom.base.common.msg.ApiResult;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.LogUtil;
import com.wisdom.base.common.util.TreeUtil;
import com.wisdom.base.common.vo.DictionaryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SysIptServiceImpl extends BaseService<IptMapper, SysIptPo> implements SysIptService {

    @Autowired
    private SysUserIptService sysUserIptService;

    @Autowired
    private SysUserIptRoleService sysUserIptRoleService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private CommDictService commDictService;


    @Override
    public List<SysIptVo> queryIptTree() {
        List<SysIptVo> list = mapper.selectIptList();
        List<SysIptVo> iptTree = TreeUtil.bulid(list, 0);
        return iptTree;
    }

    @Override
    public List<SysIptVo> queryIptsBySearch(SysSearchIptForm searchMap) {
        List<SysIptVo> iptTree = this.queryIptTree();
        if (searchMap.getSearcher() == null || searchMap.getSearcher() == "") {
            return iptTree;
        } else {
            List<Integer> retIds = mapper.selectIptsBySearch(searchMap);
            boolean auth = false;
            List<SysIptVo> retIptTree = this.querySearchIptTree(retIds, iptTree, auth);
            return retIptTree;
        }
    }


    /**
     * ??????????????????
     *
     * @param iptId
     * @return
     */
    @Override
    public SysIptInfoVo getIptInfo(Integer iptId) {
        SysIptInfoVo sysIptInfoVo = mapper.selectIptById(iptId);
        Map<String, DictionaryVo> dictMap = commDictService.getDictMapByTypeCode("sys.org.level");
        if (!ObjectUtils.isEmpty(dictMap) && !ObjectUtils.isEmpty(sysIptInfoVo.getLevel())) {
            sysIptInfoVo.getLevel().setName(String.valueOf(dictMap.get(sysIptInfoVo.getLevel().getCode()).getName()));
        }
        return sysIptInfoVo;
    }

    /**
     * ??????ipt
     *
     * @param ipt
     * @return
     */
    @Override
    public SysIptPo addIpt(SysIptAddForm ipt) {
        SysIptPo sysIptPo = dozerMapper.map(ipt, SysIptPo.class);
        if (!ObjectUtils.isEmpty(ipt.getSort())) {
            sysIptPo.setSort(this.getNextSortByParentId(ipt.getParentId()));
        }
        //??????????????????????????????
        List<SysIptPo> list = this.getIptPoByCode(ipt.getIptCode());
        if (!ObjectUtils.isEmpty(list)) {
            throw new BaseException("IPT??????????????????");
        }

        super.insertSelective(sysIptPo);
        return sysIptPo;
    }

    public int getNextSortByParentId(Integer parentId) {
        Example example = new Example(SysIptPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("parentId", parentId);
        return this.selectCountByExample(example);
    }

    /**
     * ??????ipt
     *
     * @param iptUpdate
     * @return
     */
//    @CacheClear(keys = {"acm:ipts"})
    @Override
    @AddLog(title = "??????IPT" ,module = LoggerModuleEnum.SM_IPT)
    public SysIptPo updateIpt(SysIptUpdateForm iptUpdate) {
        //??????ipt????????????
        SysIptPo sysIptPo = mapper.selectByPrimaryKey(iptUpdate.getId());
        if (ObjectUtils.isEmpty(sysIptPo)) {
            throw new BaseException("???IPT?????????");
        }

        // ??????????????????
        this.addChangeLogger(iptUpdate,sysIptPo);
        //??????????????????????????????
        List<SysIptPo> list = this.getIptPoByCode(iptUpdate.getIptCode());
        if (!ObjectUtils.isEmpty(list) && !list.get(0).getId().equals(iptUpdate.getId())) {
            throw new BaseException("????????????????????????");
        }
        dozerMapper.map(iptUpdate, sysIptPo);
        super.updateSelectiveById(sysIptPo);
        return sysIptPo;
    }

    public List<SysIptPo> getIptPoByCode(String code) {
        Example example = new Example(SysIptPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("iptCode", code);
        List<SysIptPo> list = this.mapper.selectByExample(example);
        return ObjectUtils.isEmpty(list) ? null : list;
    }

    //    @CacheClear(keys = {"acm:ipts"})
    @Override
    public void deleteIpt(List<Integer> iptIds) {
        iptIds = ListUtil.toIdList(this.queryChildrenAndMePos(iptIds));
        //????????????ipt????????????
        sysUserIptRoleService.deleteUserIptRoleRelation(iptIds);
        //????????????ipt??????
        sysUserIptService.deleteUserByIptId(iptIds);
        //??????ipt
        super.deleteByIds(iptIds);
    }

    /**
     * ???????????????ipt
     *
     * @param iptId
     * @param iptImportAddVo
     * @param hostIp
     * @param userName
     * @return
     */
    @Override
    public boolean importIptFromOrg(int iptId, IptImportAddVo iptImportAddVo, String hostIp, String userName) {
//        Date date = new Date();
//        boolean flag = false;
//        if (!ObjectUtils.isEmpty(iptImportAddVo)) {
//            List<IptOrgSelectVo> orgs = iptImportAddVo.getOrgs();
//            List<IptOrgSelectVo> orgTrue = new ArrayList<>();                                       //??????????????????????????????orgVo
//            List<IptOrgSelectVo> orgFalse = new ArrayList<>();                                      //?????????????????????orgVo
//            List<Integer> orgIdList = new ArrayList<>();                                            //?????????????????????orgVo
//            List<Integer> topOrgIds = new ArrayList<>();
//            for (IptOrgSelectVo iptOrgSelectVo : orgs) {
//                orgIdList.add(iptOrgSelectVo.getOrgId());
//                if (iptOrgSelectVo.getTopFlag() == true) {                                            //???????????????
//                    topOrgIds.add(iptOrgSelectVo.getOrgId());
//                }
//                if (iptOrgSelectVo.getLeafFlag() == true) {
//                    orgTrue.add(iptOrgSelectVo);
//                } else {
//                    orgFalse.add(iptOrgSelectVo);
//                }
//            }
//
//            List<SysOrgUserVo> orgUsers = orgMapper.selectRolationsByOrgIds(orgIdList);                    //?????????????????????????????????
//
//            List<SysIptPo> sysIptPoList = new ArrayList<>();
//
//            //???????????????????????????orgVo
//            if (!ObjectUtils.isEmpty(orgFalse)) {
//                List<SysOrgVo> sysOrgVos = orgMapper.selectOrgsByIds(orgFalse);                           //??????org??????
//                SysIptAddVo ipt = null;
//                for (SysOrgVo org : sysOrgVos) {
//                    ipt = new SysIptAddVo();
//                    ipt.setOrgId(org.getOrgId());
//                    ipt.setOrgPid(org.getParentId());
//                    ipt.setIptName(org.getOrgName());
//                    ipt.setIptCode(org.getOrgCode());
//                    ipt.setIptType("IPT");
//                    ipt.setDelFlg(2);
//                    SysIptPo sysIptPoPo = dozerMapper.map(ipt, SysIptPo.class);
//                    EntityUtils.setCreateInfo(sysIptPoPo);
//                    //??????ipt
//                    mapper.insert(sysIptPoPo);
//                    sysIptPoList.add(sysIptPoPo);
//                }
//            }
//
//            List<SysIptPo> retList = null;
//            //????????????????????????????????????orgVo
//            if (!ObjectUtils.isEmpty(orgTrue)) {
//                for (IptOrgSelectVo iptOrgSelectVo : orgTrue) {
//                    retList = new ArrayList<>();
//                    //????????????????????????
//                    SysOrgPo sysOrgPo = orgMapper.selectByPrimaryKey(iptOrgSelectVo.getOrgId());
//                    SysIptPo sysIptPo = new SysIptPo();
//                    sysIptPo.setOrgId(sysOrgPo.getId());
//                    sysIptPo.setOrgPid(sysOrgPo.getParentId());
//                    sysIptPo.setIptName(sysOrgPo.getOrgName());
//                    sysIptPo.setIptCode(sysOrgPo.getOrgCode());
//                    sysIptPo.setIptType("IPT");
//                    sysIptPo.setDelFlg(2);
//                    SysIptPo sysIptPoPo = dozerMapper.map(sysIptPo, SysIptPo.class);
//                    EntityUtils.setCreateInfo(sysIptPoPo);
//                    //??????ipt
//                    mapper.insert(sysIptPoPo);
//                    sysIptPoList.add(sysIptPoPo);
//
//                    List<SysOrgVo> orgTree = orgMapper.selectOrgsAllByPid(iptOrgSelectVo.getOrgId());  //?????????????????????
//                    List<SysIptPo> sysIptPos = this.treeToList(orgTree, retList);                                 //???????????????????????????iptAddVo
//                    for (SysIptPo sysIptPo1 : sysIptPos) {
//                        //??????ipt
//                        mapper.insert(sysIptPo1);
//                        sysIptPoList.add(sysIptPo1);
//                    }
//                }
//            }
//
//            List<SysIptPo> trList = new ArrayList<>();
//            for (SysIptPo sysIptPo : sysIptPoList) {
//                for (SysIptPo sysIptPo1 : sysIptPoList) {
//                    if (sysIptPo1.getOrgPid() == sysIptPo.getOrgId()) {
//                        sysIptPo1.setParentId(sysIptPo.getId());
//                        trList.add(sysIptPo1);
//                    }
//                }
//            }
//            for (int orgId : topOrgIds) {
//                for (SysIptPo sysIptPo : sysIptPoList) {
//                    if (sysIptPo.getOrgId() == orgId) {
//                        sysIptPo.setParentId(iptId);
//                        trList.add(sysIptPo);
//                    }
//                }
//            }
//
//            if (!ObjectUtils.isEmpty(orgUsers)) {
//                for (SysOrgUserVo sysOrgUserVo : orgUsers) {
//                    for (IptOrgSelectVo iptOrgSelectVo : orgs) {
//                        if (iptOrgSelectVo.getOrgId() == sysOrgUserVo.getOrgId()) {
//                            sysOrgUserVo.setRoleIds(iptOrgSelectVo.getRoleIds());
//                        }
//                    }
//                }
//
//                //??????iptuser??????
//                List<SysIptUserPo> sysIptUserPos = new ArrayList<>();
//                SysIptUserPo sysIptUserPo = null;
//                for (SysOrgUserVo orgUser : orgUsers) {
//                    for (SysIptPo sysIptPo : trList) {
//                        sysIptUserPo = new SysIptUserPo();
//                        if (orgUser.getOrgId() == sysIptPo.getOrgId()) {
//                            sysIptUserPo.setIptId(sysIptPo.getId());
//                            sysIptUserPo.setUserId(orgUser.getUserId());
//                            sysIptUserPo.setRoleIds(orgUser.getRoleIds());
//                            EntityUtils.setCreateInfo(sysIptUserPo);
////                            iptUser.setWsdCreator(userName);
////                            iptUser.setLastUpdIp(hostIp);
////                            iptUser.setCreatTime(date);
//                            sysIptUserPos.add(sysIptUserPo);
//                        }
//                    }
//                }
//                if (!ObjectUtils.isEmpty(sysIptUserPos)) {
//                    for (SysIptUserPo iptu : sysIptUserPos)
//                        userIptMapper.insert(iptu);
//                }
////                mapper.addIptUserRelations(iptUsers);
//            }
//            if (!ObjectUtils.isEmpty(trList)) {
//                for (SysIptPo sysIptPo : trList) {
//                    mapper.updateIptRelationFromOrg(sysIptPo.getId(), sysIptPo.getParentId());                                            //??????ipt????????????
//                }
//                flag = true;
//            }
//        }
//        return flag;
        return false;
    }


    public List<SysIptPo> treeToList(List<SysOrgVo> tree, List<SysIptPo> retList) {
        for (SysOrgVo sysOrgVo : tree) {
//            SysIptAddVo ipt = new SysIptAddVo();
//            ipt.setOrgId(sysOrgVo.getOrgId());
//            ipt.setOrgPid(sysOrgVo.getParentId());
//            ipt.setIptName(sysOrgVo.getOrgName());
//            ipt.setIptCode(sysOrgVo.getOrgCode());
//            ipt.setIptType("IPT");
//            ipt.setDelFlg(2);
//            SysIptPo sysIptPoPo = dozerMapper.map(ipt, SysIptPo.class);
//            EntityUtils.setCreateInfo(sysIptPoPo);
//            retList.add(sysIptPoPo);
//            if (!ObjectUtils.isEmpty(sysOrgVo.getChildrenList())) {
//                this.treeToList(sysOrgVo.getChildrenList(), retList);
//            }
        }
        return retList;
    }

    /**
     * ipt????????????
     *
     * @param list
     * @param iptId
     * @return
     */
    @Override
    public void addIptUser(List<SysUserIptAddForm> list, Integer iptId) {

        List<Integer> userIds = new ArrayList<>();

        for (SysUserIptAddForm sysUserIptAddForm : list){
            userIds.add(sysUserIptAddForm.getUserId());
        }
        //????????????????????????
        sysUserIptRoleService.deleteIptUserByUserIdAndIptId(userIds, iptId);
        sysUserIptService.deleteIptUserByUserIdAndIptId(userIds, iptId);

        for (SysUserIptAddForm sysUserIptAddForm : list) {

            SysUserIptPo userIptPo = sysUserIptService.validateUserIptRelationExist(sysUserIptAddForm.getUserId(), iptId);

            if (ObjectUtils.isEmpty(userIptPo)) {
                //??????iptuser??????
                SysUserIptPo sysUserIptPo = new SysUserIptPo();
                sysUserIptPo.setIptId(iptId);
                sysUserIptPo.setUserId(sysUserIptAddForm.getUserId());
                sysUserIptService.addUserIptRelation(sysUserIptPo);
            }

            //??????iptuserrole??????
            SysUserIptRolePo sysUserIptRolePo = null;
            List<Integer>  roleIds = sysUserIptAddForm.getRoleIds();
            if(!ObjectUtils.isEmpty(roleIds)){
                for (Integer roleId : sysUserIptAddForm.getRoleIds()) {
                    SysUserIptRolePo sysUserIptRolePo1 = sysUserIptRoleService.validateUserIptRoleRelation(roleId, sysUserIptAddForm.getUserId(), iptId);
                    if (ObjectUtils.isEmpty(sysUserIptRolePo1)) {
                        sysUserIptRolePo = new SysUserIptRolePo();
                        sysUserIptRolePo.setIptId(iptId);
                        sysUserIptRolePo.setUserId(sysUserIptAddForm.getUserId());
                        sysUserIptRolePo.setRoleId(roleId);
                        sysUserIptRoleService.addIptUserRoleRelation(sysUserIptRolePo);
                    }
                }
            }
        }
    }

    @Override
    public void addIptUserRole(SysUserIptAddForm sysUserIptAddForm, Integer iptId) {
        //??????iptuserrole??????
        SysUserIptRolePo sysUserIptRolePo = null;
        List<Integer> userIds = new ArrayList<>();
        userIds.add(sysUserIptAddForm.getUserId());

        //????????????????????????
        sysUserIptRoleService.deleteIptUserByUserIdAndIptId(userIds, iptId);
        for (Integer roleId : sysUserIptAddForm.getRoleIds()) {

            sysUserIptRolePo = new SysUserIptRolePo();
            sysUserIptRolePo.setIptId(iptId);
            sysUserIptRolePo.setUserId(sysUserIptAddForm.getUserId());
            sysUserIptRolePo.setRoleId(roleId);
            sysUserIptRoleService.addIptUserRoleRelation(sysUserIptRolePo);

        }
    }

    /**
     * ????????????ipt??????
     *
     * @param iptIds
     * @return
     */
    @Override
    public List<SysIptInfoVo> queryIptByIds(List<Integer> iptIds) {
        List<SysIptInfoVo> list = mapper.selectIptsByIds(iptIds);
        return list;
    }

    /**
     * ??????userIpt??????
     *
     * @param iptIds
     * @return
     */
    @Override
    public List<SysUserIptPo> queryUserIptRelationByIptIds(List<Integer> iptIds) {
        List<SysUserIptPo> list = sysUserIptService.queryUserIptRelation(iptIds);
        return list;
    }

    /**
     * ??????useriptrole??????
     *
     * @param iptIds
     * @return
     */
    @Override
    public List<SysUserIptRolePo> queryUserIptRoleRelationByiptIds(List<Integer> iptIds) {
        List<SysUserIptRolePo> list = sysUserIptRoleService.queryUserIptRoleRelation(iptIds);
        return list;
    }

    /**
     * ?????????????????????????????????
     *
     * @param retIds
     * @param iptTree
     * @param auth
     * @return
     */
    private List<SysIptVo> querySearchIptTree(List<Integer> retIds, List<SysIptVo> iptTree, boolean auth) {
        List<SysIptVo> retList = new ArrayList<>();

        if (!ObjectUtils.isEmpty(iptTree)) {
            //????????????id??????????????????????????????
            for (SysIptVo sysIptVo : iptTree) {
                boolean thisAuth = auth;
                if (!thisAuth && retIds.contains(sysIptVo.getId())) {
                    thisAuth = true;
                }
                //?????????????????????
                List<SysIptVo> childrenList = this.querySearchIptTree(retIds, sysIptVo.getChildren(), thisAuth);

                if (thisAuth || !ObjectUtils.isEmpty(childrenList)) {
                    SysIptVo retIptVo = new SysIptVo();
                    this.dozerMapper.map(sysIptVo, retIptVo);
                    if (!ObjectUtils.isEmpty(childrenList))
                        retIptVo.setChildren(childrenList);
                    retList.add(retIptVo);
                }
            }
        }
        return retList;
    }

    @Override
    public void deleteIptUser(List<Integer> userIds, Integer iptId) {

        if (ObjectUtils.isEmpty(userIds)) {
            throw new BaseException("???????????????!");
        }

        sysUserIptRoleService.deleteIptUserByUserIdAndIptId(userIds, iptId);
        sysUserIptService.deleteIptUserByUserIdAndIptId(userIds, iptId);
    }

    /**
     * ipt????????????????????????
     * @param list
     * @param iptId
     * @return
     */
    @Override
    public String queryAssignIptUserLogger(List<SysUserIptAddForm> list, Integer iptId) {
        List<Integer> userIds = new ArrayList<>();
        SysIptPo sysIptPo = super.selectById(iptId);
        if(!ObjectUtils.isEmpty(list)){
            for (SysUserIptAddForm sysUserIptAddForm : list){
                userIds.add(sysUserIptAddForm.getUserId());
            }
            String userNames = sysUserService.queryUserNamesByIds(userIds);
            String logger = "??????\""+sysIptPo.getIptName()+"\"?????????" + userNames;
            return logger;
        }
        return "";
    }

    /**
     * ipt????????????????????????
     * @param userIds
     * @param iptId
     * @return
     */
    @Override
    public String queryDeleteIptUserLogger(List<Integer> userIds, Integer iptId) {
        String userNames = sysUserService.queryUserNamesByIds(userIds);
        SysIptPo sysIptPo = super.selectById(iptId);
        String logger = "??????\""+sysIptPo.getIptName()+"\"?????????" + userNames;
        return logger;
    }
}
