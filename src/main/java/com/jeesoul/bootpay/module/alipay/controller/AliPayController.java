package com.jeesoul.bootpay.module.alipay.controller;

import com.jeesoul.bootpay.common.constants.Constants;
import com.jeesoul.bootpay.common.model.Product;
import com.jeesoul.bootpay.module.alipay.service.IAliPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 支付宝支付
 *
 * @author dxy
 * @date 2019/11/5 10:17
 */
@Controller
@RequestMapping(value = "alipay")
public class AliPayController {

    private static final Logger logger = LoggerFactory.getLogger(AliPayController.class);
    @Autowired
    private IAliPayService aliPayService;

    /**
     * 二维码支付
     *
     * @param product
     * @param map
     * @return
     */
    @RequestMapping(value = "qcPay", method = RequestMethod.POST)
    public String qcPay(Product product, ModelMap map) {
        logger.info("二维码支付");
        String message = aliPayService.aliPay(product);
        if (Constants.SUCCESS.equals(message)) {
            String img = "../qrcode/" + product.getOutTradeNo() + ".png";
            map.addAttribute("img", img);
        } else {
            //失败
        }
        return "alipay/qcpay";
    }

    /**
     * 电脑支付
     *
     * @param product
     * @param map
     * @return
     */
    @RequestMapping(value = "pcPay", method = RequestMethod.POST)
    public String pcPay(Product product, ModelMap map) {
        logger.info("电脑支付");
        String form = aliPayService.aliPayPc(product);
        map.addAttribute("form", form);
        return "alipay/pay";
    }

}
