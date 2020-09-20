package harmonised.pmmo.events;

import harmonised.pmmo.config.Config;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.config.JsonConfig;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;

public class CraftedHandler
{
    private static final double defaultCraftingXp = Config.forgeConfig.defaultCraftingXp.get();

    public static void handleCrafted( PlayerEvent.ItemCraftedEvent event )
    {
        PlayerEntity player = event.getPlayer();
        if( !player.world.isRemote() )
        {
            double defaultCraftingXp = Config.forgeConfig.defaultCraftingXp.get();
            double durabilityMultiplier = 1;

            ItemStack itemStack = event.getCrafting();
            ResourceLocation resLoc = itemStack.getItem().getRegistryName();
            Map<String, Double> xpValue = XP.getXp( XP.getResLoc( resLoc.toString() ), JType.XP_VALUE_CRAFT );
            double hardness = ((BlockItem) itemStack.getItem()).getBlock().blockHardness;

            Map<String, Double> award = new HashMap<>();
            if( xpValue.size() == 0 )
            {
                if( itemStack.getItem() instanceof BlockItem)
                    award.put( "crafting", hardness );
                else
                    award.put( "crafting", defaultCraftingXp );
            }
            else
                XP.addMaps( award, xpValue );

            if( itemStack.isDamageable() )
                durabilityMultiplier = (double) ( itemStack.getMaxDamage() - itemStack.getDamage() ) / (double) itemStack.getMaxDamage();

//            XP.multiplyMap( award, itemStack.getCount() );
            XP.multiplyMap( award, durabilityMultiplier );

            for( Map.Entry<String, Double> entry : award.entrySet() )
            {
                XP.awardXp( player, Skill.getSkill( entry.getKey() ), "crafting", entry.getValue(), false, false );
            }
        }
    }
}
