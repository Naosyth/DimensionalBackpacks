package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.client.ICurioRenderer

/** Only touched when Curios is loaded. */
class CuriosBackpackRenderer : ICurioRenderer {
    override fun <T : LivingEntity, M : EntityModel<T>> render(
        stack: ItemStack, slotContext: SlotContext, poseStack: PoseStack, renderLayerParent: RenderLayerParent<T, M>,
        buffer: MultiBufferSource, light: Int, limbSwing: Float, limbSwingAmount: Float, partialTicks: Float,
        ageInTicks: Float, netHeadYaw: Float, headPitch: Float
    ) {
        val model = renderLayerParent.model as? PlayerModel<*> ?: return
        BackpackRendering.draw(stack, slotContext.entity(), model, poseStack, buffer, light)
    }
}
