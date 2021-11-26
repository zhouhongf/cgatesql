package com.myworld.cgate.auth.authenticate.service


import com.myworld.cgate.auth.authenticate.vo.ValidateCodeType
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 校验码处理器管理器
 */
@Component
class ValidateCodeProcessorHolder {
    private val log = LogManager.getRootLogger()
    // 自动装载了一个ValidateCodeProcessor接口的Map映射
    // AbstractValidateCodeProcessor<C extends ValidateCode>使用了该接口
    // 因为其中的C extends ValidateCode可以延申出很多子类，所以这里可以使用Map<String, ValidateCodeProcessor>来装配，含有多个类型
    @Autowired
    private lateinit var validateCodeProcessors: Map<String, ValidateCodeProcessor>

    fun findValidateCodeProcessor(type: ValidateCodeType): ValidateCodeProcessor? {
        return findValidateCodeProcessor(type.toString().toLowerCase())
    }

    fun findValidateCodeProcessor(type: String): ValidateCodeProcessor? {
        log.info("ValidateCodeProcessorHolder类findValidateCodeProcessor方法")
        val name = type.toLowerCase() + ValidateCodeProcessor::class.java.simpleName
        // 根据image或sms+ValidateCodeProcessor类的底层类名ValidateCodeProcessor，组合而成的名称name, 在Map<String, ValidateCodeProcessor>中查找是否存在相同的名称
        // 即分别是ImageCodeProcessor和SmsCodeProcessor在@Component()中标注的名称是否为imageValidateCodeProcessor和smsValidateCodeProcessor
        // ImageCodeProcessor和SmsCodeProcessor这两个类都是AbstractValidateCodeProcessor<C extends ValidateCode>的子类
        // ImageCodeProcessor继承了AbstractValidateCodeProcessor<ImageCode>
        // SmsCodeProcessor继承了AbstractValidateCodeProcessor<ValidateCode>
        // ImageCode类是ValidateCode类的子类，比ValidateCode类多了一个BufferedImage属性成员
        return validateCodeProcessors[name]
        // 返回相关的ValidateCodeProcessor（imageValidateCodeProcessor或smsValidateCodeProcessor）
        // 然后再使用相关ValidateCodeProcessor的方法继续下一步
    }

}
