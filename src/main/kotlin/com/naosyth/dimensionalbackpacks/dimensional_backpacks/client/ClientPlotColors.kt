package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.PlotColorPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

/** Client-side copy of each plot's wall tint, filled by [PlotColorPayload] when entering a backpack. */
object ClientPlotColors {
    const val UNTINTED = 0xFFFFFF

    private val colors = HashMap<Int, Int>()

    fun set(plotId: Int, color: Int) {
        val old = colors[plotId]
        val new = if (color == PlotColorPayload.NONE) null else color
        if (old == new) return
        if (new == null) colors.remove(plotId) else colors[plotId] = new
        Minecraft.getInstance().levelRenderer.allChanged()
    }

    fun tint(pos: BlockPos?): Int {
        val id = pos?.let { PlotManager.idAt(it) } ?: return UNTINTED
        return colors[id] ?: UNTINTED
    }
}
