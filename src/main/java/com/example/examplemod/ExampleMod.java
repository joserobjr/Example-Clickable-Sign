package com.example.examplemod;

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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This mod is a simple example of how we can use NBT and custom TileEntities on the server to store additional data.
 */
@Mod(modid = ExampleMod.MODID, name = ExampleMod.MODID, version = ExampleMod.VERSION,

        // This mod is not required on the client and is made to be installed only on a dedicated server
        acceptableRemoteVersions = "*")
public class ExampleMod
{
    public static final String MODID = "ExampleMod";
    public static final String VERSION = "1.0";

    /**
     * The key that will be used to store all mod data on item and on the TileEntity
     */
    public static final String TAG_ROOT = MODID+":data";

    /**
     * Register our custom tile entity and register itself on the event bus
     */
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // The sign on the end isn't required
        GameRegistry.registerTileEntity(TileEntityCustomSign.class, MODID + "Sign");
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Register the /tool command
     */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        /**
         * I made it as an anonymous class for simplicity
         */
        event.registerServerCommand(new CommandBase()
        {
            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender)
            {
                // Everybody can use this command
                return true;
            }

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

                    // 1st: We create an stack
                    ItemStack stack = new ItemStack(Items.wooden_hoe);

                    // This compound will store our custom data
                    NBTTagCompound toolTag = new NBTTagCompound();

                    // Storing the tool type, just as example but we don't really need on this mod...
                    toolTag.setString("tool", "example");

                    // Store all the args as String
                    for (int i = 0; i < args.length; i++)
                        toolTag.setString("arg" + i, args[i]);

                    // We could also store an int with the amount of args to simplify the data loading,
                    // but that would be one more NBT key and isn't really necessary..

                    // Setting a custom name will also fill the stack.stackTagCompound
                    stack.setStackDisplayName("Tester Tool");
                    // Store our data on a common name
                    stack.stackTagCompound.setTag(TAG_ROOT, toolTag);
                    // Add to the player inventory
                    player.inventory.addItemStackToInventory(stack);
                }
            }
        });
    }

    /**
     * Execute our tool or show the data stored on the sign
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
            return;

        // Check if the player have a tool and if he can use it
        if(event.useItem != Event.Result.DENY)
        {
            ItemStack currentItem = event.entityPlayer.inventory.getCurrentItem();
            if(currentItem != null && currentItem.stackTagCompound != null)
            {
                NBTTagCompound toolData = currentItem.stackTagCompound.getCompoundTag(TAG_ROOT);
                if(toolData != null)
                {
                    // He has a tool, now we need to know what tool it is.
                    String tool = toolData.getString("tool");
                    if("example".equals(tool))
                    {
                        // Example tool, now we need to load the args stored in it
                        // This would be easier if we stored an extra NBT tag with the amount of args
                        // or if the amount of args were constant, but our example allows dynamic amount of args
                        List<String> toolArgs = new ArrayList<String>();
                        {
                            int i = 0;
                            String arg;

                            // The getString method returns empty when they key doesn't exists
                            while ( ! (arg = toolData.getString("arg" + i++)) .isEmpty() )
                                toolArgs.add(arg);
                        }

                        // Now we set the block above as a sign, since this is just an example we don't check if
                        // it's air or not and it will have the default position
                        event.world.setBlock(event.x, event.y + 1, event.z, Blocks.standing_sign);

                        // We will replace the TileEntitySign to our custom version
                        TileEntityCustomSign tileEntity = new TileEntityCustomSign();

                        // We will copy the args that we retrieved to the sign
                        NBTTagCompound signData = new NBTTagCompound();
                        {
                            int i = 0;
                            for (String arg : toolArgs)
                                signData.setString("arg" + i++, arg);
                        }

                        // Save our custom data and change the sign lines
                        tileEntity.modData = signData;
                        tileEntity.signText[0] = EnumChatFormatting.RED+"[ExampleSign]";
                        tileEntity.signText[1] = "Right-click to";
                        tileEntity.signText[2] = "see all the "+EnumChatFormatting.DARK_BLUE+toolArgs.size();
                        tileEntity.signText[3] = "stored args";
                        // Replace the sign tile
                        event.world.setTileEntity(event.x, event.y +1, event.z, tileEntity);
                    }

                    // The tool operation is finished
                    return;
                }
            }
        }


        // The player isn't using a tool, so let's check what block he clicked
        Block block = event.world.getBlock(event.x, event.y, event.z);

        if(block instanceof BlockSign)
        {
            // He clicked on a sign, let's check if it have our custom data
            TileEntity tileEntity = event.world.getTileEntity(event.x, event.y, event.z);
            if(tileEntity instanceof TileEntityCustomSign)
            {
                // It has our custom data, let's load and display it.
                TileEntityCustomSign sign = (TileEntityCustomSign) tileEntity;

                // The loading would be easier if we stored an integer with the amount of items as commented before
                List<String> signArgs = new ArrayList<String>();
                {
                    NBTTagCompound tagCompound = (NBTTagCompound) sign.modData;
                    int i = 0;
                    String arg;
                    while ( ! (arg = tagCompound.getString("arg" + i++)) .isEmpty() )
                        signArgs.add(arg);
                }

                // Send a chat message showing the args that are stored on this sign
                event.entityPlayer.addChatMessage(new ChatComponentText("The args are: "+signArgs));
            }
        }
    }

    /**
     * A sign with custom NBT Data
     */
    public static class TileEntityCustomSign extends TileEntitySign
    {
        /**
         * The custom data stored on this sign
         */
        NBTBase modData = null;

        @Override
        public void writeToNBT(NBTTagCompound nbtTag)
        {
            super.writeToNBT(nbtTag);

            // Add our data after the normal data is written
            if(modData != null)
                nbtTag.setTag(TAG_ROOT, modData);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbtTag)
        {
            super.readFromNBT(nbtTag);

            // Load our data after the normal data is loaded
            modData = nbtTag.getTag(TAG_ROOT);
        }
    }
}
