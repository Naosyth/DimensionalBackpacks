package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackItem

/** Draws a worn backpack's item model on the wearer's back. */
object BackpackRendering {
    fun draw(
        stack: ItemStack, entity: LivingEntity, model: PlayerModel<*>,
        poseStack: PoseStack, buffer: MultiBufferSource, light: Int
    ) {
        poseStack.pushPose()
        // Body space has +y pointing down and +z pointing at the wearer's back.
        model.body.translateAndRotate(poseStack)
        // The tier 1 model sits at the back of its block, so the offset also cancels that 5.5/16 shift.
        poseStack.translate(0.0, 0.3, 0.62)
        poseStack.scale(1f, -1f, -1f)
        Minecraft.getInstance().itemRenderer.renderStatic(
            stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.id
        )
        poseStack.popPose()
    }
}

/** Renders a backpack in the chest slot, which vanilla draws nothing for. Curios slots use their own renderer. */
class BackpackLayer(parent: RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) :
    RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(parent) {

    override fun render(
        poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, player: AbstractClientPlayer,
        limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float
    ) {
        val stack = player.getItemBySlot(EquipmentSlot.CHEST)
        if (stack.item !is BackpackItem) return
        BackpackRendering.draw(stack, player, parentModel, poseStack, buffer, packedLight)
    }
}
