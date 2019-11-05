package com.jeesoul.bootpay.common.config;

import com.alipay.demo.trade.config.Configs;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动加载支付宝、微信以及银联相关参数配置
 *
 * @author dxy
 * @date 2019/11/5 9:48
 */
@Component
public class InitPay implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //初始化 支付宝相关参数,涉及机密,此文件不会提交,请自行配置相关参数并加载

        Configs.init("zfbinfo.properties");//支付宝


    }
}
