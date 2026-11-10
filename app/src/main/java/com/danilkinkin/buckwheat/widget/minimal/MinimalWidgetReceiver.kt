package com.danilkinkin.buckwheat.widget.minimal

import com.danilkinkin.buckwheat.widget.WidgetReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MinimalWidgetReceiver : WidgetReceiver() {
    override val glanceAppWidget = MinimalWidget()
}
