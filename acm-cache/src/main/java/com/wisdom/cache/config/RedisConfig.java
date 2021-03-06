package com.wisdom.cache.config;

import com.wisdom.cache.utils.PropertiesLoaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

@Configuration
public class RedisConfig {
    @Autowired
    private Environment env;
    private JedisPool pool;
    private String maxActive;
    private String maxIdle;
    private String maxWait;
    private String host;
    private String password;
    private String timeout;
    private String database;
    private String port;
    private String enable;
    private String sysName;

    @PostConstruct
    public void  init(){
        PropertiesLoaderUtils prop = new PropertiesLoaderUtils("application.yml");
        host = prop.getProperty("redis.host");
        if(StringUtils.isBlank(host)){
            host = env.getProperty("redis.host");
            maxActive = env.getProperty("redis.pool.maxActive");
            maxIdle  = env.getProperty("redis.pool.maxIdle");
            maxWait = env.getProperty("redis.pool.maxWait");
            password = env.getProperty("redis.password");
            timeout = env.getProperty("redis.timeout");
            database = env.getProperty("redis.database");
            port = env.getProperty("redis.port");
            sysName = env.getProperty("redis.sysName");
            enable = env.getProperty("redis.enable");
        } else{
            maxActive = prop.getProperty("redis.pool.maxActive");
            maxIdle  = prop.getProperty("redis.pool.maxIdle");
            maxWait = prop.getProperty("redis.pool.maxWait");
            password = prop.getProperty("redis.password");
            timeout = prop.getProperty("redis.timeout");
            database = prop.getProperty("redis.database");
            port = prop.getProperty("redis.port");
            sysName = prop.getProperty("redis.sysName");
            enable = prop.getProperty("redis.enable");
        }
    }

    @Bean
    public JedisPoolConfig constructJedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        // ????????????pool??????????????????jedis???????????????pool.getResource()????????????
        // ???????????????-1??????????????????????????????pool???????????????maxActive???jedis??????????????????pool????????????exhausted(??????)???
        config.setMaxTotal(Integer.parseInt(maxActive));
        // ????????????pool???????????????????????????idle(?????????)???jedis?????????
        config.setMaxIdle(Integer.parseInt(maxIdle));
        // ?????????borrow(??????)??????jedis??????????????????????????????????????????????????????????????????????????????JedisConnectionException???
        config.setMaxWaitMillis(Integer.parseInt(maxWait));
        config.setTestOnBorrow(true);

        return config;
    }

    @Bean(name = "pool")
    public JedisPool constructJedisPool() {
        String ip = this.host;
        int port = Integer.parseInt(this.port);
        String password = this.password;
        int timeout = Integer.parseInt(this.timeout);
        int database = Integer.parseInt(this.database);
        if (null == pool) {
            if (StringUtils.isBlank(password)) {
                pool = new JedisPool(constructJedisPoolConfig(), ip, port, timeout);
            } else {
                pool = new JedisPool(constructJedisPoolConfig(), ip, port, timeout, password, database);
            }
        }
        return pool;
    }

    public String getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(String maxActive) {
        this.maxActive = maxActive;
    }

    public String getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(String maxIdle) {
        this.maxIdle = maxIdle;
    }

    public String getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(String maxWait) {
        this.maxWait = maxWait;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSysName() {
        return sysName;
    }

    public void setSysName(String sysName) {
        this.sysName = sysName;
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
