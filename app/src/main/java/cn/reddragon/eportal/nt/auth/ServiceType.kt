package cn.reddragon.eportal.nt.auth

enum class ServiceType(val authName: String, val displayName: String) {
    WAN("校园外网服务(out-campus NET)", "校园网"),
    CHINAMOBILE("中国移动(CMCC NET)", "中国移动"),
    CHINATELECOM("中国电信(常州)", "中国电信(常州)"),
    CHINAUNICOM("中国联通(常州)", "中国联通(常州)")
}