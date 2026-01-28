package pro.magisk.ui.install

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Spanned
import android.text.SpannedString
import android.widget.Toast
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import pro.magisk.BR
import pro.magisk.R
import pro.magisk.arch.BaseViewModel
import pro.magisk.core.AppContext
import pro.magisk.core.BuildConfig.APP_VERSION_CODE
import pro.magisk.core.Config
import pro.magisk.core.Info
import pro.magisk.core.base.ContentResultCallback
import pro.magisk.core.ktx.toast
import pro.magisk.core.repository.NetworkService
import pro.magisk.databinding.set
import pro.magisk.dialog.SecondSlotWarningDialog
import pro.magisk.events.GetContentEvent
import pro.magisk.ui.flash.FlashFragment
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File
import java.io.IOException
import pro.magisk.core.R as CoreR

class InstallViewModel(svc: NetworkService, markwon: Markwon) : BaseViewModel() {

    val isRooted get() = Info.isRooted
    val skipOptions = Info.isEmulator || (Info.isSAR && !Info.isFDE && Info.ramdisk)
    val noSecondSlot = !isRooted || !Info.isAB || Info.isEmulator

    @get:Bindable
    var step = if (skipOptions) 1 else 0
        set(value) = set(value, field, { field = it }, BR.step)

    private var methodId = -1

    @get:Bindable
    var method
        get() = methodId
        set(value) = set(value, methodId, { methodId = it }, BR.method) {
            when (it) {
                R.id.method_patch -> {
                    GetContentEvent("*/*", UriCallback()).publish()
                }
                R.id.method_inactive_slot -> {
                    SecondSlotWarningDialog().show()
                }
            }
        }

    val data: LiveData<Uri?> get() = uri

    @get:Bindable
    var notes: Spanned = SpannedString("")
        set(value) = set(value, field, { field = it }, BR.notes)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val noteFile = File(AppContext.cacheDir, "${APP_VERSION_CODE}.md")
                val noteText = when {
                    noteFile.exists() -> noteFile.readText()
                    else -> {
                        val note = svc.fetchUpdate(APP_VERSION_CODE)?.note.orEmpty()
                        if (note.isEmpty()) return@launch
                        noteFile.writeText(note)
                        note
                    }
                }
                val spanned = markwon.toMarkdown(noteText)
                withContext(Dispatchers.Main) {
                    notes = spanned
                }
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    fun install() {
        when (method) {
            R.id.method_patch -> FlashFragment.patch(data.value!!).navigate(true)
            R.id.method_direct -> FlashFragment.flash(false).navigate(true)
            R.id.method_inactive_slot -> FlashFragment.flash(true).navigate(true)
            else -> error("Unknown value")
        }
    }

    override fun onSaveState(state: Bundle) {
        state.putParcelable(
            INSTALL_STATE_KEY, InstallState(
                methodId,
                step,
                Config.keepVerity,
                Config.keepEnc,
                Config.recovery
            )
        )
    }

    override fun onRestoreState(state: Bundle) {
        state.getParcelable<InstallState>(INSTALL_STATE_KEY)?.let {
            methodId = it.method
            step = it.step
            Config.keepVerity = it.keepVerity
            Config.keepEnc = it.keepEnc
            Config.recovery = it.recovery
        }
    }

    @Parcelize
    class UriCallback : ContentResultCallback {
        override fun onActivityLaunch() {
            AppContext.toast(CoreR.string.patch_file_msg, Toast.LENGTH_LONG)
        }

        override fun onActivityResult(result: Uri) {
            uri.value = result
        }
    }

    @Parcelize
    class InstallState(
        val method: Int,
        val step: Int,
        val keepVerity: Boolean,
        val keepEnc: Boolean,
        val recovery: Boolean,
    ) : Parcelable

    companion object {
        private const val INSTALL_STATE_KEY = "install_state"
        private val uri = MutableLiveData<Uri?>()
    }
}
