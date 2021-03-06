package com.wisdom.acm.sys.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.wisdom.acm.sys.form.SysUpdatePassWordForm;
import com.wisdom.acm.sys.form.SysUserAddBatchFrom;
import com.wisdom.acm.sys.form.SysUserAddFrom;
import com.wisdom.acm.sys.form.SysUserSearchForm;
import com.wisdom.acm.sys.form.SysUserUpdateFrom;
import com.wisdom.acm.sys.form.UserLevelSearchForm;
import com.wisdom.acm.sys.form.UserLevelUpdateForm;
import com.wisdom.acm.sys.mapper.UserMapper;
import com.wisdom.acm.sys.po.SysOrgPo;
import com.wisdom.acm.sys.po.SysUserOrgPo;
import com.wisdom.acm.sys.po.SysUserOrgRolePo;
import com.wisdom.acm.sys.po.SysUserPo;
import com.wisdom.acm.sys.service.SysIPAccRuleService;
import com.wisdom.acm.sys.service.SysLoginService;
import com.wisdom.acm.sys.service.SysOrgService;
import com.wisdom.acm.sys.service.SysPwdRuleService;
import com.wisdom.acm.sys.service.SysUserIptRoleService;
import com.wisdom.acm.sys.service.SysUserIptService;
import com.wisdom.acm.sys.service.SysUserOrgRoleService;
import com.wisdom.acm.sys.service.SysUserOrgService;
import com.wisdom.acm.sys.service.SysUserService;
import com.wisdom.acm.sys.vo.SysAllUserVo;
import com.wisdom.acm.sys.vo.SysIPAccessVo;
import com.wisdom.acm.sys.vo.SysIptUserVo;
import com.wisdom.acm.sys.vo.SysPwdRuleVo;
import com.wisdom.acm.sys.vo.SysRoleVo;
import com.wisdom.acm.sys.vo.SysUserInfoVo;
import com.wisdom.acm.sys.vo.SysUserIptRoleVo;
import com.wisdom.acm.sys.vo.SysUserOrgRoleVo;
import com.wisdom.acm.sys.vo.SysUserProjectInfoVo;
import com.wisdom.acm.sys.vo.SysUserVo;
import com.wisdom.acm.sys.vo.SystemVo;
import com.wisdom.acm.sys.vo.UserLevelVo;
import com.wisdom.base.common.aspect.AddLog;
import com.wisdom.base.common.enums.HMACEnum;
import com.wisdom.base.common.enums.LoggerModuleEnum;
import com.wisdom.base.common.exception.BaseException;
import com.wisdom.base.common.feign.CommDictService;
import com.wisdom.base.common.service.BaseService;
import com.wisdom.base.common.util.DateUtil;
import com.wisdom.base.common.util.HMACUtil;
import com.wisdom.base.common.util.IPUtil;
import com.wisdom.base.common.util.ListUtil;
import com.wisdom.base.common.util.PageUtil;
import com.wisdom.base.common.vo.DictionarysMap;
import com.wisdom.base.common.vo.GeneralVo;
import com.wisdom.base.common.vo.SelectVo;
import com.wisdom.base.common.vo.UserInfo;
import com.wisdom.base.common.vo.UserVo;
import com.wisdom.base.common.vo.sys.UserOrgVo;
import com.wisdom.cache.annotation.CacheClear;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class SysUserServiceImpl extends BaseService<UserMapper, SysUserPo> implements SysUserService {

    @Autowired
    private SysUserOrgService sysOrgUserService;

    @Autowired
    private SysUserOrgRoleService sysUserOrgRoleService;


    @Autowired
    private SysUserIptService sysUserIptService;

    @Autowired
    private SysUserIptRoleService sysUserIptRoleService;

    @Autowired
    private SysPwdRuleService sysPwdRuleService;

    @Autowired
    private SysIPAccRuleService sysIPAccRuleService;

    @Autowired(required = false)
    private CommDictService commDictService;

    @Autowired
    private SysLoginService sysLoginService;

    @Autowired
    private SystemVo systemVo;

    @Autowired
    private SysOrgService orgService;

    /**
     * ???activiti??????
     * @param userId
     * @return
     */
    @Override
    public GeneralVo selectUserInfoForAct(String userId) {
        return mapper.selectUserInfoForAct(userId);
    }

    //    @Cache(key = "acm:users:search{1}")
    @Override
    public PageInfo<SysUserVo> queryUserList(SysUserSearchForm searchMap, Integer pageSize, Integer currentPageNum) {
        //????????????
        PageHelper.startPage(currentPageNum, pageSize);
        List<SysUserVo> sysUserVoList = mapper.selectUsers(searchMap);
        PageInfo<SysUserVo> pageInfo = new PageInfo<>(sysUserVoList);

        //???????????????????????????
        List<SysUserVo> userList = pageInfo.getList();

        if (!ObjectUtils.isEmpty(userList)) {
            //????????????id??????
            List<Integer> ids = new ArrayList<>();
            for (SysUserVo sysUserVo : userList) {
                ids.add(sysUserVo.getId());
            }
            //??????????????????
            List<SysUserOrgRoleVo> roles = sysUserOrgRoleService.queryUserRoleByUserId(ids);
            Map<Integer, List<SysUserOrgRoleVo>> roleMap = ListUtil.bulidTreeListMap(roles, "userId", Integer.class);
            SysRoleVo sysRoleVo1 = null;
            List<SysRoleVo> roleVos1 = null;
            //????????????????????????
            for (SysUserVo sysUserVo : userList) {
                if (!ObjectUtils.isEmpty(roleMap.get(sysUserVo.getId()))) {
                    roleVos1 = new ArrayList<>();
                    for (SysUserOrgRoleVo sysUserOrgRoleVo : roleMap.get(sysUserVo.getId())) {
                        sysRoleVo1 = new SysRoleVo();
                        sysRoleVo1.setId(sysUserOrgRoleVo.getRoleId());
                        sysRoleVo1.setRoleName(sysUserOrgRoleVo.getRoleName());
                        roleVos1.add(sysRoleVo1);
                    }
                    sysUserVo.setRoles(roleVos1);
                }
            }
            //????????????????????????
            List<SysRoleVo> roleVos = null;
            if (!ObjectUtils.isEmpty(userList)) {
                for (SysUserVo sysUserVo : userList) {
                    roleVos = sysUserVo.getRoles();
                    StringBuffer str = new StringBuffer();
                    if (!ObjectUtils.isEmpty(roleVos)) {
                        for (SysRoleVo sysRoleVo : roleVos) {
                            str.append("," + sysRoleVo.getRoleName());
                        }
                        String retRole = str.substring(1);
                        sysUserVo.setRetRole(retRole);
                        sysUserVo.setRoles(null);
                    }
                }
            }
        }
        return pageInfo;
    }

    @Override
    public SysUserVo queryUserById(Integer id) {
        SysUserVo sysUserVo = mapper.selectUserByUserId(id);
        if (!ObjectUtils.isEmpty(sysUserVo)) {
            //????????????id??????
            List<Integer> ids = new ArrayList<>();
            ids.add(sysUserVo.getId());
            //??????????????????
            List<SysUserOrgRoleVo> roles = sysUserOrgRoleService.queryUserRoleByUserId(ids);
            SysRoleVo sysRoleVo1 = null;
            List<SysRoleVo> roleVos1 = null;
            //????????????????????????
            if (!ObjectUtils.isEmpty(roles)) {
                roleVos1 = new ArrayList<>();
                for (SysUserOrgRoleVo sysUserOrgRoleVo : roles) {
                    sysRoleVo1 = new SysRoleVo();
                    sysRoleVo1.setId(sysUserOrgRoleVo.getRoleId());
                    sysRoleVo1.setRoleName(sysUserOrgRoleVo.getRoleName());
                    roleVos1.add(sysRoleVo1);
                }
            }
            sysUserVo.setRoles(roleVos1);

            //????????????????????????
            List<SysRoleVo> roleVos = null;
            roleVos = sysUserVo.getRoles();
            StringBuffer str = new StringBuffer();
            if (!ObjectUtils.isEmpty(roleVos)) {
                for (SysRoleVo sysRoleVo : roleVos) {
                    str.append("," + sysRoleVo.getRoleName());
                }
                String retRole = str.substring(1);
                sysUserVo.setRetRole(retRole);
                sysUserVo.setRoles(null);
            }


        }
        return sysUserVo;
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @return
     */
    @Override
    public SysUserInfoVo getUserInfo(Integer userId) {
        SysUserInfoVo sysUserPoVo = mapper.selectUserInfo(userId);
        GeneralVo sysMainUserVo = mapper.selectUserMainOrg(userId);
//        if (ObjectUtils.isEmpty(sysMainUserVo)) {
//            throw new BaseException("????????????????????????????????????");
//        }
        List<SysRoleVo> roles = null;
        if (!ObjectUtils.isEmpty(sysMainUserVo)) {
            roles = sysUserOrgRoleService.queryRoleListByOrgIdAndUserId(sysMainUserVo.getId(), userId);
            sysUserPoVo.setOrg(sysMainUserVo);
            sysUserPoVo.setRoles(roles);
        }
        List<String> dictList = new ArrayList<>();
        dictList.add("comm.secutylevel");
        dictList.add("sys.user.cardtype");
        DictionarysMap dictionarysMap = commDictService.getDictMapByTypeCodes(dictList);
        if (!ObjectUtils.isEmpty(dictionarysMap) && !ObjectUtils.isEmpty(sysUserPoVo)) {
            if (!ObjectUtils.isEmpty(sysUserPoVo.getLevel())) {
                sysUserPoVo.getLevel().setName(dictionarysMap.getDictionaryName("comm.secutylevel", sysUserPoVo.getLevel().getCode()));
            }
            if (!ObjectUtils.isEmpty(sysUserPoVo.getCardType())) {
                sysUserPoVo.getCardType().setName(dictionarysMap.getDictionaryName("sys.user.cardtype", sysUserPoVo.getCardType().getCode()));
            }
        }
        return sysUserPoVo;
    }

    @Override
    public SysUserInfoVo getUserDept(Integer userId) {
        SysUserInfoVo sysUserPoVo = mapper.selectUserInfo(userId);
        GeneralVo sysMainUserVo = mapper.selectUserMainOrg(userId);
        return null;
    }

    /**
     * ????????????
     *
     * @param sysUserAddFrom
     * @return
     */
    @Override
    public SysUserPo addUser(SysUserAddFrom sysUserAddFrom) {
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getSort())) {
            sysUserAddFrom.setSort(mapper.selectNextSort());
        }
        SysUserPo sysUserPo = dozerMapper.map(sysUserAddFrom, SysUserPo.class);
        SysUserPo sysUserPoIs = mapper.selectUserByUserName(sysUserPo.getUserName());
        if (!ObjectUtils.isEmpty(sysUserPoIs)) {
            throw new BaseException("??????????????????");
        }
        //??????uuv ??????????????????  status ??????StaffStatus ??????????????????StaffStatus????????????
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getStaffStatus())) {
            if (1 == sysUserAddFrom.getStaffStatus()) {
                sysUserPo.setStatus(1);
            } else {
                sysUserPo.setStatus(0);
            }
            sysUserPo.setStaffStatus("1");
            sysUserAddFrom.setStaffStatus(1);
        }
        if (ObjectUtils.isEmpty(sysUserPo.getStatus())) {
            sysUserPo.setStatus(1);
        }

        if (ObjectUtils.isEmpty(sysUserAddFrom.getSort())) {
            sysUserPo.setSort(mapper.selectNextSort());
        }
        //????????????
        String pwd = null;
        if (ObjectUtils.isEmpty(sysUserPo.getPassword())) {
            pwd = HMACUtil.getHMAC("123456", HMACEnum.HMAC_KEY.getType());
            sysUserPo.setPassword(pwd);
        }else{
            pwd = HMACUtil.getHMAC(sysUserPo.getPassword(), HMACEnum.HMAC_KEY.getType());
            sysUserPo.setPassword(pwd);
        }
        sysUserPo.setUserType("1");
        sysUserPo.setUpdatePwdTime(new Date());
        //????????????????????????
        super.insert(sysUserPo);

        Integer userId = sysUserPo.getId();
        SysUserOrgPo sysOrgUserPo = new SysUserOrgPo();
        SysUserOrgRolePo sysRoleUserPo = null;

        sysOrgUserPo.setOrgId(sysUserAddFrom.getOrgId());
        sysOrgUserPo.setUserId(sysUserPo.getId());
        sysOrgUserPo.setMainOrg(1);

        //????????????????????????
        sysOrgUserService.addOrgUserRelation(sysOrgUserPo);

        //????????????????????????
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getRoles())) {
            for (Integer roleId : sysUserAddFrom.getRoles()) {
                sysRoleUserPo = new SysUserOrgRolePo();
                sysRoleUserPo.setRoleId(roleId);
                sysRoleUserPo.setUserId(sysUserPo.getId());
                sysRoleUserPo.setOrgId(sysUserAddFrom.getOrgId());
                sysUserOrgRoleService.addUserOrgRoleRelation(sysRoleUserPo);
            }
        }
        return sysUserPo;
    }

    /**
     * ????????????
     *
     * @param sysUserUpdateVo
     * @return
     */
