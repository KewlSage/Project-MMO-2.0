package harmonised.pmmo.events;

import harmonised.pmmo.config.AutoValues;
import harmonised.pmmo.config.FConfig;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.config.JsonConfig;
import harmonised.pmmo.network.MessageDoubleTranslation;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.party.Party;
import harmonised.pmmo.party.PartyMemberInfo;
import harmonised.pmmo.pmmo_saved_data.PmmoSavedData;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import java.util.Map;

public class DamageHandler
{
    public static double getEnduranceMultiplier( EntityPlayer player )
    {
        int enduranceLevel = Skill.getLevel( Skill.ENDURANCE.toString(), player );
        double endurancePerLevel = FConfig.endurancePerLevel;
        double maxEndurance = FConfig.maxEndurance;
        double endurePercent = (enduranceLevel * endurancePerLevel);
        if( endurePercent > maxEndurance )
            endurePercent = maxEndurance;
        endurePercent /= 100;
        return endurePercent;
    }

    public static double getFallSaveChance(EntityPlayer player )
    {
        int agilityLevel = Skill.getLevel( Skill.AGILITY.toString(), player );
        double maxFallSaveChance = FConfig.maxFallSaveChance;
        double saveChancePerLevel = Math.min( maxFallSaveChance, FConfig.saveChancePerLevel / 100 );

        return agilityLevel * saveChancePerLevel;
    }

