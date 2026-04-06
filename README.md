# EPortalNT

一个基于 **Android + Jetpack Compose** 的校园网认证客户端项目。

## 项目概览
- UI：Jetpack Compose + Material 3
- 网络：Ktor Client（OkHttp 引擎）
- 校园网 API：逆向自 EPortal 网页
- 数据持久化：DataStore + `kotlinx.serialization`

## 功能
- 账号管理：支持多个账号的添加、编辑和删除
- 自动登录：可选启动自动登录、离线自动登录
- 信息展示：展示当前账号的认证状态、剩余时长、在线设备等信息
- 快捷开关：通过通知栏快捷开关，便捷进行登录登出操作

## 截图
<img src="./art/1.jpg" width="180"/> <img src="./art/2.jpg" width="180"/> <img src="./art/3.jpg" width="180"/> <img src="./art/4.jpg" width="180"/>

## 环境要求

- Android Studio / IntelliJ IDEA
- JDK 11（项目 `sourceCompatibility/targetCompatibility` 为 11）
- Android SDK（`compileSdk = 36`，`minSdk = 29`）

## 作者
- GPT-5.3-Codex
- RedDragon0293

## 协议
MIT LICENSE