//    @CacheClear(keys = {"acm:users"})
    @Override
    @AddLog(title = "????????????" , module = LoggerModuleEnum.SM_USER)
    public SysUserPo updateUser(SysUserUpdateFrom sysUserUpdateVo) {
        SysUserPo sysUserPo = mapper.selectByPrimaryKey(sysUserUpdateVo.getId());
        if (ObjectUtils.isEmpty(sysUserPo)) {
            throw new BaseException("??????????????????");
        }

        // ??????????????????
        this.addChangeLogger(sysUserUpdateVo,sysUserPo);
        String userName = sysUserUpdateVo.getUserName();
        SysUserPo sysUserPoIs = mapper.selectUserByUserName(userName);
        if (!ObjectUtils.isEmpty(sysUserPoIs) && !sysUserPoIs.getId().equals(sysUserUpdateVo.getId())) {
            throw new BaseException("??????????????????");
        }

        //??????uuv ??????????????????  status ??????StaffStatus ??????????????????StaffStatus????????????
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getStaffStatus())) {
            if (1 == sysUserUpdateVo.getStaffStatus()) {
                sysUserPo.setStatus(1);
            } else {
                sysUserPo.setStatus(0);
            }
            sysUserPo.setStaffStatus("1");
            sysUserUpdateVo.setStaffStatus(1);
        }

        //?????????????????????
        GeneralVo sysMainUserVo = mapper.selectUserMainOrg(sysUserUpdateVo.getId());
        Integer oldOrgId = null;
        if (!ObjectUtils.isEmpty(sysMainUserVo))
            oldOrgId = sysMainUserVo.getId();
        //????????????????????????
        dozerMapper.map(sysUserUpdateVo, sysUserPo);
        super.updateSelectiveById(sysUserPo);

        //??????????????????
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getOrgId()) && !sysUserUpdateVo.getOrgId().equals(oldOrgId)) {
            List<Integer> userIds = new ArrayList<>();
            userIds.add(sysUserUpdateVo.getId());
            SysUserOrgPo sysUserOrgPo = new SysUserOrgPo();
            sysUserOrgPo.setUserId(sysUserUpdateVo.getId());
            sysUserOrgPo.setOrgId(sysUserUpdateVo.getOrgId());
            sysUserOrgPo.setMainOrg(1);
            sysOrgUserService.deleteUserOrgRelationByUserIdAndOrgId(userIds, oldOrgId);
            sysOrgUserService.addOrgUserRelation(sysUserOrgPo);
        }

        //??????????????????
        SysUserOrgRolePo sysRoleUserPo = null;
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getRoles())) {
            mapper.deleRoleUserByOrgId(sysUserUpdateVo.getId(), oldOrgId);
            for (Integer roleId : sysUserUpdateVo.getRoles()) {
                sysRoleUserPo = new SysUserOrgRolePo();
                sysRoleUserPo.setUserId(sysUserUpdateVo.getId());
                sysRoleUserPo.setOrgId(sysUserUpdateVo.getOrgId());
                sysRoleUserPo.setRoleId(roleId);
                sysUserOrgRoleService.addUserOrgRoleRelation(sysRoleUserPo);
            }
        }

        return sysUserPo;
    }

    @Override
    public SysUserPo updateUserLastLoginTime(SysUserPo sysUserPo) {
        sysUserPo.setLastloginTime(new Date());
        mapper.updateUserLastLoginTime(sysUserPo);
        return null;
    }

    /**
     * ???uuv????????????
     *
     * @param sysUserAddFrom
     * @return
     */
    @Override
    public SysUserPo xinZengUser(SysUserAddFrom sysUserAddFrom) {
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getSort())) {
            sysUserAddFrom.setSort(mapper.selectNextSort());
        }
        SysUserPo sysUserPo = dozerMapper.map(sysUserAddFrom, SysUserPo.class);
        SysUserPo sysUserPoIs = mapper.selectUserByUserName(sysUserPo.getUserName());
        if (!ObjectUtils.isEmpty(sysUserPoIs)) {
            throw new BaseException("??????????????????");
        }
        //??????uuv ??????????????????  status ??????StaffStatus ??????????????????StaffStatus????????????
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getStaffStatus())) {
            if (1 == sysUserAddFrom.getStaffStatus()) {
                sysUserPo.setStatus(1);
            } else {
                sysUserPo.setStatus(0);
            }
            sysUserPo.setStaffStatus("1");
            sysUserAddFrom.setStaffStatus(1);
        }
        if (ObjectUtils.isEmpty(sysUserPo.getStatus())) {
            sysUserPo.setStatus(1);
        }

        if (ObjectUtils.isEmpty(sysUserAddFrom.getSort())) {
            sysUserPo.setSort(mapper.selectNextSort());
        }
        //????????????
        String pwd = null;
        if (ObjectUtils.isEmpty(sysUserPo.getPassword())) {
            pwd = HMACUtil.getHMAC("123456", HMACEnum.HMAC_KEY.getType());
            sysUserPo.setPassword(pwd);
        }else{
            pwd = HMACUtil.getHMAC(sysUserPo.getPassword(), HMACEnum.HMAC_KEY.getType());
            sysUserPo.setPassword(pwd);
        }
        sysUserPo.setUserType("1");
        sysUserPo.setUpdatePwdTime(new Date());
        //????????????????????????
        super.insert(sysUserPo);

        Integer userId = sysUserPo.getId();
        SysUserOrgPo sysOrgUserPo = new SysUserOrgPo();
        SysUserOrgRolePo sysRoleUserPo = null;

        sysOrgUserPo.setOrgId(sysUserAddFrom.getOrgId());
        sysOrgUserPo.setUserId(sysUserPo.getId());
        sysOrgUserPo.setMainOrg(1);

        //????????????????????????
        sysOrgUserService.addOrgUserRelation(sysOrgUserPo);

        //????????????????????????
        if (!ObjectUtils.isEmpty(sysUserAddFrom.getRoles())) {
            for (Integer roleId : sysUserAddFrom.getRoles()) {
                sysRoleUserPo = new SysUserOrgRolePo();
                sysRoleUserPo.setRoleId(roleId);
                sysRoleUserPo.setUserId(sysUserPo.getId());
                sysRoleUserPo.setOrgId(sysUserAddFrom.getOrgId());
                sysUserOrgRoleService.addUserOrgRoleRelation(sysRoleUserPo);
            }
        }
        return sysUserPo;
    }

    /**
     * ???uuv????????????
     *
     * @param sysUserUpdateVo
     * @return
     */
