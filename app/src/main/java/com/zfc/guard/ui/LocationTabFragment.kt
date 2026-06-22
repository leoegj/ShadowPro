package com.zfc.guard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.zfc.guard.R

/**
 * Tab 1 — 定位模拟
 *
 * 简单的坐标输入 + 状态控制界面
 * （完整版可集成高德地图 SDK，参见 Shadow 原项目）
 */
class LocationTabFragment : Fragment() {

    private var tvLat: TextView? = null
    private var tvLng: TextView? = null
    private var btnStart: Button? = null
    private var btnStop: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)
        tvLat = view.findViewById(R.id.tvLat)
        tvLng = view.findViewById(R.id.tvLng)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)

        btnStart?.setOnClickListener {
            // 写入坐标到 /data/local/tmp/irest_loc.conf
            val lat = tvLat?.text.toString()
            val lng = tvLng?.text.toString()
            try {
                val cmd = "echo '$lat,$lng' > /data/local/tmp/irest_loc.conf"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            } catch (_: Exception) { }
        }

        btnStop?.setOnClickListener {
            try {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "rm -f /data/local/tmp/irest_loc.conf"))
            } catch (_: Exception) { }
        }

        return view
    }
}
