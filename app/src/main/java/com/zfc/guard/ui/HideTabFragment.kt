package com.zfc.guard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.zfc.guard.R

/**
 * Tab 2 — 环境隐藏
 *
 * 显示各隐藏功能的开关状态（实际模块在 LSPosed 中启用后持续生效）
 */
class HideTabFragment : Fragment() {

    private var switchMockHide: Switch? = null
    private var switchCellHide: Switch? = null
    private var switchWifiHide: Switch? = null
    private var switchSensorHide: Switch? = null
    private var switchBtHide: Switch? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hide, container, false)

        switchMockHide = view.findViewById(R.id.switchMockHide)
        switchCellHide = view.findViewById(R.id.switchCellHide)
        switchWifiHide = view.findViewById(R.id.switchWifiHide)
        switchSensorHide = view.findViewById(R.id.switchSensorHide)
        switchBtHide = view.findViewById(R.id.switchBtHide)

        // 开关仅为状态显示，实际 Hook 由 LSPosed 框架管理
        // 模块在 LSPosed 中启用后，所有 Hook 自动生效
        switchMockHide?.isChecked = true
        switchCellHide?.isChecked = true
        switchWifiHide?.isChecked = true
        switchSensorHide?.isChecked = true
        switchBtHide?.isChecked = true

        return view
    }
}
