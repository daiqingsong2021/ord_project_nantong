package com.wisdom.acm.base.controller;

import com.wisdom.acm.base.form.tmpldelv.BaseTmpldelvAddForm;
import com.wisdom.acm.base.form.tmpldelv.BaseTmpldelvUpdateForm;
import com.wisdom.acm.base.po.BaseTmpldelvPo;
import com.wisdom.acm.base.service.BaseTmplDelvService;
import com.wisdom.acm.base.vo.tmpldelv.BaseTmpldelvTreeVo;
import com.wisdom.acm.base.vo.tmpldelv.BaseTmpldelvVo;
import com.wisdom.base.common.aspect.AddLog;
import com.wisdom.base.common.controller.BaseController;
import com.wisdom.base.common.enums.LoggerModuleEnum;
import com.wisdom.base.common.log.AcmLogger;
import com.wisdom.base.common.msg.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/tmpldelv")
public class BaseTmplDelvController extends BaseController {
    @Autowired
    BaseTmplDelvService baseTmplDelvService;

    @GetMapping("/tree")
    public ApiResult querryPageBaseTmplList() {
        List<BaseTmpldelvTreeVo> pageBaseTmplList = baseTmplDelvService.querryTmpldelvList();
        return ApiResult.success(pageBaseTmplList);
    }

    @GetMapping("/{tmpldelvId}/tree")
    public ApiResult querryPageBaseTmplListByTmplId(@PathVariable("tmpldelvId") Integer tmpldelvId) {
        List<BaseTmpldelvTreeVo> pageBaseTmplList = baseTmplDelvService.querryTmpldelvListSubByTmplId(tmpldelvId);
        return ApiResult.success(pageBaseTmplList);
    }

    @GetMapping("/{tmpldelvId}/{taskId}/assign/tree")
    public ApiResult querryPageBaseTmplListAssignTree(@PathVariable("tmpldelvId") Integer tmpldelvId,@PathVariable("tmpldelvId") Integer taskId) {
        List<BaseTmpldelvTreeVo> pageBaseTmplList = baseTmplDelvService.querryPageBaseTmplListAssignTree(tmpldelvId,taskId);
        return ApiResult.success(pageBaseTmplList);
    }

    @GetMapping("/{id}/pbs/info")
    public ApiResult getTmpldelvPbsById(@PathVariable("id") Integer tmpldelvId) {
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(tmpldelvId);
        return ApiResult.success(baseTmpldelvVo);
    }

    @GetMapping("/{id}/delv/info")
    public ApiResult getTmpldelvDelvById(@PathVariable("id") Integer tmpldelvId) {
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(tmpldelvId);
        return ApiResult.success(baseTmpldelvVo);
    }

    @AddLog(title = "??????PBS",module = LoggerModuleEnum.BM_TMPL_DELV,initContent = true)
    @PostMapping("/pbs/add")
    public ApiResult addPbsTmpldelv(@RequestBody @Valid BaseTmpldelvAddForm baseTmpldelvAddForm){
        this.setAcmLogger(new AcmLogger("????????????????????????PBS,?????????:"+baseTmpldelvAddForm.getDelvTitle()));
        BaseTmpldelvPo baseTmpldelvPo = baseTmplDelvService.addPbsTmpldelv(baseTmpldelvAddForm);
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(baseTmpldelvPo.getId());
        return ApiResult.success(baseTmpldelvVo);
    }

    @AddLog(title = "???????????????",module = LoggerModuleEnum.BM_TMPL_DELV,initContent = true)
    @PostMapping("/delv/add")
    public ApiResult addSubTmpldelv(@RequestBody @Valid BaseTmpldelvAddForm baseTmpldelvAddForm){
        this.setAcmLogger(new AcmLogger("??????PBS???????????????,?????????:"+baseTmpldelvAddForm.getDelvTitle()));
        BaseTmpldelvPo baseTmpldelvPo = baseTmplDelvService.addSubTmpldelv(baseTmpldelvAddForm);
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(baseTmpldelvPo.getId());
        return ApiResult.success(baseTmpldelvVo);
    }

    @PutMapping("/pbs/update")
    public ApiResult updateTmplpbs(@RequestBody @Valid BaseTmpldelvUpdateForm baseTmpldelvUpdateForm) {
        BaseTmpldelvPo baseTmpldelvPo = baseTmplDelvService.updateTmpldelv(baseTmpldelvUpdateForm);
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(baseTmpldelvPo.getId());
        return ApiResult.success(baseTmpldelvVo);
    }

    @PutMapping("/delv/update")
    public ApiResult updateTmpldelv(@RequestBody @Valid BaseTmpldelvUpdateForm baseTmpldelvUpdateForm) {
        BaseTmpldelvPo baseTmpldelvPo = baseTmplDelvService.updateTmpldelv(baseTmpldelvUpdateForm);
        BaseTmpldelvVo baseTmpldelvVo = baseTmplDelvService.getTmpldelvById(baseTmpldelvPo.getId());
        return ApiResult.success(baseTmpldelvVo);
    }

    @DeleteMapping("/pbs/delete")
    @AddLog(title = "??????PBS",module = LoggerModuleEnum.BM_TMPL_DELV)
    public ApiResult deleteTmplpbs(@RequestBody List<Integer> ids){
        String delvTitles = baseTmplDelvService.queryNamesByIds(ids,"delvTitle");
        this.setAcmLogger(new AcmLogger("??????????????????????????????PBS??????????????????"+delvTitles));
        baseTmplDelvService.deleteTmpldelv(ids);
        return ApiResult.success();
    }

    @DeleteMapping("/delv/delete")
    @AddLog(title = "???????????????",module = LoggerModuleEnum.BM_TMPL_DELV)
    public ApiResult deleteTmpldelv(@RequestBody List<Integer> ids){
        String delvTitles = baseTmplDelvService.queryNamesByIds(ids,"delvTitle");
        this.setAcmLogger(new AcmLogger("??????????????????????????????PBS?????????????????????????????????"+delvTitles));
        baseTmplDelvService.deleteTmpldelv(ids);
        return ApiResult.success();
    }

    /**
     * (??????)?????????????????????
     * @param ids
     * @return
     */
    @PostMapping("/import/list")
    public ApiResult queryTmpldelvPosByIds(@RequestBody List<Integer> ids){
        List<BaseTmpldelvPo> tmpldelvPos = baseTmplDelvService.queryTmpldelvPosByIds(ids);
        return ApiResult.success(tmpldelvPos);
    }

}
