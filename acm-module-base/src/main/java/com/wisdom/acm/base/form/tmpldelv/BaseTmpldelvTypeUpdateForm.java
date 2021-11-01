package com.wisdom.acm.base.form.tmpldelv;

import com.wisdom.base.common.aspect.LogParam;
import com.wisdom.base.common.form.BaseForm;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BaseTmpldelvTypeUpdateForm extends BaseForm {

    @NotNull(message = "交付物模板主键不能为空")
    private Integer id;

    @NotNull(message = "标题不能为空")
    @LogParam(title = "标题")
    private String typeTitle;

    @NotNull(message = "编号不能为空")
    @LogParam(title = "编号")
    private String typeNum;

    @NotNull(message = "版本不能为空")
    @LogParam(title = "版本号")
    private String typeVersion;

    @LogParam(title = "类型")
    private String typeType;

    @LogParam(title = "备注")
    private String typeDesc;

}