package com.myworld.cgate.service

import com.myworld.cgate.auth.authenticate.config.UserContextHolder
import com.myworld.cgate.auth.authenticate.service.handler.MyLoginHandler
import com.myworld.cgate.auth.authenticate.service.image.ImageCodeProcessor
import com.myworld.cgate.auth.authenticate.service.sms.SmsCodeProcessor
import com.myworld.cgate.auth.data.entity.SysUser
import com.myworld.cgate.auth.data.entity.UserAvatar
import com.myworld.cgate.auth.data.repository.PcdsRepository
import com.myworld.cgate.auth.data.repository.SysUserRepository
import com.myworld.cgate.auth.data.repository.UserAvatarRepository
import com.myworld.cgate.auth.data.repository.UserInfoRepository
import com.myworld.cgate.auth.service.JwtUtil
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.*
import com.myworld.cgate.util.IPUtil
import com.myworld.cgate.util.StringUtil
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*



@Service
class SysUserServiceImpl : SysUserService {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var smsCodeProcessor: SmsCodeProcessor
    @Autowired
    private lateinit var imageCodeProcessor: ImageCodeProcessor
    @Autowired
    private lateinit var sysRoleService: SysRoleService
    @Autowired
    private lateinit var myLoginHandler: MyLoginHandler
    @Autowired
    private lateinit var sysUserRepository: SysUserRepository
    @Autowired
    private lateinit var userAvatarRepository: UserAvatarRepository
    @Autowired
    private lateinit var pcdsRepository: PcdsRepository
    @Autowired
    private lateinit var userInfoRepository: UserInfoRepository
    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var template: RedisTemplate<String, Any>

    override fun checkLogin(ctx: ServerWebExchange): ApiResult<*> {
        val request = ctx.request
        val token = request.headers.getFirst(JwtUtil.HEADER_AUTH)
        if (token.isNullOrEmpty()) {
            return ResultUtil.failure()
        }
        return JwtUtil.checkToken(token, ServiceName.DEFAULT.name, template, userInfoRepository)
    }


    override fun doesUserExists(username: String): ApiResult<*> {
        val theUsername = MyUserKeyService.getRealUsername(username)
        val sysUser = sysUserRepository.findByUsername(theUsername)
        return if (sysUser != null) {
            ResultUtil.success(msg = "用户存在")
        } else {
            ResultUtil.failure(-2, "用户不存在")
        }
    }

    override fun getUsername(wid: String?): String? {
        var widNeed = wid
        if (widNeed.isNullOrEmpty()) {
            val currentUser = UserContextHolder.getUserContext()
            widNeed = currentUser!!.wid
        }
        val optional = sysUserRepository.findById(widNeed)
        return if (optional.isPresent) {
            optional.get().username
        } else {
            null
        }
    }


    override fun register(username: String, smsCode: String, password: String, ctx: ServerWebExchange): ApiResult<*> {
        val (code) = smsCodeProcessor.validate(ctx)
        if (code != 0) {
            return ResultUtil.failure(-2, "验证码验证失败")
        }
        val realUsername = MyUserKeyService.getRealUsername(username)
        val user = sysUserRepository.findByUsername(realUsername)
        if (user != null) {
            return ResultUtil.failure(-2, "用户已存在，请勿重复注册")
        }
        val realPassword = MyUserKeyService.getRealPassword(password)
        return createSysUser(realUsername, realPassword, realUsername)
    }

    override fun createSysUser(realUsername: String, realPassword: String, creator: String): ApiResult<*> {
        sysRoleService.updateRole(PlayerType.USERS_CLIENT.name)
        val theRandom = ((Math.random() * 9 + 1) * 1000).toLong()
        val wid = SecurityConstants.USER_ID_PREFIX + Date().time + theRandom.toString()
        val passwordEncode = MyUserKeyService.aesEncrypt(realPassword)
        val sysUser = SysUser(creator = creator, updater = creator, wid = wid, username = realUsername, password = passwordEncode, sysroles = PlayerType.USERS_CLIENT.name)
        sysUserRepository.save(sysUser)
        return ResultUtil.success()
    }

    // 此处的password是去除混淆字母后，再重新加密的密码
    override fun setSysUser(username: String, passwordEncode: String, wid: String, sysroles: MutableList<String>, updater: String): ApiResult<*> {
        val sysUserOptional = sysUserRepository.findById(wid)
        if (!sysUserOptional.isPresent) {
            return ResultUtil.failure(msg = "用户IP不匹配");
        }
        val sysrolesStr = sysroles.joinToString(",")
        val sysuser = sysUserOptional.get()
        sysuser.username = username
        sysuser.password = passwordEncode
        sysuser.sysroles = sysrolesStr
        sysuser.updater = updater
        sysuser.updateTime = Date().time
        sysUserRepository.save(sysuser)
        return ResultUtil.success()
    }

