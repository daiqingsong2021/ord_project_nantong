package com.wisdom.auth.server.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.wisdom.auth.server.entity.ApiResult;
import com.wisdom.base.common.msg.TableResultResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("sso")
@Slf4j
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*", methods = { RequestMethod.POST }, origins = "*")
public class SsoController {
	String baseUrl = "http://www.nsst.cc:32237";

	/**
	 * 
	 * 对外客户列表查询接口 ------
	 * 
	 * 
	 */
	@GetMapping(value = "/queryTrafficMainList/{pageSize}/{currentPageNum}")
	public ApiResult<String> queryTrafficMainList(@RequestParam Map<String, Object> mapWhere, @PathVariable("pageSize") Integer pageSize, @PathVariable("currentPageNum") Integer currentPageNum) {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
		factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(factory);
		// request body
		Map<String, String> foo = new HashMap<>();
		foo.put("userName", "weihuyuan");
		foo.put("password", "123456");
		foo.put("name", "维护员");
		foo.put("uid", "UVU8888");
		foo.put("phone_number", "");
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/api/auth/jwt/ssoToken", foo, String.class);
		// parse response
		Map mapTypes = JSON.parseObject(responseEntity.getBody());
		if ((Integer) mapTypes.get("status") == 200) {
			String data = (String) mapTypes.get("data");
			if (data != null && data != "") {
				HttpHeaders headers = new HttpHeaders();
				headers.add("Content-Type", "application/json;charset=UTF-8");
				headers.add("Authorization", (String) mapTypes.get("data"));
				HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
				String urlTemplate = "/api/dc1/traffic/queryTrafficMainList/" + pageSize + "/" + currentPageNum + "?startTime=" + mapWhere.get("startTime") + "&endTime=" + mapWhere.get("endTime");
				String url = String.format(urlTemplate, 1, 2);
				ResponseEntity<String> resEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
				Map resMap = JSON.parseObject(resEntity.getBody());
				return ApiResult.success1(resMap.get("data"), (Integer) resMap.get("total"));
			} else {
				return ApiResult.error("调用queryTrafficMainList接口失败");
			}
		} else {
			return ApiResult.error("调用token接口失败");
		}
	}
}
