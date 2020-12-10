package harmonised.pmmo.config;

import com.google.common.collect.Multimap;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoValues
{
    public static final Logger LOGGER = LogManager.getLogger();

    private static void addJsonConfigValue( String resLoc, JType jType, Map<String, Double> values, boolean fillIfExists )
    {
        double value;
        boolean hadEntry = JsonConfig.localData.get( jType ).containsKey( resLoc );
        if( !hadEntry )
            JsonConfig.localData.get( jType ).put( resLoc, new HashMap<>() );

        if( !hadEntry || fillIfExists )
        {
            for( Map.Entry<String, Double> entry : values.entrySet() )
            {
                value = entry.getValue();
                if( JsonConfig.levelJTypes.contains( jType ) && entry.getValue() > JsonConfig.maxLevel )
                    value = JsonConfig.maxLevel;
                if( !JsonConfig.localData.get( jType ).get( resLoc ).containsKey( entry.getKey() ) )
                    JsonConfig.localData.get( jType ).get( resLoc ).put( entry.getKey(), value );
            }
            if( JsonConfig.localData.get( jType ).get( resLoc ).size() == 0 )
                JsonConfig.localData.get( jType ).remove( resLoc );
        }
    }

    private static Map<Attribute, AttributeModifier> mergeMultimaps(Multimap<Attribute, AttributeModifier> ... maps )
    {
        Map<Attribute, AttributeModifier> output = new HashMap<>();

        for( Multimap<Attribute, AttributeModifier> map : maps )
        {
            for( Map.Entry<Attribute, AttributeModifier> entry : map.entries() )
            {
                output.put( entry.getKey(), entry.getValue() );
            }
        }

        return output;
    }

    public static double getWearReqFromStack( ItemStack itemStack )
    {
        Multimap<Attribute, AttributeModifier> headHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.HEAD );
        Multimap<Attribute, AttributeModifier> chestHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.CHEST );
        Multimap<Attribute, AttributeModifier> legsHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.LEGS );
        Multimap<Attribute, AttributeModifier> feetHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.FEET );

        Map<Attribute, AttributeModifier> attributes = mergeMultimaps( headHandAttributes, chestHandAttributes, legsHandAttributes, feetHandAttributes );

        AttributeModifier armorAttribute = attributes.get( Attributes.ARMOR );
        AttributeModifier armorToughnessAttribute = attributes.get( Attributes.ARMOR_TOUGHNESS );

        double armor            = armorAttribute          == null ? 0D : armorAttribute.getAmount();
        double armorToughness   = armorToughnessAttribute == null ? 0D : armorToughnessAttribute.getAmount();

        return Math.ceil( armor * Config.forgeConfig.armorReqScale.get() + armorToughness * Config.forgeConfig.armorToughnessReqScale.get() );
    }

    public static double getWeaponReqFromStack( ItemStack itemStack )
    {
        Multimap<Attribute, AttributeModifier> mainHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.MAINHAND );
        Multimap<Attribute, AttributeModifier> offHandAttributes = itemStack.getAttributeModifiers( EquipmentSlotType.OFFHAND );

        Map<Attribute, AttributeModifier> attributes = mergeMultimaps( mainHandAttributes, offHandAttributes );

        AttributeModifier attackSpeedAttribute = attributes.get( Attributes.ATTACK_SPEED );
        AttributeModifier attackDamageAttribute = attributes.get( Attributes.ATTACK_DAMAGE );

        double attackSpeed      = attackSpeedAttribute    == null ? 0D : attackSpeedAttribute.getAmount();
        double attackDamage     = attackDamageAttribute   == null ? 0D : attackDamageAttribute.getAmount();

        return Math.ceil( (attackDamage) * Config.forgeConfig.attackDamageReqScale.get() * (4+attackSpeed) );
    }

    public static Map<String, Double> getToolReqFromStack( ItemStack itemStack )
    {
        Map<String, Double> reqTool = new HashMap<>();
        Set<ToolType> toolTypes = itemStack.getToolTypes();
        double speed, toolReq;

        for( ToolType toolType : toolTypes )
        {
            if( toolType.equals( ToolType.AXE ) )
            {
                speed = itemStack.getDestroySpeed( Blocks.OAK_LOG.getDefaultState() );
                toolReq = Math.max( 1, speed * Config.forgeConfig.toolReqScaleLog.get() );
                reqTool.put( Skill.WOODCUTTING.toString(), toolReq );
            }
            if( toolType.equals( ToolType.PICKAXE ) )
            {
                speed = itemStack.getDestroySpeed( Blocks.STONE.getDefaultState() );
                toolReq = Math.max( 1, speed * Config.forgeConfig.toolReqScaleOre.get() );
                reqTool.put( Skill.MINING.toString(), toolReq );
            }
            if( toolType.equals( ToolType.SHOVEL ) )
            {
                speed = itemStack.getDestroySpeed( Blocks.DIRT.getDefaultState() );
                toolReq = Math.max( 1, speed * Config.forgeConfig.toolReqScaleDirt.get() );
                reqTool.put( Skill.EXCAVATION.toString(), toolReq );
            }
        }

        return reqTool;
    }

    public static Skill getItemSpecificSkill( String resLoc )
    {
        Map<String, Double> itemSpecificMap = JsonConfig.data.get( JType.ITEM_SPECIFIC ).getOrDefault( resLoc.toString(), new HashMap<>() );
        Skill skill = null;

        if( itemSpecificMap.getOrDefault( "meleeWeapon", 0D ) != 0 )
            skill = Skill.COMBAT;
        else if( itemSpecificMap.getOrDefault( "archeryWeapon", 0D ) != 0 )
            skill = Skill.ARCHERY;
        else if( itemSpecificMap.getOrDefault( "magicWeapon", 0D ) != 0 )
            skill = Skill.MAGIC;

        return skill;
    }

    public static void setAutoValues()
    {
        JsonConfig.maxLevel = Config.forgeConfig.maxLevel.get();

        if( Config.forgeConfig.autoGenerateValuesEnabled.get() )
        {
            for( Item item : ForgeRegistries.ITEMS )
            {
                try
                {
                    ItemStack itemStack = new ItemStack( item );
                    String resLoc = item.getRegistryName().toString();

                    double enduranceReq = getWearReqFromStack( itemStack );
                    double combatReq = getWeaponReqFromStack( itemStack );
                    Map<String, Double> reqTool = getToolReqFromStack( itemStack );

                    //Wear Req
                    if( enduranceReq > 1 && Config.forgeConfig.wearReqEnabled.get() && Config.forgeConfig.autoGenerateWearReqEnabled.get() )
                    {
                        Map<String, Double> reqWear     = new HashMap<>();
                        reqWear.put( Skill.ENDURANCE.toString(), Math.max( 1, enduranceReq ) );
                        addJsonConfigValue( resLoc, JType.REQ_WEAR, reqWear, false );
                    }

                    //Weapon Req
                    if( combatReq > 1 && Config.forgeConfig.weaponReqEnabled.get() && Config.forgeConfig.autoGenerateWeaponReqEnabled.get() )
                    {
                        Map<String, Double> reqWeapon   = new HashMap<>();
                        reqWeapon.put( getItemSpecificSkill( item.getRegistryName().toString() ).toString(),  Math.max( 1, combatReq ) );
                        addJsonConfigValue( resLoc, JType.REQ_WEAPON, reqWeapon, false );
                    }

                    //Tool Req
                    if( reqTool.size() > 0 && Config.forgeConfig.toolReqEnabled.get() && Config.forgeConfig.autoGenerateToolReqEnabled.get() )
                    {
                        addJsonConfigValue( resLoc, JType.REQ_TOOL, reqTool, true );
                    }

                    //Crafting Xp Value
                    if( Config.forgeConfig.autoGenerateCraftingXpEnabled.get() )
                    {
                        double highestToolReq = reqTool.values().stream().reduce( Math::max ).orElse( 0D );

                        double craftingXp = 0;
                        double smithingXp = 0;

                        if( enduranceReq > 0 || combatReq > 0 || highestToolReq > 0 )
                        {
                            craftingXp = enduranceReq * 10D +                           Math.max( ( Math.max( combatReq - 10, 1 ) ) * 5D,  ( Math.max( highestToolReq - 10, 1 ) ) * 5D );
                            smithingXp = ( Math.max( enduranceReq - 10, 1 ) ) * 5D  +   Math.max( ( Math.max( combatReq - 10, 1 ) ) * 2D,  ( Math.max( highestToolReq - 10, 1 ) ) * 2D );

                            craftingXp *= Config.forgeConfig.autoGeneratedCraftingXpValueMultiplierCrafting.get();
                            smithingXp *= Config.forgeConfig.autoGeneratedCraftingXpValueMultiplierSmithing.get();
                        }

                        Map<String, Double> xpValueMap = new HashMap<>();
                        if( craftingXp > 0 )
                            xpValueMap.put( Skill.CRAFTING.toString(), craftingXp );
                        if( smithingXp > 0 )
                            xpValueMap.put( Skill.SMITHING.toString(), smithingXp );
                        addJsonConfigValue( resLoc, JType.XP_VALUE_CRAFT, xpValueMap, true );
                    }
                }
                catch( Exception e )
                {
                    LOGGER.debug( e );
                }
            }
            if( Config.forgeConfig.autoGenerateExtraChanceEnabled.get() )
            {
                for( Block block : ForgeRegistries.BLOCKS )
                {
                    try
                    {
//                ItemStack itemStack = new ItemStack( block );
                        String resLoc = block.getRegistryName().toString();
                        Material material = block.getDefaultState().getMaterial();
                        Skill skill = XP.getSkill( material );
                        JType jType = JType.NONE;
                        Map<String, Double> infoMap = new HashMap<>();
                        double chance = 0;
                        Set<ResourceLocation> tags = block.getTags();

                        //Ore/Log/Plant Extra Chance
                        if( block instanceof OreBlock || tags.contains( new ResourceLocation( "forge:ores" ) ) )
                        {
                            jType = JType.INFO_ORE;
                            chance = Config.forgeConfig.defaultExtraChanceOre.get();
                        }
                        else if( block instanceof CropsBlock || tags.contains( new ResourceLocation( "minecraft:crops" ) ) )
                        {
                            jType = JType.INFO_PLANT;
                            chance = Config.forgeConfig.defaultExtraChancePlant.get();
                        }
                        else if( tags.contains( new ResourceLocation( "minecraft:logs" ) ) )
                        {
                            jType = JType.INFO_LOG;
                            chance = Config.forgeConfig.defaultExtraChanceLog.get();
                        }
                        if( !jType.equals( JType.NONE ) )
                            infoMap.put( "extraChance", chance );

                        if( infoMap.size() > 0 && infoMap.getOrDefault( "extraChance", 0D ) > 0 )
                            addJsonConfigValue( resLoc, jType, infoMap, false );
                    }
                    catch( Exception e )
                    {
                        LOGGER.error( e );
                    }
                }
            }
            JsonConfig.data = JsonConfig.localData;
        }
    }
}