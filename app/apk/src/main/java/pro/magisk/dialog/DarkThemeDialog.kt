package pro.magisk.dialog

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import pro.magisk.R
import pro.magisk.arch.UIActivity
import pro.magisk.core.Config
import pro.magisk.events.DialogBuilder
import pro.magisk.view.MagiskDialog
import pro.magisk.core.R as CoreR

class DarkThemeDialog : DialogBuilder {

    override fun build(dialog: MagiskDialog) {
        val activity = dialog.ownerActivity!!
        dialog.apply {
            setTitle(CoreR.string.settings_dark_mode_title)
            setMessage(CoreR.string.settings_dark_mode_message)
            setButton(MagiskDialog.ButtonType.POSITIVE) {
                text = CoreR.string.settings_dark_mode_light
                icon = R.drawable.ic_day
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_NO, activity) }
            }
            setButton(MagiskDialog.ButtonType.NEUTRAL) {
                text = CoreR.string.settings_dark_mode_system
                icon = R.drawable.ic_day_night
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, activity) }
            }
            setButton(MagiskDialog.ButtonType.NEGATIVE) {
                text = CoreR.string.settings_dark_mode_dark
                icon = R.drawable.ic_night
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_YES, activity) }
            }
        }
    }

    private fun selectTheme(mode: Int, activity: Activity) {
        Config.darkTheme = mode
        (activity as UIActivity<*>).delegate.localNightMode = mode
    }
}
