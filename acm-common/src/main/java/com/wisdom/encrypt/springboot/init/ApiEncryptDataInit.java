package com.wisdom.encrypt.springboot.init;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wisdom.encrypt.springboot.annotation.DecryptRequestBody;
import com.wisdom.encrypt.springboot.annotation.EncryptResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wisdom.encrypt.springboot.HttpMethodTypePrefixConstant;

public class ApiEncryptDataInit implements ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(ApiEncryptDataInit.class);

    /**
     * 需要对响应内容进行加密的接口URI<br>
     * 比如：/user/list<br>
     * 不支持@PathVariable格式的URI
     */
    public static List<String> responseEncryptUriList = new ArrayList<String>();

    /**
     * 需要对请求内容进行解密的接口URI<br>
     * 比如：/user/list<br>
     * 不支持@PathVariable格式的URI
     */
    public static List<String> requestDecyptUriList = new ArrayList<String>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        //Map<String, Object> beanMap = ctx.getBeansWithAnnotation(RestController.class);
        //initData(beanMap);
        Map<String, Object> beanMap = ctx.getBeansWithAnnotation(Controller.class);
        initData(beanMap);
    }

    private void initData(Map<String, Object> beanMap) {
        if (beanMap != null) {
            for (Object bean : beanMap.values()) {
                Class<?> clz = bean.getClass();
                Method[] methods = clz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(EncryptResponseBody.class)) {
                        // 注解中的URI优先级高
                        boolean isEnabled = method.getAnnotation(EncryptResponseBody.class).value();
                        if (isEnabled) {
                            String uri = getApiUri(clz, method);
                            logger.debug("EncryptResponseBody URI: {}", uri);
                            responseEncryptUriList.add(uri);
                        }
                    }
                    if (method.isAnnotationPresent(DecryptRequestBody.class)) {
                        boolean isEnabled = method.getAnnotation(DecryptRequestBody.class).value();
                        if (isEnabled) {
                            String uri = getApiUri(clz, method);
                            logger.debug("DecryptRequestBody URI: {}", uri);
                            requestDecyptUriList.add(uri);
                        }
                    }
                }
            }
        }
    }

    private String getApiUri(Class<?> clz, Method method) {
        String methodType = "";
        StringBuilder uri = new StringBuilder();

        if (clz.isAnnotationPresent(RequestMapping.class)) {
            uri.append(formatUri(clz.getAnnotation(RequestMapping.class).value()[0]));
        }

        if (method.isAnnotationPresent(GetMapping.class)) {

            methodType = HttpMethodTypePrefixConstant.GET;
            uri.append(formatUri(method.getAnnotation(GetMapping.class).value()[0]));

        } else if (method.isAnnotationPresent(PostMapping.class)) {

            methodType = HttpMethodTypePrefixConstant.POST;
            uri.append(formatUri(method.getAnnotation(PostMapping.class).value()[0]));

        } else if (method.isAnnotationPresent(RequestMapping.class)) {

            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            RequestMethod m = requestMapping.method()[0];
            methodType = m.name().toLowerCase();
            uri.append(formatUri(requestMapping.value()[0]));

        } else if (method.isAnnotationPresent(PutMapping.class)) {

            methodType = HttpMethodTypePrefixConstant.PUT;
            uri.append(formatUri(method.getAnnotation(PutMapping.class).value()[0]));

        } else if (method.isAnnotationPresent(DeleteMapping.class)) {

            methodType = HttpMethodTypePrefixConstant.DELETE;
            uri.append(formatUri(method.getAnnotation(DeleteMapping.class).value()[0]));

        }

        return methodType + uri.toString();
    }

    private String formatUri(String uri) {
        if (uri.startsWith("/")) {
            return uri;
        }
        return "/" + uri;
    }
}
