package com.jeesoul.bootpay.module.alipay.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayResponse;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.jeesoul.bootpay.common.constants.Constants;
import com.jeesoul.bootpay.common.model.Product;
import com.jeesoul.bootpay.common.utils.CommonUtil;
import com.jeesoul.bootpay.module.alipay.config.AliPayConfig;
import com.jeesoul.bootpay.module.alipay.service.IAliPayService;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付宝
 *
 * @author dxy
 * @date 2019/11/5 9:59
 * ======================
 * 商户端私钥：
 * 由我们自己生成的RSA私钥（必须与商户端公钥是一对），生成后要保存在服务端，绝对不能保存在客户端，也绝对不能从服务端下发。
 * 用来对订单信息进行加签，加签过程一定要在服务端完成，绝对不能在客户端做加签工作，客户端只负责用加签后的订单信息调起支付宝来支付。
 * ======================
 * 商户端公钥：
 * 由我们自己生成的RSA公钥（必须与商户端私钥是一对），生成后需要填写在支付宝开放平台，
 * 用来给支付宝服务端验签经过我们加签后的订单信息，以确保订单信息确实是我们商户端发给支付宝的，并且确保订单信息在传输过程中未被篡改。
 * ======================
 * 支付宝私钥：
 * 支付宝自己生成的，他们自己保存，开发者是无法看到的，用来对支付结果进行加签。
 * ======================
 * 支付宝公钥：
 * 支付宝公钥和支付宝私钥是一对，也是支付宝生成的，当我们把商户端公钥填写在支付宝开放平台后，平台就会给我们生成一个支付宝公钥。
 * 我们可以复制下来保存在服务端，同样不要保存在客户端，并且不要下发，避免被反编译或截获，而被篡改支付结果。
 * 用来让服务端对支付宝服务端返给我们的同步或异步支付结果进行验签，以确保支付结果确实是由支付宝服务端返给我们服务端的，而且没有被篡改。
 * 对支付结果的验签工作也一定要在服务端完成，绝对不能在客户端验签，因为支付宝公钥一旦存储在客户端用来验签，那就可能被反编译，这样就谁都可以验签支付结果并篡改了。
 * ======================
 * 支付宝建议加签方式升级为RSA(SHA256)密钥，以为 SHA 貌似已经被破解了。
 */
@Service
public class AliPayServiceImpl implements IAliPayService {
    private static final Logger logger = LoggerFactory.getLogger(AliPayServiceImpl.class);

    @Value("${alipay.notify.url}")
    private String notify_url;

    @Override
    public String aliPay(Product product) {
        logger.info("订单号：{}生成支付宝支付码",product.getOutTradeNo());
        String  message = Constants.SUCCESS;
        //二维码存放路径
        System.out.println(Constants.QRCODE_PATH);
        String imgPath= Constants.QRCODE_PATH+Constants.SF_FILE_SEPARATOR+product.getOutTradeNo()+".png";
        String outTradeNo = product.getOutTradeNo();
        String subject = product.getSubject();
        String totalAmount =  CommonUtil.divide(product.getTotalFee(), "100").toString();
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";
        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";
        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");
        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = product.getBody();
        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";
        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject)
                .setTotalAmount(totalAmount)
                .setOutTradeNo(outTradeNo)
                .setSellerId(sellerId)
                .setBody(body)//128长度 --附加信息
                .setStoreId(storeId)
                .setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(notify_url);//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置

        AlipayF2FPrecreateResult result = AliPayConfig.getAlipayTradeService().tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, imgPath);
                break;

            case FAILED:
                logger.info("支付宝预下单失败!!!");
                message = Constants.FAIL;
                break;

            case UNKNOWN:
                logger.info("系统异常，预下单状态未知!!!");
                message = Constants.FAIL;
                break;

            default:
                logger.info("不支持的交易状态，交易返回异常!!!");
                message = Constants.FAIL;
                break;
        }
        return message;
    }
    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(), response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    @Override
    public String aliRefund(Product product) {
        return null;
    }

    @Override
    public String aliCloseorder(Product product) {
        return null;
    }

    @Override
    public String downloadBillUrl(String billDate, String billType) {
        return null;
    }

    @Override
    public String aliPayMobile(Product product) {
        return null;
    }

    @Override
    public String aliPayPc(Product product) {
        logger.info("支付宝PC支付下单");
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        String returnUrl = "前台回调地址 http 自定义";
        alipayRequest.setReturnUrl(returnUrl);//前台通知
        alipayRequest.setNotifyUrl(notify_url);//后台回调
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", product.getOutTradeNo());
        bizContent.put("total_amount", product.getTotalFee());//订单金额:元
        bizContent.put("subject",product.getSubject());//订单标题
        bizContent.put("seller_id", Configs.getPid());//实际收款账号，一般填写商户PID即可
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");//电脑网站支付
        bizContent.put("body", "两个苹果五毛钱");
        /**
         * 这里有三种模式可供选择
         * 如果在系统内支付，并且是弹出层支付，建议选择模式二、其他模式会跳出当前iframe(亲测有效)
         */
        bizContent.put("qr_pay_mode", "2");
        String biz = bizContent.toString().replaceAll("\"", "'");
        alipayRequest.setBizContent(biz);
        logger.info("业务参数:"+alipayRequest.getBizContent());
        String form = Constants.FAIL;
        try {
            form = AliPayConfig.getAlipayClient().pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            logger.error("支付宝构造表单失败",e);
        }
        return form;
    }

    @Override
    public String appPay(Product product) {
        return null;
    }

    @Override
    public boolean rsaCheckV1(Map<String, String> params) {
        return false;
    }

    @Override
    public boolean rsaCheckV2(Map<String, String> params) {
        return false;
    }
}
