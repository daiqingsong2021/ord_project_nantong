package com.wisdom.acm.wf.service;

import com.wisdom.acm.wf.form.TodoBusinessForm;
import com.wisdom.acm.wf.form.TodoMsgForm;
import com.wisdom.base.common.form.WfStartProcessForm;
import com.wisdom.base.common.vo.wf.WfRunProcessVo;

import java.util.List;

public interface ActSsoService {

    public int insertTodoMsg(WfStartProcessForm form, WfRunProcessVo procVo);
    public int insertTodoBusiness(WfStartProcessForm form, WfRunProcessVo procVo);
}
