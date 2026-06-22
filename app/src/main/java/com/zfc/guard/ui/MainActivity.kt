package com.zfc.guard.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.zfc.guard.R

/**
 * ZfC 主界面
 *
 * 两个 Tab：
 *   Tab 1 — 定位模拟（来自 Shadow 的 UI）
 *   Tab 2 — 环境隐藏（HideMockLocation + 传感器/蓝牙开关）
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tvStatus = findViewById(R.id.tvStatus)

        // 设置 ViewPager 适配器（两个 Fragment）
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter

        // 绑定 TabLayout 与 ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_location)
                1 -> getString(R.string.tab_hide)
                else -> ""
            }
        }.attach()

        // 初始状态
        tvStatus.text = getString(R.string.status_stopped)
    }
}
