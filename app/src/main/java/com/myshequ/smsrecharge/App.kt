package com.myshequ.smsrecharge

import android.app.Application
import com.myshequ.smsrecharge.util.LanguageHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 恢复应用语言：从未选择过语言时默认显示英文界面（可在设备绑定页或主界面菜单中切换）。
        // 在 Application.onCreate 中调用可保证首个 Activity 创建前生效。
        LanguageHelper.applyOnStartup(this)
    }
}
