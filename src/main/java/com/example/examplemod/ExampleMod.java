package com.example.examplemod;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.MODID, version = ExampleMod.VERSION, acceptableRemoteVersions = "*")
public class ExampleMod
{
    public static final String MODID = "ExampleMod";
    public static final String TAG_ROOT = MODID+":data";
    public static final String VERSION = "1.0";
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        GameRegistry.registerTileEntity(TileEntityCustomSign.class, MODID+"Sign");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandBase()
        {
            @Override
            public String getCommandName()
            {
                return "tool";
            }

            @Override
            public String getCommandUsage(ICommandSender p_71518_1_)
            {
                return "Get a test tool";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args)
            {
                if (sender instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer) sender;

                    ItemStack stack = new ItemStack(Items.wooden_hoe);
                    NBTTagCompound toolTag = new NBTTagCompound();
                    toolTag.setString("tool", "example");
                    for (int i = 0; i < args.length; i++)
                        toolTag.setString("arg" + i, args[i]);

                    stack.setStackDisplayName("Tester Tool");
                    stack.stackTagCompound.setTag(TAG_ROOT, toolTag);
                    player.inventory.addItemStackToInventory(stack);
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
            return;

        if(event.useItem != Event.Result.DENY)
        {
            ItemStack currentItem = event.entityPlayer.inventory.getCurrentItem();
            if(currentItem != null && currentItem.stackTagCompound != null)
            {
                NBTTagCompound toolData = currentItem.stackTagCompound.getCompoundTag(TAG_ROOT);
                if(toolData != null)
                {
                    String tool = toolData.getString("tool");
                    if("example".equals(tool))
                    {
                        List<String> toolArgs = new ArrayList<String>();
                        {
                            int i = 0;
                            String arg;
                            while ( ! (arg = toolData.getString("arg" + i++)) .isEmpty() )
                                toolArgs.add(arg);
                        }

                        event.world.setBlock(event.x, event.y + 1, event.z, Blocks.standing_sign);

                        TileEntityCustomSign tileEntity = new TileEntityCustomSign();
                        NBTTagCompound signData = new NBTTagCompound();
                        {
                            int i = 0;
                            for (String arg : toolArgs)
                                signData.setString("arg" + i++, arg);
                        }

                        tileEntity.modData = signData;
                        tileEntity.signText[0] = EnumChatFormatting.RED+"[ExampleSign]";
                        tileEntity.signText[1] = "Right-click to";
                        tileEntity.signText[2] = "see all the "+EnumChatFormatting.DARK_BLUE+toolArgs.size();
                        tileEntity.signText[3] = "stored args";
                        event.world.setTileEntity(event.x, event.y +1, event.z, tileEntity);
                    }

                    return;
                }
            }
        }


        Block block = event.world.getBlock(event.x, event.y, event.z);
        if(block instanceof BlockSign)
        {
            TileEntity tileEntity = event.world.getTileEntity(event.x, event.y, event.z);
            if(tileEntity instanceof TileEntityCustomSign)
            {
                TileEntityCustomSign sign = (TileEntityCustomSign) tileEntity;
                List<String> signArgs = new ArrayList<String>();
                {
                    NBTTagCompound tagCompound = (NBTTagCompound) sign.modData;
                    int i = 0;
                    String arg;
                    while ( ! (arg = tagCompound.getString("arg" + i++)) .isEmpty() )
                        signArgs.add(arg);
                }

                event.entityPlayer.addChatMessage(new ChatComponentText("The args are: "+signArgs));
            }
        }
    }

    public static class TileEntityCustomSign extends TileEntitySign
    {
        NBTBase modData = null;

        @Override
        public void writeToNBT(NBTTagCompound nbtTag)
        {
            super.writeToNBT(nbtTag);
            if(modData != null)
                nbtTag.setTag(TAG_ROOT, modData);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbtTag)
        {
            super.readFromNBT(nbtTag);
            modData = nbtTag.getTag(TAG_ROOT);
        }
    }
}
