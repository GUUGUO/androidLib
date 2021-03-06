package com.guuguo.android.lib.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.CallSuper
import android.support.annotation.ColorInt
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.graphics.drawable.DrawableCompat.setTint
import android.support.v7.widget.DrawableUtils
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import com.guuguo.android.R
import com.guuguo.android.dialog.dialog.NormalListDialog
import com.guuguo.android.dialog.dialog.TipDialog
import com.guuguo.android.dialog.utils.DialogHelper
import com.guuguo.android.dialog.utils.dialogWarningShow
import com.guuguo.android.dialog.utils.showDialogOnMain
import com.guuguo.android.lib.extension.getColorCompat
import com.guuguo.android.lib.extension.initNav
import com.guuguo.android.lib.extension.safe
import com.guuguo.android.lib.extension.toast
import com.guuguo.android.lib.lifecycle.AppHelper
import com.guuguo.android.lib.systembar.SystemBarHelper
import com.guuguo.android.lib.utils.FileUtil
import com.guuguo.android.lib.utils.MemoryLeakUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.Serializable
import java.lang.System.exit
import java.util.concurrent.TimeUnit


/**
 * Created by guodeqing on 16/5/31.
 */
abstract class LBaseActivity : RxAppCompatActivity() {

    open fun getApp() = AppHelper.app
    private var mLoadingDialog: TipDialog? = null
    /*fragment*/
    var mFragment: LBaseFragment? = null

    /*onCreate*/
    val BACK_DEFAULT = 0
    val BACK_DIALOG_CONFIRM = 1
    val BACK_WAIT_TIME = 2

    protected open fun getLayoutResId() = R.layout.base_activity_simple_back
    var activity = this
    protected open val isFullScreen = false
    protected open val backExitStyle = BACK_DEFAULT
    protected open val backWaitTime = 2000L
    private var TOUCH_TIME: Long = 0

    private fun fullScreen(): Boolean =
            isFullScreen || mFragment != null && mFragment!!.isFullScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        initFromIntent(intent)
        if (!isTaskRoot
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intent.action != null
                && intent.action == Intent.ACTION_MAIN) {
            finish()
            return
        }