//    @CacheClear(keys = {"acm:users"})
    @Override
    public SysUserPo xiuGaiUser(SysUserUpdateFrom sysUserUpdateVo) {
        SysUserPo sysUserPo = mapper.selectByPrimaryKey(sysUserUpdateVo.getId());
        if (ObjectUtils.isEmpty(sysUserPo)) {
            throw new BaseException("??????????????????");
        }

        // ??????????????????
        this.addChangeLogger(sysUserUpdateVo,sysUserPo);
        String userName = sysUserUpdateVo.getUserName();
        SysUserPo sysUserPoIs = mapper.selectUserByUserName(userName);
        if (!ObjectUtils.isEmpty(sysUserPoIs) && !sysUserPoIs.getId().equals(sysUserUpdateVo.getId())) {
            throw new BaseException("??????????????????");
        }

        //?????????????????????
        GeneralVo sysMainUserVo = mapper.selectUserMainOrg(sysUserUpdateVo.getId());
        Integer oldOrgId = null;
        if (!ObjectUtils.isEmpty(sysMainUserVo))
            oldOrgId = sysMainUserVo.getId();
        //????????????????????????
        dozerMapper.map(sysUserUpdateVo, sysUserPo);

        //??????uuv ??????????????????  status ??????StaffStatus ??????????????????StaffStatus????????????
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getStaffStatus())) {
            if (1 == sysUserUpdateVo.getStaffStatus()) {
                sysUserPo.setStatus(1);
            } else {
                sysUserPo.setStatus(0);
            }
            sysUserPo.setStaffStatus("1");
            sysUserUpdateVo.setStaffStatus(1);
        }
        super.updateSelectiveById(sysUserPo);

        //??????????????????
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getOrgId()) && !sysUserUpdateVo.getOrgId().equals(oldOrgId)) {
            List<Integer> userIds = new ArrayList<>();
            userIds.add(sysUserUpdateVo.getId());
            SysUserOrgPo sysUserOrgPo = new SysUserOrgPo();
            sysUserOrgPo.setUserId(sysUserUpdateVo.getId());
            sysUserOrgPo.setOrgId(sysUserUpdateVo.getOrgId());
            sysUserOrgPo.setMainOrg(1);
            sysOrgUserService.deleteUserOrgRelationByUserIdAndOrgId(userIds, oldOrgId);
            sysOrgUserService.addOrgUserRelation(sysUserOrgPo);
        }

        //??????????????????
        SysUserOrgRolePo sysRoleUserPo = null;
        if (!ObjectUtils.isEmpty(sysUserUpdateVo.getRoles())) {
            mapper.deleRoleUserByOrgId(sysUserUpdateVo.getId(), oldOrgId);
            for (Integer roleId : sysUserUpdateVo.getRoles()) {
                sysRoleUserPo = new SysUserOrgRolePo();
                sysRoleUserPo.setUserId(sysUserUpdateVo.getId());
                sysRoleUserPo.setOrgId(sysUserUpdateVo.getOrgId());
                sysRoleUserPo.setRoleId(roleId);
                sysUserOrgRoleService.addUserOrgRoleRelation(sysRoleUserPo);
            }
        }

        return sysUserPo;
    }

    /**
     * ????????????
     *
     * @param userIds
     * @return
     */
    @CacheClear(keys = {"acm:users"})
    @Override
    public void deleteUser(List<Integer> userIds) {
        sysOrgUserService.deleteUserOrgRelationByUserIds(userIds);
        sysUserOrgRoleService.deleteUserOrgRoleRelationByUserIds(userIds);
        sysUserIptService.deleteUserIptRelationByUserIds(userIds);
        sysUserIptRoleService.deleteUserIptRelationByUserIds(userIds);
        super.deleteByIds(userIds);
    }

    /**
     * ??????????????????
     *
     * @param userList
     * @return
     */
    @Override
    //@Transactional
    public List<Integer> addUserBatch(List<SysUserAddBatchFrom> userList) {
        List<Integer> userIds = new ArrayList<>();
        for (SysUserAddBatchFrom sysUserAddFrom : userList) {
            SysUserPo sysUserPo = dozerMapper.map(sysUserAddFrom, SysUserPo.class);
            SysUserPo sysUserPoIs = mapper.selectUserByUserName(sysUserPo.getUserName());
            if (!ObjectUtils.isEmpty(sysUserPoIs)) {
                throw new BaseException("??????????????????");
            }
            if (ObjectUtils.isEmpty(sysUserPo.getStatus())) {
                sysUserPo.setStatus(1);
            }
            //????????????
            String pwd = null;

            pwd = HMACUtil.getHMAC(sysUserAddFrom.getPassword(), HMACEnum.HMAC_KEY.getType());
            sysUserPo.setPassword(pwd);

            sysUserPo.setStaffStatus("1");
            sysUserPo.setSort(mapper.selectNextSort());
            sysUserPo.setUserType("1");
            sysUserPo.setUpdatePwdTime(new Date());
            //????????????????????????
            super.insert(sysUserPo);
            userIds.add(sysUserPo.getId());
            Integer userId = sysUserPo.getId();
            SysUserOrgPo sysOrgUserPo = new SysUserOrgPo();
            SysUserOrgRolePo sysRoleUserPo = null;

            sysOrgUserPo.setOrgId(sysUserAddFrom.getOrgId());
            sysOrgUserPo.setUserId(sysUserPo.getId());
            sysOrgUserPo.setMainOrg(1);

            //????????????????????????
            sysOrgUserService.addOrgUserRelation(sysOrgUserPo);

            //????????????????????????
            if (!ObjectUtils.isEmpty(sysUserAddFrom.getRoles())) {
                for (Integer roleId : sysUserAddFrom.getRoles()) {
                    sysRoleUserPo = new SysUserOrgRolePo();
                    sysRoleUserPo.setRoleId(roleId);
                    sysRoleUserPo.setUserId(sysUserPo.getId());
                    sysRoleUserPo.setOrgId(sysUserAddFrom.getOrgId());
                    sysUserOrgRoleService.addUserOrgRoleRelation(sysRoleUserPo);
                }
            }
        }
        return userIds;
    }


    /**
     * ??????????????????
     *
     * @param orgId
     * @return
     */
    @Override
    public List<SysUserVo> selectUsersByOrgId(Integer orgId) {
        List<SysUserVo> list = mapper.selectUsersByOrgId(orgId);
        return list;
    }

    /**
     * ????????????????????????
     *
     * @param id
     * @param pageSize
     * @param curragenum
     * @return
     */
    @Override
    public PageInfo<SysUserVo> queryUsersByOrgIdPage(Integer id, Integer pageSize, Integer curragenum) {
        PageHelper.startPage(curragenum, pageSize);
        List<SysUserVo> list = mapper.selectUsersByOrgId(id);
        PageInfo<SysUserVo> pageInfo = new PageInfo<>(list);
        List<SysRoleVo> roleVos = null;
        if (!ObjectUtils.isEmpty(pageInfo.getList())) {
            for (SysUserVo sysUserVo : pageInfo.getList()) {
                roleVos = sysUserVo.getRoles();
                StringBuffer str = new StringBuffer();
                if (!ObjectUtils.isEmpty(roleVos)) {
                    for (SysRoleVo sysRoleVo : roleVos) {
                        str.append("," + sysRoleVo.getRoleName());
                    }
                    String retRole = str.substring(1);
                    sysUserVo.setRetRole(retRole);
                    sysUserVo.setRoles(null);
                }
            }
        }
        return pageInfo;
    }

    /**
     * ??????ipt????????????
     *
     * @param iptId
     * @return
     */
    @Override
    public PageInfo<SysIptUserVo> queryUsersByIptIdPage(Integer iptId, Integer pageSize, Integer currentPageNum) {
        PageHelper.startPage(currentPageNum, pageSize);
        List<SysIptUserVo> list = mapper.selectUsersByIptId(iptId);
        PageInfo<SysIptUserVo> pageInfo = new PageInfo<>(list);
        List<SysIptUserVo> userIptPos = pageInfo.getList();
        if (!ObjectUtils.isEmpty(userIptPos)) {
            List<Integer> ids = new ArrayList<>();
            for (SysIptUserVo sysIptUserVo : userIptPos) {
                ids.add(sysIptUserVo.getId());
            }
            List<SysUserIptRoleVo> roles = sysUserIptRoleService.queryUserIptRoleRelationByUserId(ids, iptId);
            Map<Integer, List<SysUserIptRoleVo>> roleMap = ListUtil.bulidTreeListMap(roles, "userId", Integer.class);
            GeneralVo generalVo = null;
            List<GeneralVo> generalVos = null;
            for (SysIptUserVo sysIptUserVo : userIptPos) {
                generalVos = new ArrayList<>();
                if (!ObjectUtils.isEmpty(roleMap.get(sysIptUserVo.getId()))) {
                    for (SysUserIptRoleVo sysUserIptRoleVo : roleMap.get(sysIptUserVo.getId())) {
                        generalVo = new GeneralVo();
                        generalVo.setId(sysUserIptRoleVo.getRoleId());
                        generalVo.setName(sysUserIptRoleVo.getRoleName());
                        generalVos.add(generalVo);
                    }
                    sysIptUserVo.setRole(generalVos);
                }
            }
        }
        return pageInfo;
    }


    @Override
    public UserInfo getUserInfoByName(String userName) {

        // ????????????????????????????????????
        SysUserPo userPo = this.getUserPoByName(userName);
        if (userPo != null) {
            // PO???????????????userInfo???
            UserInfo userInfo = this.dozerMapper.map(userPo, UserInfo.class);
            // ????????????ID???????????????orgIds??????
            List<Integer> orgIds = sysOrgUserService.queryOrgIdsByUserId(userInfo.getId());
            userInfo.setOrgIds(orgIds);

            return userInfo;
        }
        return null;
    }


    @Override
    public UserInfo getUserInfoByCode(String userCode) {
        // ????????????????????????????????????
        Example example = new Example(SysUserPo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userCode", userCode);
        SysUserPo userPo = this.selectOneByExample(example);
        if (userPo != null) {
            // PO???????????????userInfo???
            UserInfo userInfo = this.dozerMapper.map(userPo, UserInfo.class);
            // ????????????ID???????????????orgIds??????
            List<Integer> orgIds = sysOrgUserService.queryOrgIdsByUserId(userInfo.getId());
            userInfo.setOrgIds(orgIds);

            return userInfo;
        }
        return null;
    }

    @Override
    public SysUserPo selectUserByUserName(String userName) {
        return mapper.selectUserByUserName(userName);
    }

    @Override
    public SysUserPo getUserPoByName(String userName) {

        Example example = new Example(SysUserPo.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("userName", userName);

        List<SysUserPo> list = this.mapper.selectByExample(example);
        return !ObjectUtils.isEmpty(list) ? list.get(0) : null;

    }

    /**
     * ??????????????????id??????????????????
     *
     * @param userIds
     * @return
     */
    @Override
    public List<SysUserVo> queryUsersByIds(List<Integer> userIds) {
        List<SysUserVo> list = mapper.selectUsersByIds(userIds);
        return list;
    }

    /**
     * ??????????????????id??????????????????
     *
     * @param userIds
     * @return
     */
    @Override
    public Map<Integer, UserVo> queryUserVoByUserIds(List<Integer> userIds) {

        List<UserVo> userVos = new ArrayList<>();
        List<SysUserPo> userVoList = this.selectByIds(userIds);
        if (!ObjectUtils.isEmpty(userVoList)) {
            userVoList.forEach(user -> {
                UserVo u = new UserVo(user.getId(), user.getUserName(), user.getActuName(),user.getUserCode());
                userVos.add(u);
            });
        }
        return ListUtil.listToMap(userVos, "id", Integer.class);
    }

    /**
     * ??????????????????
     *
     * @param userName
     * @param password
     * @param hostIp
     * @return
     */
    @Override
    public UserInfo validate(String userName, String password, String hostIp) {
        String pwd = HMACUtil.getHMAC(password, HMACEnum.HMAC_KEY.getType());
        // ????????????????????????????????????????????????????????????????????????
        SysUserPo userPo = this.getUserPoByName(userName);
        if (ObjectUtils.isEmpty(userPo)){
            throw new BaseException("??????????????????!");
        }
        //??????????????????????????? ??????????????????????????????
        if (!ObjectUtils.isEmpty(userPo.getUserCode())) {
            if (!userPo.getUserCode().contains("UVU")) {
                throw new BaseException("?????????????????????????????????????????????!!");
            }
        }
        // ???????????????????????????
        if (userPo.getStatus().equals(0) && ObjectUtils.isEmpty(userPo.getErrotTime())){
            throw new BaseException("????????????????????????????????????!");
        }
        // ??????????????????
        SysPwdRuleVo sysPwdRuleVo = sysPwdRuleService.queryPwdRuleAll();
        // ????????????????????????????????????????????????????????????????????????????????????
        int errorNum = 5;
        int time = 30;
        if(sysPwdRuleVo != null && sysPwdRuleVo.getErrorNumber() != null){
            errorNum = sysPwdRuleVo.getErrorNumber();
            time = sysPwdRuleVo.getLockTime();
        }

        if(!ObjectUtils.isEmpty(userPo.getErrotTime())){
            int timeDiff = DateUtil.getTimeChaByTwoTime(userPo.getErrotTime(), new Date());
            // ???????????????????????????????????????????????????????????????????????????
            if(time <= timeDiff){
                userPo.setStatus(1);
                userPo.setErrorNumber(0);
                userPo.setErrotTime(null);
                super.updateById(userPo);
            }
            // ???????????????????????????????????????????????????????????????????????????
            if(userPo.getErrorNumber() >= errorNum ){
                userPo.setStatus(0);
                super.updateById(userPo);
                throw new BaseException("?????????????????????,??????" + (time - timeDiff) + "?????????????????????");
            }
        }
        // ????????????????????????????????????
        if(!pwd.equals(userPo.getPassword())){
            int num = this.validatePwdUpdateCount(userPo, errorNum , time);
            
            if(num == errorNum){
                throw new BaseException("??????????????????????????????"+errorNum+"????????????????????????????????????" + time + "?????????????????????");
            }

            throw new BaseException("??????????????????!????????????" + num + "???????????????" + (sysPwdRuleVo.getErrorNumber() - num) + "");
        }

        UserInfo userInfo = mapper.getUserInfoByUsername(userName,pwd);
        this._validate(userInfo, hostIp);
        return userInfo;
    }

    /**
     * ????????????????????????
     *
     * @param userName
     * @param password
     * @param hostIp
     * @return
     */
    @Override
    public UserInfo snValidate(String userName, String password, String hostIp) {
        UserInfo user = mapper.getUserInfoByUsername(userName, password);
        this._validate(user, hostIp);
        return user;
    }

    /**
     * ??????????????????
     *
     * @param user
     * @param hostIp
     * @return
     */
    private void _validate(UserInfo user, String hostIp) {
        SysUserPo userPo = this.getUserPoByName(user.getUserName());

        //???????????????????????????
        if (ObjectUtils.isEmpty(userPo)) {
            throw new BaseException("????????????????????????????????????");
        }
        //????????????????????????
        Integer tmm = systemVo.getEnable();
        if((tmm == 1 && userPo.getUserName().equals("admin"))){
            throw new BaseException("?????????????????????");
        }
        if (tmm == 1) {
            //????????????????????????
            SysPwdRuleVo sysPwdRuleVo = sysPwdRuleService.queryPwdRuleAll();
            //??????ip??????????????????
            boolean flag = this.validateIpAccess(hostIp);
            if (flag) {
                throw new BaseException("?????????IP?????????@@");
            }
            //????????????????????????
            //this.validatePwdLockTime(userPo, sysPwdRuleVo);
            if (ObjectUtils.isEmpty(userPo)){
                throw new BaseException("????????????????????????????????????!O(???_???)O~???");
            }
            //????????????????????????
//            this.validateUpdatePwdCycly(userPo, sysPwdRuleVo);
//            if (ObjectUtils.isEmpty(user) && !ObjectUtils.isEmpty(userPo)) {
//                //????????????????????????
//                int num = this.validatePwdUpdateCount(userPo, sysPwdRuleVo.getErrorNumber(),sysPwdRuleVo.getLockTime());
//                throw new BaseException("??????????????????!????????????" + num + "???????????????" + (sysPwdRuleVo.getErrorNumber() - num) + "");
//            }

        } else {
            if (ObjectUtils.isEmpty(user)) {
                throw new BaseException("????????????????????????????????????!");
            }
        }
        if (tmm == 0 && (userPo.getUserType().equals("3") || userPo.getUserType().equals("4"))) {
            throw new BaseException("???????????????????????????????????????????????????");
        }
        userPo.setErrorNumber(0);
        // userPo.setLastloginTime(new Date()); ???????????????????????????????????????
        Integer visitNum = userPo.getVisits() == null ? 0 : userPo.getVisits();
        userPo.setVisits(visitNum+1);
        super.updateById(userPo);
    }

    private boolean validateIpAccess(String hostIp) {
        List<SysIPAccessVo> rules = sysIPAccRuleService.queryIPAccByIsEffect();
        boolean flag = false;
        if (!ObjectUtils.isEmpty(rules)) {
            for (SysIPAccessVo sysIPAccessVo : rules) {
                Long userIp = IPUtil.getIpNum(hostIp);
                Long startIp = IPUtil.getIpNum(sysIPAccessVo.getStartIP());
                Long endIp = IPUtil.getIpNum(sysIPAccessVo.getEndIP());
                flag = IPUtil.isInner(userIp, startIp, endIp);
                if (flag) {
                    return flag;
                }
            }
        }
        return flag;
    }

    /**
     * ????????????????????????
     *
     * @param userPo
     * @param sysPwdRuleVo
     */
    private void validateUpdatePwdCycly(SysUserPo userPo, SysPwdRuleVo sysPwdRuleVo) {
        int time = DateUtil.getDaysChaByTwoTime(userPo.getUpdatePwdTime(), new Date());
        if (time >= sysPwdRuleVo.getCycle()) {
            userPo.setStatus(0);
            super.updateById(userPo);
            throw new BaseException("???????????????????????????????????????????????????(=????????=)");
        }
    }

    /**
     * ????????????????????????
     *
     * @param userPo
     * @param sysPwdRuleVo
     */
    private void validatePwdLockTime(SysUserPo userPo, SysPwdRuleVo sysPwdRuleVo) {
        if (!ObjectUtils.isEmpty(userPo.getStatus())) {
            if (userPo.getStatus() == 0) {
                int time = DateUtil.getTimeChaByTwoTime(userPo.getErrotTime(), new Date());
                if (time >= sysPwdRuleVo.getLockTime()) {
                    userPo.setStatus(1);
                } else {
                    throw new BaseException("????????????????????????(=????????=)");
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param sysUpdatePassWordForm
     * @param userName
     */
    @Override
    public void updatePassWord(SysUpdatePassWordForm sysUpdatePassWordForm, String userName) {
        if (!ObjectUtils.isEmpty(sysUpdatePassWordForm)) {
            SysUserPo userPo = this.getUserPoByName(userName);

            //????????????????????????
            SysPwdRuleVo sysPwdRuleVo = sysPwdRuleService.queryPwdRuleAll();

            String newPwd = sysUpdatePassWordForm.getNewPassWord();

            //??????????????????
            this.validatePwdLength(newPwd, sysPwdRuleVo.getLength());

            //???????????????
            String newPassword = HMACUtil.getHMAC(newPwd, HMACEnum.HMAC_KEY.getType());

            String oldPwd = HMACUtil.getHMAC(sysUpdatePassWordForm.getOldPassWord(), HMACEnum.HMAC_KEY.getType());
            if (newPassword.equals(oldPwd)) {
                throw new BaseException("??????????????????????????????");
            }
            //???????????????????????????
            if (!userPo.getPassword().equals(oldPwd)) {
                throw new BaseException("??????????????????");
            }

            //????????????
            SysUserPo sysUserpo = mapper.selectByPrimaryKey(userPo.getId());

            int time = DateUtil.getDaysChaByTwoTime(userPo.getUpdatePwdTime(), new Date());
            if (time >= sysPwdRuleVo.getCycle()) {
                sysUserpo.setStatus(1);
            }
            sysUserpo.setPassword(newPassword);
            sysUserpo.setUpdatePwdTime(new Date());
            sysUserpo.setLastloginTime(new Date());
            super.updateById(sysUserpo);
        }
    }

    @Override
    public void updUserPo(SysUserPo userPo) {
        super.updateById(userPo);
    }

    /**
     * ????????????????????????
     *  @param userPo
     * @param errorNumber
     * @param lockTime
     */
    private int validatePwdUpdateCount(SysUserPo userPo, Integer errorNumber, Integer lockTime) {
        //??????????????????????????????
//        if (userPo.getErrorNumber() != null) {
//            if (userPo.getErrorNumber() + 1 >= errorNumber) {
//                //?????????????????????????????????
//                userPo.setStatus(0);
//                //??????????????????
//                userPo.setErrorNumber(0);
//                //????????????????????????
//                userPo.setErrotTime(new Date());
//                super.updateById(userPo);
//               // throw new BaseException("???????????????????????????" + errorNumber + "???,??????????????????!??( ?? ??? ??|||)?????????????????????"+lockTime+"??????");
//            }
//        }
        userPo.setErrotTime(new Date());
        Integer number = userPo.getErrorNumber() == null ? 0 : userPo.getErrorNumber();
        userPo.setErrorNumber(number + 1);
        super.updateById(userPo);
        return number + 1;
    }

    /**
     * ??????????????????
     *
     * @param newPassWord
     * @param length
     */
    private void validatePwdLength(String newPassWord, Integer length) {
        if (newPassWord.length() < length) {
            throw new BaseException("???????????????????????????! ???????????????"+length+"?????? (?????????)~*");
        }
    }

    /**
     * ????????????
     *
     * @param userIds
     */
    @Override
    public void resetPassword(List<Integer> userIds) {
        if (!ObjectUtils.isEmpty(userIds)) {
            String pwd = HMACUtil.getHMAC("123456", HMACEnum.HMAC_KEY.getType());
            mapper.updatePasswordByUserIds(userIds, new Date(), pwd);
        }
    }

    /**
     * ????????????
     *
     * @param userIds
     */
    @Override
    public void lockUser(List<Integer> userIds) {
        if (!ObjectUtils.isEmpty(userIds)) {
            List<Integer> ids = new ArrayList<>();
            for (Integer userId : userIds) {
                SysUserPo sysUserPo = mapper.selectByPrimaryKey(userId);
                if (sysUserPo.getStatus().equals(1)) {
                    ids.add(sysUserPo.getId());
                }
            }
            mapper.updateLockUser(ids, new Date());
        }
    }

    /**
     * ????????????
     *
     * @param userIds
     */
    @Override
    public void unlockUser(List<Integer> userIds) {
        if (!ObjectUtils.isEmpty(userIds)) {
            mapper.updateUnLockUser(userIds, new Date());
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param orgId
     * @return
     */
    @Override
    public List<SelectVo> queryUserSelectVosByOrgId(Integer orgId) {
        List<SelectVo> list = mapper.selectUserSelectByOrgId(orgId);
        return list;
    }

    /**
     * ????????????id????????????????????????????????????
     *
     * @param userId
     * @return
     */
    @Override
    public SysUserInfoVo getUserProjectInfo(Integer userId) {
        SysUserInfoVo sysUserInfoVo = this.getUserInfo(userId);
        if (!ObjectUtils.isEmpty(sysUserInfoVo)) {
            List<SysUserProjectInfoVo> getUserProjectInfoVos = mapper.getUserProjectInfoVos(userId);
            sysUserInfoVo.setProjectInfoVos(getUserProjectInfoVos);
        }
        return sysUserInfoVo;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @Override
    public List<SysAllUserVo> queryAllUserList() {
        List<SysAllUserVo> sysUserVoList = mapper.selectAllUsers();
        return sysUserVoList;
    }

    @Override
    public List<SysUserVo> queryAllUser() {
        return mapper.queryAllUser();
    }


    /**
     * ???????????????
     *
     * @param userIds
     * @return
     */
    @Override
    public String queryUserNamesByIds(List<Integer> userIds) {
        if (!ObjectUtils.isEmpty(userIds)) {
            List<SysUserVo> list = this.queryUsersByIds(userIds);
            StringBuffer names = new StringBuffer();
            for (SysUserVo sysUserVo : list) {
                names.append(sysUserVo.getActuName() + "???");
            }
            String retName = names.toString().substring(0, names.toString().length() - 1);
            return retName;
        }
        return "";
    }

    @Override
    public PageInfo<UserLevelVo> queryUserLevelList(UserLevelSearchForm searchForm, Integer pageSize, Integer
            currentPageNum) {
        //1????????????????????????UserLevelVo ??????????????????
        List<UserLevelVo> list = mapper.selectUserLevelList(searchForm);
        //2?????????????????????
        List<SysOrgPo> orgPos = orgService.queryOrgsByBizIdAndBizType(null, null);
        // ???????????????????????????????????????
        Map<Integer, SysOrgPo> sysPoMap = ListUtil.listToMap(orgPos);
        List<UserLevelVo> reList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(list)) {
            for (UserLevelVo userLevelVo : list) {
                // ???????????????????????????
                SysOrgPo org = this.getOrgName(sysPoMap, userLevelVo.getDepartment().getId());
                if (org != null) {
                    userLevelVo.setOrg(new GeneralVo(org.getId(), org.getOrgName()));
                }
                // ????????????????????????????????????????????????
                if (!ObjectUtils.isEmpty(searchForm.getOrgId())) {
                    //????????????????????????????????????ID???????????????????????????ID??????
                    if (org != null && org.getId().equals(searchForm.getOrgId())) {
                        reList.add(userLevelVo);
                    }
                } else {
                    // ?????????????????????????????????????????????
                    reList.add(userLevelVo);
                }
            }
        }

        PageInfo<UserLevelVo> pageInfo = PageUtil.getPageInfo(reList,pageSize,currentPageNum);

        return pageInfo;
    }

    private SysOrgPo getOrgName(Map<Integer, SysOrgPo> sysPoMap, Integer id) {
        SysOrgPo orgPo = sysPoMap.get(id);
        if (orgPo != null && orgPo.getOrgType() != null && orgPo.getOrgType().equals(0)) {
            return orgPo;
        } else if (orgPo != null) {
            return this.getOrgName(sysPoMap, orgPo.getParentId());
        }
        return orgPo;
    }

    /**
     * ??????????????????
     *
     * @param updateForm
     * @return
     */
    @Override
    public SysUserPo updateUserLevel(UserLevelUpdateForm updateForm) {
        SysUserPo sysUserPo = this.selectById(updateForm.getId());
        if (!ObjectUtils.isEmpty(sysUserPo)) {
            this.dozerMapper.map(updateForm, sysUserPo);
            this.updateById(sysUserPo);
        }
        return sysUserPo;
    }

    @Override
    public List<UserVo> querySysUserPoByIds(List<Integer> ids){
        return this.mapper.selectUserVoByUserIds(ids);
    }

    @Override
    public void updateUserByUserIdAndUpOrDown(Integer id, String upOrDown) {
        if(!ObjectUtils.isEmpty(id)){
            SysUserPo userPo = this.selectById(id);

            Integer sortNum = userPo.getSort();

            if(!ObjectUtils.isEmpty(sortNum)){
                if("up".equals(upOrDown)){
                    SysUserPo upUserPo = this.selectUserPoBySortNum(sortNum-1);

                    upUserPo.setSort(sortNum);
                    this.updateById(upUserPo);

                    userPo.setSort(sortNum-1);
                    this.updateById(userPo);
                }else if("down".equals(upOrDown)){
                    SysUserPo downUserPo = this.selectUserPoBySortNum(sortNum+1);

                    downUserPo.setSort(sortNum);
                    this.updateById(downUserPo);

                    userPo.setSort(sortNum+1);
                    this.updateById(userPo);
                }
            }
        }
    }

    /**
     * ??????sort??????????????????
     * @param sortNum
     * @return
     */
    private SysUserPo selectUserPoBySortNum(Integer sortNum){
        if(!ObjectUtils.isEmpty(sortNum)){
            Example example = new Example(SysUserPo.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("sort",sortNum);
            return this.selectOneByExample(example);
        }
        return null;
    }

    @Override
    public List<UserOrgVo> queryUserOrgByProjectId(Integer projectId) {
        // ????????????????????????
        boolean useTeam = orgService.isUseTeamByProjectId(projectId);
        String bizType = null;
        Integer bizId = null;
        if(useTeam){
            bizId = projectId;
            bizType = "project";
        }
        return this.mapper.selectUserOrgsByBiz(bizType,bizId);

    }
}
