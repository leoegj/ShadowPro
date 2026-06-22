package com.zfc.guard.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 主界面 ViewPager 适配器
 */
class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LocationTabFragment()  // 定位模拟 Tab
            1 -> HideTabFragment()      // 环境隐藏 Tab
            else -> LocationTabFragment()
        }
    }
}
