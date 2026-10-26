package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

object PlotTravel {
    private const val RETURN_KEY = "dimensional_backpacks_return"

    fun enter(player: ServerPlayer, plot: Plot, pocket: ServerLevel) {
        // Keep the original return point if the player is already inside a pocket.
        if (player.level().dimension() != PlotManager.DIMENSION) {
            val data = CompoundTag()
            data.putString("dim", player.level().dimension().location().toString())
            data.putDouble("x", player.x)
            data.putDouble("y", player.y)
            data.putDouble("z", player.z)
            data.putFloat("yaw", player.yRot)
            data.putFloat("pitch", player.xRot)
            player.persistentData.put(RETURN_KEY, data)
        }
        val custom = plot.customSpawn
        PlotColorSync.send(player, plot)
        if (custom != null) {
            player.teleportTo(pocket, custom.x, custom.y, custom.z, custom.yaw, custom.pitch)
        } else {
            val pos = plot.defaultSpawnPos
            player.teleportTo(pocket, pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, player.yRot, player.xRot)
        }
        player.fallDistance = 0f
    }

    fun exit(player: ServerPlayer) {
        val server = player.server
        val data = player.persistentData.getCompound(RETURN_KEY)
        val saved = if (data.contains("dim")) {
            server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.getString("dim"))))
        } else null
        if (saved != null && data.contains("x")) {
            player.teleportTo(
                saved, data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
                data.getFloat("yaw"), data.getFloat("pitch")
            )
        } else {
            // The return point is unknown (e.g. entered by command or the dimension was removed).
            val level = server.overworld()
            val spawn = level.sharedSpawnPos
            player.teleportTo(level, spawn.x + 0.5, spawn.y.toDouble(), spawn.z + 0.5, player.yRot, player.xRot)
        }
        player.fallDistance = 0f
        player.persistentData.remove(RETURN_KEY)
    }
}
