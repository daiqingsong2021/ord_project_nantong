package com.wisdom.base.common.constant;

public class CommonConstants {
    public final static String RESOURCE_TYPE_MENU = "menu";
    public final static String RESOURCE_TYPE_BTN = "button";
    // 许可文件模块授权
    public static String BUNDLESPERMISSION = null;
    // 用户token异常
    public static final Integer EX_USER_INVALID_CODE = 40101;
    public static final Integer EX_USER_PASS_INVALID_CODE = 40001;
    // 客户端token异常
    public static final Integer EX_CLIENT_INVALID_CODE = 40301;
    public static final Integer EX_CLIENT_FORBIDDEN_CODE = 40331;
    public static final Integer EX_OTHER_CODE = 500;
    public static final String CONTEXT_KEY_USER_ID = "currentUserId";
    public static final String CONTEXT_KEY_USERNAME = "currentUserName";
    public static final String CONTEXT_KEY_USER_NAME = "currentUser";
    public static final String CONTEXT_KEY_USER_TOKEN = "currentUserToken";
    public static final String JWT_KEY_USER_ID = "userId";
    public static final String JWT_KEY_NAME = "name";
    //运行时校验异常
    public static final Integer EX_RUNTIME_COMPARA_CODE = 40401;
}