    override fun login(username: String, password: String, imageCode: String, ctx: ServerWebExchange): ApiResult<*> {
        val (code) = imageCodeProcessor.validate(ctx)
        if (code != 0) {
            return ResultUtil.failure(-2, "验证码验证失败")
        }
        val usernameReal = MyUserKeyService.getRealUsername(username)
        val passwordReal = MyUserKeyService.getRealPassword(password)
        val sysUser = sysUserRepository.findByUsername(usernameReal) ?: return ResultUtil.failure(-2, "用户不存在")
        val passwordSave = MyUserKeyService.aesDecrypt(sysUser.password!!)
        if (passwordReal != passwordSave) {
            return ResultUtil.failure(-2, "密码不正确")
        }
        return myLoginHandler.onAuthenticationSuccess(sysUser, ctx)
    }

    override fun smsLogin(mobile: String, smsCode: String, ctx: ServerWebExchange): ApiResult<*> {
        val (code) = smsCodeProcessor.validate(ctx)
        if (code != 0) {
            return ResultUtil.failure(-2, "验证码验证失败")
        }
        val usernameReal = MyUserKeyService.getRealUsername(mobile)
        val sysUser = sysUserRepository.findByUsername(usernameReal) ?: return ResultUtil.failure(-2, "用户不存在")
        return myLoginHandler.onAuthenticationSuccess(sysUser, ctx)
    }

    override fun changePassword(passwordold: String, password: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val optional = sysUserRepository.findById(userDetail.wid)
        if (!optional.isPresent) {
            return ResultUtil.failure(-2, "用户令牌不正确，无此用户")
        }
        val sysUser = optional.get()

        val passwordoldReal = MyUserKeyService.getRealPassword(passwordold)
        val passwordSave = MyUserKeyService.aesDecrypt(sysUser.password!!)
        if (passwordoldReal != passwordSave) {
            return ResultUtil.failure(-2, "原密码不正确")
        }
        val passwordReal = MyUserKeyService.getRealPassword(password)
        val passwordEncode = MyUserKeyService.aesEncrypt(passwordReal)
        sysUser.passwordold = sysUser.password
        sysUser.password = passwordEncode
        sysUser.updater = sysUser.username
        sysUser.updateTime = Date().time
        sysUserRepository.save(sysUser)
        return ResultUtil.success()
    }

    override fun resetPassword(mobile: String, smsCode: String, password: String, ctx: ServerWebExchange): ApiResult<*> {
        val (code) = smsCodeProcessor.validate(ctx)
        if (code != 0) {
            return ResultUtil.failure(-2, "验证码验证失败")
        }

        val userDetail = UserContextHolder.userContext.get()
        val optional = sysUserRepository.findById(userDetail.wid)
        if (!optional.isPresent) {
            return ResultUtil.failure(-2, "用户令牌不正确，无此用户")
        }
        val sysUser = optional.get()

        val realUsername = MyUserKeyService.getRealUsername(mobile)
        val usernameSave = sysUser.username
        if (usernameSave != realUsername) {
            return ResultUtil.failure(-2, "用户名不正确")
        }

        val realPassword = MyUserKeyService.getRealPassword(password)
        val passwordEncode = MyUserKeyService.aesEncrypt(realPassword)

        // 上面检验通过后，更新Sysuser
        sysUser.passwordold = sysUser.password
        sysUser.password = passwordEncode
        sysUser.updater = usernameSave
        sysUser.updateTime = Date().time
        sysUserRepository.save(sysUser)
        return ResultUtil.success()
    }

    override fun uploadUserAvatarBase64(base64: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val wid = userDetail.wid
        val optional = userAvatarRepository.findById(wid)
        val userAvatar = if (optional.isPresent) {
            optional.get()
        } else {
            UserAvatar(wid = wid, fileName = "$wid.jpg", extensionType = "image/jpeg")
        }
        val base64Bytes = StringUtil.base64ToBytes(base64)
        userAvatar.fileByte = base64Bytes
        userAvatarRepository.save(userAvatar)
        return ResultUtil.success()
    }

    override fun avatar(id: String, ctx: ServerWebExchange): Mono<Void> {
        val response = ctx.response
        val optional = userAvatarRepository.findById(id)
        if (optional.isPresent) {
            val userAvatar = optional.get()
            val bodyDataBuffer = response.bufferFactory().wrap(userAvatar.fileByte!!)
            return response.writeWith(Mono.just(bodyDataBuffer))
        }
        return response.setComplete()
    }