    public static void handleDamage( LivingDamageEvent event )
    {
        if( !(event.getEntity() instanceof FakePlayer) )
        {
            double damage = event.getAmount();
            double startDmg = damage;
            EntityLivingBase target = event.getEntityLiving();
            Entity source = event.getSource().getTrueSource();
            if( target instanceof EntityPlayerMP )		//player hurt
            {
                EntityPlayerMP player = (EntityPlayerMP) target;
                double agilityXp = 0;
                double enduranceXp;
                boolean hideEndurance = false;
///////////////////////////////////////////////////////////////////////PARTY//////////////////////////////////////////////////////////////////////////////////////////
                if( source instanceof EntityPlayerMP && !(source instanceof FakePlayer) )
                {
                    EntityPlayerMP sourcePlayer = (EntityPlayerMP) source;
                    Party party = PmmoSavedData.get().getParty( player.getUniqueID() );
                    if( party != null )
                    {
                        PartyMemberInfo sourceMemberInfo = party.getMemberInfo( sourcePlayer.getUniqueID() );
                        double friendlyFireMultiplier = FConfig.partyFriendlyFireAmount / 100D;

                        if( sourceMemberInfo != null )
                            damage *= friendlyFireMultiplier;
                        if( damage == 0 )
                        {
                            event.setCanceled( true );
                            return;
                        }
                    }
                }
///////////////////////////////////////////////////////////////////////ENDURANCE//////////////////////////////////////////////////////////////////////////////////////////
                double endured = Math.max( 0, damage * getEnduranceMultiplier( player ) );

                damage -= endured;

                enduranceXp = ( damage * 5 ) + ( endured * 7.5 );
///////////////////////////////////////////////////////////////////////FALL//////////////////////////////////////////////////////////////////////////////////////////////
                if( event.getSource().getDamageType().equals( "fall" ) )
                {
                    double award;
                    int saved = 0;

                    double chance = getFallSaveChance( player );
                    for( int i = 0; i < damage; i++ )
                    {
                        if( Math.ceil( Math.random() * 100 ) <= chance )
                        {
                            saved++;
                        }
                    }
                    damage -= saved;

                    if( damage <= 0 )
                        event.setCanceled( true );

                    if( saved != 0 && player.getHealth() > damage )
                        player.sendStatusMessage( new TextComponentTranslation( "pmmo.savedFall", saved ), true );

                    award = saved * 5;

                    agilityXp = award;
                }

                event.setAmount( (float) damage );

                if( player.getHealth() > damage )
                {
                    if( agilityXp > 0 )
                        hideEndurance = true;

                    if( event.getSource().getTrueSource() != null )
                        XP.awardXp( player, Skill.ENDURANCE.toString(), event.getSource().getTrueSource().getDisplayName().getUnformattedText(), enduranceXp, hideEndurance, false, false );
                    else
                        XP.awardXp( player, Skill.ENDURANCE.toString(), event.getSource().getDamageType(), enduranceXp, hideEndurance, false, false );

                    if( agilityXp > 0 )
                        XP.awardXp( player, Skill.AGILITY.toString(), "surviving " + startDmg + " fall damage", agilityXp, false, false, false );
                }
            }

///////////////////////////////////////Attacking////////////////////////////////////////////////////////////

            if ( target instanceof EntityLiving && event.getSource().getTrueSource() instanceof EntityPlayerMP )
            {
//				IAttributeInstance test = target.getAttributeMap().getAttribute( Attributes.GENERIC_MOVEMENT_SPEED );
//				if( !(target instanceof EntityAnimal) )
//					System.out.println( test.getValue() + " " + test.getBaseValue() );

                EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();

                if( XP.isHoldingDebugItemInOffhand( player ) )
                    player.sendStatusMessage( new TextComponentString( target.getName() ), false );

                if( XP.isPlayerSurvival( player ) )
                {
                    ItemStack itemStack = player.getHeldItemMainhand();
                    ResourceLocation resLoc = player.getHeldItemMainhand().getItem().getRegistryName();
                    Map<String, Double> weaponReq = XP.getJsonMap( resLoc, JType.REQ_WEAPON );
                    String skill = event.getSource().damageType.equals( "arrow" ) ? Skill.ARCHERY.toString() : Skill.COMBAT.toString();
                    String itemSpecificSkill = AutoValues.getItemSpecificSkill( itemStack.getItem().getRegistryName().toString() );
                    if( skill.equals( Skill.COMBAT.toString() ) )
                        skill = itemSpecificSkill;
                    if( FConfig.getConfig( "wearReqEnabled" ) != 0 && FConfig.getConfig( "autoGenerateValuesEnabled" ) != 0 && FConfig.getConfig( "autoGenerateWeaponReqDynamicallyEnabled" ) != 0 )
                        weaponReq.put( skill, weaponReq.getOrDefault( skill, AutoValues.getWeaponReqFromStack( itemStack ) ) );
                    int weaponGap = XP.getSkillReqGap( player, weaponReq );
                    int enchantGap = XP.getSkillReqGap( player, XP.getEnchantsUseReq( player.getHeldItemMainhand() ) );
                    int gap = Math.max( weaponGap, enchantGap );
                    if( gap > 0 )
                    {
                        if( enchantGap < gap )
                            NetworkHandler.sendToPlayer( new MessageDoubleTranslation( "pmmo.notSkilledEnoughToUseAsWeapon", player.getHeldItemMainhand().getDisplayName(), "", true, 2 ), (EntityPlayerMP) player );
                        if( FConfig.strictReqWeapon )
                        {
                            event.setCanceled( true );
                            return;
                        }
                    }

                    //Apply damage bonuses
                    damage = event.getAmount();
                    if( skill.equals( Skill.COMBAT.toString() ) )
                        damage += Skill.getLevel( skill, player ) / FConfig.levelsPerDamageMelee;
                    else if( skill.equals( Skill.ARCHERY.toString() ) )
                        damage += Skill.getLevel( skill, player ) / FConfig.levelsPerDamageArchery;
                    else if( skill.equals( Skill.MAGIC.toString() ) )
                        damage += Skill.getLevel( skill, player ) / FConfig.levelsPerDamageMagic;

                    int killGap = 0;

                    if( target.getName() != null )
                    {
                        killGap = XP.getSkillReqGap( player, XP.getResLoc( target.getName() ), JType.REQ_KILL );
                        if( killGap > 0 )
                        {
                            player.sendStatusMessage( new TextComponentTranslation( "pmmo.notSkilledEnoughToDamage", new TextComponentTranslation( target.getName() ) ).setStyle( XP.textStyle.get( "red" ) ), true );
                            player.sendStatusMessage( new TextComponentTranslation( "pmmo.notSkilledEnoughToDamage", new TextComponentTranslation( target.getName() ) ).setStyle( XP.textStyle.get( "red" ) ), false );

                            for( Map.Entry<String, Double> entry : JsonConfig.data.get( JType.REQ_KILL ).get( target.getName() ).entrySet() )
                            {
                                int level = Skill.getLevel( entry.getKey(), player );

                                if( level < entry.getValue() )
                                    player.sendStatusMessage( new TextComponentTranslation( "pmmo.levelDisplay", new TextComponentTranslation( "pmmo." + entry.getKey() ), "" + (int) Math.floor( entry.getValue() ) ).setStyle( XP.textStyle.get( "red" ) ), false );
                                else
                                    player.sendStatusMessage( new TextComponentTranslation( "pmmo.levelDisplay", new TextComponentTranslation( "pmmo." + entry.getKey() ), "" + (int) Math.floor( entry.getValue() ) ).setStyle( XP.textStyle.get( "green" ) ), false );
                            }
                        }
                        if( FConfig.strictReqKill )
                        {
                            event.setCanceled( true );
                            return;
                        }
                    }
                    float amount = 0;
                    float playerHealth = player.getHealth();
                    float targetHealth = target.getHealth();
                    float targetMaxHealth = target.getMaxHealth();
                    float lowHpBonus = 1.0f;

                    //apply damage penalties
                    damage /= weaponGap + 1;
                    damage /= killGap + 1;
                    event.setAmount( (float) damage );

                    //no overkill xp
                    if( damage > targetHealth )
                        damage = targetHealth;

                    amount += damage * 3;

                    //kill reduce xp
                    if ( startDmg >= targetHealth )
                        amount /= 2;

                    //max hp kill reduce xp
                    if( startDmg >= targetMaxHealth )
                        amount /= 1.5;

//					player.setHealth( 1f );

                    //reduce xp if passive mob
                    if( target instanceof EntityAnimal )
                        amount /= 2;
                    else if( playerHealth <= 10 )	//increase xp if aggresive mob and player low on hp
                    {
                        lowHpBonus += ( 11 - playerHealth ) / 5;
                        if( playerHealth <= 2 )
                            lowHpBonus += 1;
                    }

                    if( skill.equals( Skill.ARCHERY.toString() ) || skill.equals( Skill.MAGIC.toString() ) )
                    {
                        double distance = event.getEntity().getDistance( player );
                        if( distance > 16 )
                            distance -= 16;
                        else
                            distance = 0;

                        amount += ( Math.pow( distance, 1.25 ) * ( damage / target.getMaxHealth() ) * ( damage >= targetMaxHealth ? 1.5 : 1 ) );	//add distance xp
                        amount *= lowHpBonus;

                        XP.awardXp( player, skill, player.getHeldItemMainhand().getDisplayName(), amount, false, false, false );
                    }
                    else
                    {
                        amount *= lowHpBonus;
                        XP.awardXp( player, skill, player.getHeldItemMainhand().getDisplayName(), amount, false, false, false );
                    }

                    if( weaponGap > 0 )
                        player.getHeldItemMainhand().damageItem( weaponGap - 1, player );
                }
            }
        }
    }
}