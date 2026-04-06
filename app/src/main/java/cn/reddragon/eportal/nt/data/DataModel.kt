package cn.reddragon.eportal.nt.data

import cn.reddragon.eportal.nt.auth.ServiceType
import kotlinx.serialization.Serializable
import java.net.Inet4Address
import java.net.InetAddress

/**
 * 当前在线用户的数据
 * @param username 姓名
 * @param studentId 学号
 * @param loginType 登录方式
 * @param pack 套餐
 * @param fee 余额
 * @param remainingDuration 剩余时长
 * @param devices 在线设备数量
 * @param ip ip地址
 */
data class UserStatus(
    val username: String,
    val studentId: String,
    val loginType: ServiceType,
    val pack: String,
    val fee: Double,
    val remainingDuration: Int,
    val devices: Int,
    val ip: String,
) {
    companion object {
        fun default() = UserStatus("-", "-", ServiceType.WAN, "-", 0.0, 0, 0, "-")
    }
}

/**
 * 校园网账号
 */
@Serializable
data class AccountItem(
    val username: String,
    val studentId: String,
    val password: String
)