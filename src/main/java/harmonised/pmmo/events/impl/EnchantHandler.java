package harmonised.pmmo.events.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.api.enums.EventType;
import harmonised.pmmo.api.events.EnchantEvent;
import harmonised.pmmo.core.Core;
import harmonised.pmmo.features.party.PartyUtils;
import harmonised.pmmo.util.TagBuilder;
import harmonised.pmmo.util.TagUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class EnchantHandler {

	public static void handle(EnchantEvent event) {
		Core core = Core.get(event.getPlayer().getLevel());
		CompoundTag hookOutput = new CompoundTag();
		boolean serverSide = !event.getPlayer().level.isClientSide; 
		if (serverSide) {
			CompoundTag dataIn = TagBuilder.start()
					.withString(APIUtils.STACK, event.getItem().serializeNBT().getAsString())
					.withString(APIUtils.PLAYER_ID, event.getPlayer().getUUID().toString())
					.withInt(APIUtils.ENCHANT_LEVEL, event.getEnchantment().level)
					.withString(APIUtils.ENCHANT_NAME, event.getEnchantment().enchantment.getDescriptionId()).build();
			hookOutput = core.getEventTriggerRegistry().executeEventListeners(EventType.ENCHANT, event, dataIn);
		}
		hookOutput = TagUtils.mergeTags(hookOutput, core.getPerkRegistry().executePerk(EventType.ENCHANT, event.getPlayer(), hookOutput, core.getSide()));
		if (serverSide) {
			double proportion = (double)event.getEnchantment().level / (double)event.getEnchantment().enchantment.getMaxLevel();
			Map<String, Long> xpAward = core.getExperienceAwards(EventType.ENCHANT, event.getItem(), event.getPlayer(), hookOutput);
			Set<String> keys = xpAward.keySet();
			keys.forEach((skill) -> {
				xpAward.computeIfPresent(skill, (key, value) -> Double.valueOf((double)value * proportion).longValue());
			});
			List<ServerPlayer> partyMembersInRange = PartyUtils.getPartyMembersInRange((ServerPlayer) event.getPlayer());
			core.awardXP(partyMembersInRange, xpAward);	
		}
		
		
	}
}