    override fun getAvatar(): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val wid = simpleUser.wid
        val optional = userAvatarRepository.findById(wid)
        if (optional.isPresent) {
            val userAvatar = optional.get()
            val base64 = StringUtil.bytesToBase64(userAvatar.fileByte!!)
            return ResultUtil.success(data = base64)
        }
        return ResultUtil.failure(msg = "此用户无头像")
    }

    override fun updateVisitor(ctx: ServerWebExchange): ApiResult<*> {
        val request = ctx.request
        val ip = request.remoteAddress!!.address.hostAddress
        val region = IPUtil.getCityInfo(ip)
        log.info("region是：{}", region)

        var provinceName = ""
        var cityName = ""
        if (region != null) {
            val theNames = region.split("|")
            provinceName = theNames[2]
            cityName = theNames[3]
        }
        // 以下用于返回给前端 城市名称 和 经纬度
        if (cityName == "内网IP" || cityName == "") {
            cityName = "苏州市"
        }
        if (provinceName == "内网IP" || provinceName == "") {
            provinceName = "江苏省"
        }

        val lngLat = getCityLngLatByProvinceAndCity(provinceName, cityName)
        val city: String = StringUtil.filterPlaceName(cityName)

        // val res = WeatherUtil.getWeatherByCityName(city)
        // var temp = ""
        // var weather = ""
        // if (res != null) {
        //     val strContent = res.substring(12)
        //     val jsonObject = JSONObject.parseObject(strContent)
        //     temp = jsonObject.getString("temp")
        //     weather = jsonObject.getString("weather")
        // }

        val map: MutableMap<String, String> = HashMap()
        map["city"] = city
        map["lngLat"] = lngLat!!
        // map["temp"] = temp
        // map["weather"] = weather
        return ResultUtil.success(data = map)
    }

    override fun getCityLngLatByProvinceAndCity(provinceName: String?, cityName: String?): String? {
        if (cityName == null) {
            return null
        }
        // 先单独通过cityName去找
        val cities = pcdsRepository.findByNameAndLevel(cityName, "city")
        if (cities.size == 1) {
            return cities[0].center
        }
        // 找不到，再通过provinceName-cityName去找
        if (provinceName != null) {
            val fullname = "$provinceName-$cityName"
            val city = pcdsRepository.findByFullname(fullname) ?: return null
            return city.center
        }
        return null
    }



    /**
     * 以下为admin管理部分
     */
    override fun adminGetUsers(playerType: String, pageSize: Int, pageIndex: Int): ApiResult<*> {
        val currentUser = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        if (PlayerType.ADMIN_SUPER.name in currentUser.roles) {
            val pageable: Pageable = PageRequest.of(pageIndex, pageSize, Sort.Direction.DESC, "updateTime")
            val sysUsers = sysUserRepository.findBySysrolesContaining(playerType = playerType, pageable = pageable)
            if (sysUsers.isEmpty) {
                return ResultUtil.failure(msg = "没有数据")
            }
            return ResultUtil.success(num = sysUsers.totalElements, data = sysUsers.content)
        }
        return ResultUtil.failure(msg = "非管理员权限")
    }


    override fun adminSetUser(username: String, password: String, wid: String?, sysroles: MutableList<String>): ApiResult<*> {
        val currentUser = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val optionalAdmin = sysUserRepository.findById(currentUser.wid)
        if (!optionalAdmin.isPresent) {
            return ResultUtil.failure(-2, "无此管理员，不能创建用户");
        }

        if (PlayerType.ADMIN_SUPER.name in currentUser.roles) {
            val realUsername = MyUserKeyService.getRealUsername(username)
            val creator = optionalAdmin.get().username!!

            val realPassword = MyUserKeyService.getRealPassword(password)
            if (wid.isNullOrEmpty()) {
                val user = sysUserRepository.findByUsername(realUsername)
                return if (user == null) {
                    createSysUser(realUsername, realPassword, creator)
                } else {
                    ResultUtil.failure(-2, "用户已存在，请勿重复注册")
                }
            }
            val passwordEncode = MyUserKeyService.aesEncrypt(realPassword)
            return setSysUser(username = realUsername, passwordEncode = passwordEncode, wid = wid, sysroles = sysroles, updater = creator)
        }
        return ResultUtil.failure(msg = "非管理员权限")
    }

    // 管理员15895501880设定不能被删除
    override fun adminDelUser(wid: String): ApiResult<*> {
        val currentUser = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        if (PlayerType.ADMIN_SUPER.name in currentUser.roles) {
            if (wid == "MYUSER15847652605237599") {
                return ResultUtil.failure(msg = "不能删除15895501880用户")
            } else {
                sysUserRepository.deleteById(wid)
                return ResultUtil.success()
            }
        }
        return ResultUtil.failure(msg = "非管理员权限")
    }



}
