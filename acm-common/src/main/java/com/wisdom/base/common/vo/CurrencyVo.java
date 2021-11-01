package com.wisdom.base.common.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CurrencyVo {

    /**
     * id
     */
    private Integer id;

    /**
     * 货币名称
     */
    private String name;

    /**
     * 货币代码
     */
    private String code;

    /**
     * 货币符号
     */
    private String symbol;

}