        setFullScreen(fullScreen())
        initVariable(savedInstanceState)
        setLayoutResId(getLayoutResId())
        init(savedInstanceState)
    }


    protected open fun setLayoutResId(layoutResId: Int) {
        if (layoutResId != 0)
            setContentView(layoutResId)
    }

    /*toolbar*/
    open fun getToolBar(): Toolbar? = null

    open fun getBackIconRes(): Int = mFragment?.getBackIconRes().safe(R.drawable.ic_arrow_back_white_24dp)
    open fun getAppBar(): ViewGroup? = null
    protected open fun isNavigationBack() = mFragment?.isNavigationBack().safe(true)
    protected open fun isStatusBarTextDark() = false
    protected open fun initToolBar() {
        val toolBar = getToolBar()
        setSupportActionBar(toolBar)
        if (isNavigationBack())
            toolBar?.initNav(activity, getBackIconRes())
        getHeaderTitle()?.let {
            title = it
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mFragment?.let {
            if (mFragment?.onOptionsItemSelected(item)!!)
                return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** -1未初始化 0 false 1 true*/
    private var _isLight = -1

    fun isLightTheme(): Boolean {
        if (_isLight == -1) {
            val attrs = intArrayOf(R.attr.isLightTheme)
            val typedArray = activity.obtainStyledAttributes(attrs)
            val isLight = typedArray.getBoolean(0, true)
            typedArray.recycle()
            this._isLight = if (isLight) 1 else 0
            return isLight
        }
        return false
    }

    protected open fun initStatusBar() {
        if (!fullScreen()) {
            val ta = theme.obtainStyledAttributes(null, R.styleable.ActionBar, R.attr.actionBarStyle, 0)
            val color = ta.getColor(R.styleable.AppBarLayout_android_background, getColorCompat(R.color.colorPrimary))

            SystemBarHelper.tintStatusBar(activity, color, 0f)
            if (isStatusBarTextDark()) {
                SystemBarHelper.setStatusBarDarkMode(activity)
            }
        }
    }

    open fun lightBar(@ColorInt textColor: Int = Color.BLACK) {
        SystemBarHelper.tintStatusBar(activity, Color.WHITE, 0f)
        getToolBar()?.setBackgroundColor(Color.WHITE)
        getAppBar()?.setBackgroundColor(Color.WHITE)
        getToolBar()?.setTitleTextColor(textColor)
        SystemBarHelper.setStatusBarDarkMode(activity)
        getToolBar()?.navigationIcon?.let {
            val icon = DrawableCompat.wrap(it).apply {
                mutate()
                DrawableCompat.setTint(this, textColor)
            }
            getToolBar()?.navigationIcon = icon
        }
        getToolBar()?.popupTheme = R.style.Base_Widget_AppCompat_PopupMenu_Overflow
    }

    open fun darkBar(@ColorInt color: Int = 0) {
        if (color != 0) {
            getToolBar()?.setBackgroundColor(color)
            getAppBar()?.setBackgroundColor(color)
        }
        getToolBar()?.setTitleTextColor(Color.WHITE)
        getToolBar()?.navigationIcon?.let {
            val icon = DrawableCompat.wrap(it).apply {
                mutate()
                DrawableCompat.setTint(this, Color.WHITE)
            }
            getToolBar()?.navigationIcon = icon
        }
        SystemBarHelper.setStatusBarLightMode(activity)
        getToolBar()?.popupTheme = R.style.Base_Widget_AppCompat_Light_PopupMenu_Overflow
    }

/*menu and title*/

    protected open fun getHeaderTitle(): String? = ""
    override fun setTitle(title: CharSequence?) {
        supportActionBar?.title = title
    }

    protected open fun getMenuResId() = 0
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val res = getMenuResId()
        if (res != 0)
            menuInflater.inflate(res, menu)
        mFragment?.onCreateOptionsMenu(menu, menuInflater)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mFragment?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    protected open fun initVariable(savedInstanceState: Bundle?) {}
    protected open fun initView() {}
    @Deprecated("用带参数的方法吧",replaceWith = ReplaceWith("loadData(false)"), level = DeprecationLevel.WARNING)
    open fun loadData() {}
    open fun loadData(isRefresh:Boolean) {}
    @CallSuper
    protected fun init(savedInstanceState: Bundle?) {
        mFragment?.let {
            val trans = supportFragmentManager.beginTransaction()
            trans.replace(R.id.content, mFragment!! as Fragment)
            trans.commitAllowingStateLoss()
        }
        initToolBar()
        initStatusBar()
        initView()
        loadData()
        loadData(false)
    }

    @CallSuper
    override fun onDestroy() {
        mLoadingDialog = null
        MemoryLeakUtil.fixInputMethodManagerLeak(activity)
        DialogHelper.clearCalls(activity)
        super.onDestroy()
    }

    /** 初始化 单fragment activity */
    private fun initFromIntent(data: Intent?) {
        mFragment = getFragmentInstance(data)
        if (mFragment == null)
            return
        val args = data?.extras

        if (args != null) {
            mFragment!!.arguments = args
        }
    }

    open fun getFragmentInstance(data: Intent?): LBaseFragment? {
        try {
            val clz = intent.getSerializableExtra(SIMPLE_ACTIVITY_INFO) as Class<*>?
            if (data == null || clz == null) {
                return null
            }
            try {
                return Fragment.instantiate(this,clz.name) as LBaseFragment
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalArgumentException("generate fragment error. by value:" + clz.toString())
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override fun onBackPressed() {
        when (backExitStyle) {
            BACK_DIALOG_CONFIRM ->
                exitDialog()
            BACK_WAIT_TIME -> {
                if (System.currentTimeMillis() - TOUCH_TIME < backWaitTime) {
                    exit()
                } else {
                    TOUCH_TIME = System.currentTimeMillis()
                    getString(R.string.press_again_exit).toast()
                }
            }
            BACK_DEFAULT -> {
                if (mFragment != null && mFragment!!.onBackPressed())
                else {
                    super.onBackPressed()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition()
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition()
    }

    open fun overridePendingTransition() {
        mFragment?.overridePendingTransition()
//        overridePendingTransition(R.anim.h_fragment_enter, R.anim.h_fragment_exit)
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition()
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
        overridePendingTransition()
    }

    fun exitDialog() {
        dialogWarningShow("确定退出软件？", "取消", "确定") { exit() }
    }

    fun exit() {
        val home = Intent(Intent.ACTION_MAIN)
        home.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        home.addCategory(Intent.CATEGORY_HOME)
        startActivity(home)
        Completable.complete().delay(200, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
            AppHelper.mActivityLifecycle.clear()
        }.isDisposed
    }

    open fun dialogTakePhotoShow(takePhotoListener: DialogInterface.OnClickListener, pickPhotoListener: DialogInterface.OnClickListener) {
        if (FileUtil.isExternalStorageMounted()) {
            val rxPermissions = RxPermissions(this)
            rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) { // Always true pre-M
                            val strings = arrayOf("拍照", "从相册中选取")
                            val listDialog = NormalListDialog(activity, strings).title("请选择")
                            listDialog.layoutAnimation(null)
                            listDialog.setOnOperItemClickL { _, _, position, _ ->
                                if (position == 0)
                                    takePhotoListener.onClick(listDialog, position)
                                else if (position == 1)
                                    pickPhotoListener.onClick(listDialog, position)
                                listDialog.dismiss()
                            }
                            showDialogOnMain(listDialog)
                        } else {
                            "拍照权限被拒绝".toast()
                        }
                    }.isDisposed
        } else {
            "未检测到外部sd卡".toast()
        }
    }

    private fun setFullScreen(boolean: Boolean) {
        if (boolean) {
            val params = window.attributes;
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN;
            window.attributes = params;
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            val params = window.attributes;
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
            window.attributes = params;
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    private fun isValidContext(c: Context): Boolean {

        val a = c as Activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (a.isDestroyed || a.isFinishing) {
                Log.i("YXH", "Activity is invalid." + " isDestoryed-->" + a.isDestroyed + " isFinishing-->" + a.isFinishing)
                return false
            } else {
                return true
            }
        }
        return false
    }

    companion object {

        val SIMPLE_ACTIVITY_INFO = "SIMPLE_ACTIVITY_INFO"
        val SIMPLE_ACTIVITY_TOOLBAR = "SIMPLE_ACTIVITY_TOOLBAR"

        fun <F : Fragment, A : Activity> intentTo(activity: Activity, targetFragment: Class<F>, targetActivity: Class<A>, map: HashMap<String, *>? = null, targetCode: Int = 0) {
            val intent = getIntent(activity, targetFragment, targetActivity, map)
            if (targetCode == 0)
                activity.startActivity(intent)
            else
                activity.startActivityForResult(intent, targetCode)
        }

        fun <A : Activity, F : Fragment> getIntent(activity: Activity, targetFragment: Class<F>, targetActivity: Class<A>, map: HashMap<String, *>? = null): Intent {
            val intent = Intent(activity, targetActivity)
            intent.putExtra(SIMPLE_ACTIVITY_INFO, targetFragment)

            val bundle = bundleData(map)

            intent.putExtras(bundle)
            return intent
        }

        fun bundleData(map: HashMap<String, *>?): Bundle {
            val bundle = Bundle()
            map?.forEach {
                when (it.value) {
                    is String -> bundle.putString(it.key, it.value as String)
                    is Int -> bundle.putInt(it.key, it.value as Int)
                    is Float -> bundle.putFloat(it.key, it.value as Float)
                    is Parcelable -> bundle.putParcelable(it.key, it.value as Parcelable)
                    is Serializable -> bundle.putSerializable(it.key, it.value as Serializable)
                }
            }
            return bundle
        }
    }
}

