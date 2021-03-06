package top.guuguo.myapplication.ui.fragment

import android.app.Activity
import android.graphics.Color
import com.guuguo.android.lib.app.LBaseActivity
import com.guuguo.android.lib.app.LBaseFragment
import com.guuguo.android.lib.extension.dpToPx
import kotlinx.android.synthetic.main.fragment_dividerview.*
import top.guuguo.dividerview.DividerDrawable
import top.guuguo.myapplication.R
import top.guuguo.myapplication.ui.activity.BaseTitleActivity

class DividerViewFragment : LBaseFragment() {
    override fun getLayoutResId() = R.layout.fragment_dividerview
    val alignTypes = arrayOf(DividerDrawable.dv_LC, DividerDrawable.dv_TC, DividerDrawable.dv_RC, DividerDrawable.dv_BC, DividerDrawable.dv_LB, DividerDrawable.dv_TR, DividerDrawable.dv_RT, DividerDrawable.dv_BL)
    var alignTypesPosition = 0;
    override fun initView() {
        super.initView()

        dtv_click_add_divider.setOnClickListener {
            if (alignTypesPosition == 4 || alignTypesPosition == 0) {
                dtv_click_add_divider.delegate.clearDivider()
            }
            if (alignTypesPosition <= 3)
                dtv_click_add_divider.delegate.addDivider(alignTypes[alignTypesPosition], 2.dpToPx(), 0, Color.BLUE, 5.dpToPx())
            else
                dtv_click_add_divider.delegate.addDivider(alignTypes[alignTypesPosition], 2.dpToPx(), 30.dpToPx(), Color.BLUE, 0)
            alignTypesPosition++;
            if (alignTypesPosition == 8) {
                alignTypesPosition = 0
            }
        }
    }

    companion object {
        fun intentTo(activity: Activity) {
            LBaseActivity.intentTo(activity,DividerViewFragment::class.java,BaseTitleActivity::class.java)
        }
    }
}
