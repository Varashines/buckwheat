package com.danilkinkin.buckwheat.widget.extend

import com.danilkinkin.buckwheat.widget.WidgetReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExtendWidgetReceiver : WidgetReceiver() {
    override val glanceAppWidget = ExtendWidget()
}
