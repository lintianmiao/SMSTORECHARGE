package com.myshequ.smsrecharge.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.myshequ.smsrecharge.R

/**
 * 应用语言辅助类：语言选择对话框、语言应用与启动恢复。
 *
 * 存储规则（AppSettings 的 app_language）：
 * - 未存储（null）：用户从未选择过语言，默认显示英文界面；
 * - "system"：用户选择跟随系统语言；
 * - 具体语言标签（zh/en/fr/es/pt）：对应用户选择的语言。
 *
 * 语言名称固定使用各自的母语名称，保证任何界面语言下都能找到目标语言。
 */
object LanguageHelper {

    /** 跟随系统语言的存储标记 */
    const val TAG_SYSTEM = "system"

    private const val TAG_ENGLISH = "en"

    private val tags = arrayOf(TAG_SYSTEM, "zh", TAG_ENGLISH, "fr", "es", "pt")
    private val nativeNames = arrayOf(null, "中文", "English", "Français", "Español", "Português")

    /**
     * 弹出语言选择对话框（主界面菜单与设备绑定页共用）。
     * 选择后立即生效（appcompat 自动重建 Activity）；从未选择过语言时默认勾选 English。
     */
    fun showPicker(activity: Activity) {
        val savedTag = AppSettings.getAppLanguage(activity)
        val checkedIndex = when {
            savedTag == null -> tags.indexOf(TAG_ENGLISH)
            else -> tags.indexOf(savedTag).takeIf { it >= 0 } ?: tags.indexOf(TAG_ENGLISH)
        }
        val displayNames = nativeNames.map {
            it ?: activity.getString(R.string.language_system_default)
        }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(displayNames, checkedIndex) { dialog, which ->
                apply(activity, tags[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.clear_messages_cancel, null)
            .show()
    }

    /** 保存用户的语言选择并立即应用。 */
    fun apply(context: Context, tag: String) {
        AppSettings.setAppLanguage(context, tag)
        setLocales(if (tag == TAG_SYSTEM) "" else tag)
    }

    /**
     * APP 启动时恢复语言：在 Application.onCreate 中调用（先于任何 Activity 创建），
     * 从未选择过语言时默认显示英文界面。
     */
    fun applyOnStartup(context: Context) {
        when (val savedTag = AppSettings.getAppLanguage(context)) {
            null -> setLocales(TAG_ENGLISH)
            TAG_SYSTEM -> setLocales("")
            else -> setLocales(savedTag)
        }
    }

    /** 应用语言标签；空串表示清除应用级语言、跟随系统。 */
    private fun setLocales(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )
    }
}
