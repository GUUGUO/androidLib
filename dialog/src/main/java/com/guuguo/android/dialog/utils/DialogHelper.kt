package com.guuguo.android.dialog.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Log.d
import android.view.Gravity
import com.guuguo.android.dialog.TipDialog
import com.guuguo.android.dialog.WarningDialog
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by mimi on 2016-11-11.
 */
object DialogHelper {
    val callMaps = HashMap<Context, CompositeDisposable>()
    //    private var mLoadingDialog: StateDialog? = null
    private var mLoadingDialog: TipDialog? = null
    private var mDialogs = HashMap<Context, ArrayList<Dialog>>()

    fun addCall(context: Context, dispose: Disposable) {
        if (callMaps.containsKey(context))
            callMaps[context]?.add(dispose)
        else {
            val composite = CompositeDisposable()
            callMaps.put(context, composite)
            composite.add(dispose)
        }
    }

    fun clearCalls(context: Context) {
        callMaps[context]?.clear()
        callMaps.remove(context)
        mLoadingDialog?.dismiss()
        mDialogs[context]?.forEach { it.dismiss() }
        mDialogs.remove(context)
    }

    fun dialogLoadingShow(context: Context, msg: String, canTouchCancel: Boolean = false, maxDelay: Long = 0, listener: DialogInterface.OnDismissListener? = null, drawable: Drawable? = null): TipDialog? {
        mLoadingDialog = TipDialog.show(context, msg.safe("加载中"), TipDialog.STATE_STYLE.loading).apply { drawable?.let { customDrawable = it } }

        if (maxDelay > 0)
            dialogDismiss(context, maxDelay, mLoadingDialog!!, listener)
        mLoadingDialog?.setCanceledOnTouchOutside(canTouchCancel)
        showDialogOnMain(context, mLoadingDialog!!)
        return mLoadingDialog
    }

    fun dialogMsgShow(context: Context, msg: String, btnText: String, listener: (() -> Unit)?): WarningDialog? {
        val normalDialog = WarningDialog(context)
                .contentGravity(Gravity.CENTER)
                .content(msg.safe())
                .btnNum(1)
                .btnText(btnText)
        normalDialog.setOnBtnClickL({
            dialogDismiss(context, 0, normalDialog, DialogInterface.OnDismissListener { listener?.invoke() })
        })
        showDialogOnMain(context, normalDialog)
        "dialogMsgShow".log()
        return normalDialog
    }

    fun dialogStateShow(context: Context, msg: String, listener: DialogInterface.OnDismissListener?, stateStyle: Int, delayTime: Long): TipDialog? {
        val stateDialog = TipDialog.show(context, msg.safe(), stateStyle)

        stateDialog.setCanceledOnTouchOutside(false)
        showDialogOnMain(context, stateDialog)
        dialogDismiss(context, delayTime, stateDialog, listener)
        "dialogStateShow".log()
        return stateDialog
    }

    fun dialogWarningShow(context: Context, msg: String, cancelStr: String, confirmStr: String, listener: (() -> Unit)?, cancelListener: (() -> Unit)? = null): WarningDialog? {
        val normalDialog = WarningDialog(context)
                .contentGravity(Gravity.CENTER)
                .content(msg.safe())
                .btnNum(2)
                .btnText(cancelStr, confirmStr)
        normalDialog.setCanceledOnTouchOutside(false)

        normalDialog.setOnBtnClickL(cancelListener, {
            normalDialog.dismiss()
            listener?.invoke()
        })
        showDialogOnMain(context, normalDialog)
        "dialogWarningShow".log()
        return normalDialog
    }

    fun showDialogOnMain(context: Context, dialog: Dialog) {
        if (context is Activity)
            if (context.isFinishing) {
                "activity already finished,can not open dialog".log()
                return
            }
        Single.just(dialog).observeOn(AndroidSchedulers.mainThread()).subscribe { d ->
            d.show()
            if (mDialogs[context] == null) {
                val list = arrayListOf(dialog)
                mDialogs.put(context, list)
            } else {
                mDialogs[context]?.add(dialog)
            }
        }
    }

    fun dialogDismiss() {
        Completable.complete().observeOn(AndroidSchedulers.mainThread()).subscribe { mLoadingDialog?.dismiss() }
    }

    fun dialogDismiss(context: Context, delay: Long = 0, dialog: Dialog, listener: DialogInterface.OnDismissListener? = null) {
        Single.just(dialog).delay(delay, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Dialog> {
                    override fun onSuccess(d: Dialog) {
                        if (context is Activity)
                            if (context.isFinishing) {
                                mDialogs.remove(context)
                                return
                            }
                        d.dismiss()
                        listener?.onDismiss(d)
                        mDialogs[context]?.remove(dialog)
                    }

                    override fun onSubscribe(d: Disposable) {
                        addCall(context, d)
                    }

                    override fun onError(e: Throwable) {}
                })
    }

    fun String?.safe(default: String = ""): String {
        if (this.isNullOrEmpty())
            return default
        else return this!!;
    }

    val DEBUG = false
    fun String.log(hint: String = ""): String {
        if (DEBUG)
            Log.i("dialog", if (hint.isEmpty()) toString() else (hint + "║ " + toString()))
        return this
    }
}