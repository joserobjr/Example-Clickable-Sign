package com.example.examplemod;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Do a simple modification on {@link net.minecraft.tileentity.TileEntitySign} to add a custom field and two static invocations.
 * <br/>
 * The final code would be:
 * <pre><code>
 *     public class TileEntitySign extends TileEntity{
 *         public NBTBase exampleModData;
 *         // ... original fields
 *
 *         public void writeToNBT(NBTTagCompound var1){
 *             // ... original code
 *             SignClassTransformer.writeToNBT(this, var1);
 *         }
 *
 *         public void readFromNBT(NBTTagCompound var1){
 *             // ... original code
 *             SignClassTransformer.readFromNBT(this, var1);
 *         }
 *     }
 * </code></pre>
 * <p>
 *     This class also allows reading and writing the custom field.
 * </p>
 */
public class SignClassTransformer implements IClassTransformer
{
    // ------- MOD PART ------- //
    // This part provide read/write access to the custom field in-game.

    /**
     * The cached field. We cache it to increase performance.
     */
    private static Field exampleModDataField;

    /**
     * Get the cached field or initialize it if it's not initialized yet
     * @param sign A instance of {@link net.minecraft.tileentity.TileEntitySign}. It's an {@link Object} arg to simplify the ASM code generation.
     * @return The cached field
     * @throws RuntimeException if the transformer failed
     */
    private static Field getExampleModDataField(Object sign)
    {
        if(exampleModDataField == null)
            try
            {
                exampleModDataField = sign.getClass().getField("exampleModData");
            } catch (NoSuchFieldException e)
            {
                throw new RuntimeException(e);
            }

        return exampleModDataField;
    }

    /**
     * Read the custom field stored in a patched {@link net.minecraft.tileentity.TileEntitySign}.
     * @param sign A instance of {@link net.minecraft.tileentity.TileEntitySign}. It's an {@link Object} arg to simplify the ASM code generation.
     * @return The custom data.
     */
    @Nullable
    public static NBTBase getExampleModDataValue(Object sign)
    {
        try
        {
            return (NBTBase) getExampleModDataField(sign).get(sign);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Store the data to the custom field.
     * @param sign A instance of {@link net.minecraft.tileentity.TileEntitySign}. It's an {@link Object} arg to simplify the ASM code generation.
     * @param modData The custom data.
     */
    public static void setExampleModDataValue(Object sign, @Nullable NBTBase modData)
    {
        try
        {
            getExampleModDataField(sign).set(sign, modData);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    // ------- CORE-MOD PART ------- //
    // This part patches the TileEntitySign to add the custom field provide save/load support

    /**
     * Method executed before {@link net.minecraft.tileentity.TileEntitySign#writeToNBT(NBTTagCompound)} returns
     * @param sign A instance of {@link net.minecraft.tileentity.TileEntitySign}. It's an {@link Object} arg to simplify the ASM code generation.
     * @param tagCompound The NBT tag that is being written
     */
    @SuppressWarnings("unused")
    public static void writeToNBT(Object sign, NBTTagCompound tagCompound)
    {
        NBTBase modData = getExampleModDataValue(sign);
        if(modData != null)
            tagCompound.setTag(ExampleMod.TAG_ROOT, modData);
    }

    /**
     * Method executed before {@link net.minecraft.tileentity.TileEntitySign#writeToNBT(NBTTagCompound)} returns
     * @param sign A instance of {@link net.minecraft.tileentity.TileEntitySign}. It's an {@link Object} arg to simplify the ASM code generation.
     * @param tagCompound The NBT tag that is being read
     */
    @SuppressWarnings("unused")
    public static void readFromNBT(Object sign, NBTTagCompound tagCompound)
    {
        NBTBase modData = tagCompound.getTag(ExampleMod.TAG_ROOT);
        setExampleModDataValue(sign, modData);
    }

    /**
     * Adds an static invocation to a method on this class, the local method must return {@code void} and accept the arguments {@link Object} and {@link NBTTagCompound}
     */
    private class SignGeneratorAdapter extends GeneratorAdapter
    {
        /**
         * The name of a method in {@link SignClassTransformer}.
         */
        String localMethodName;

        /**
         * Constructor that preserves the method name and properties attributes
         * @param mv The method that is being visited
         * @param access The method access flag
         * @param name The method name
         * @param desc The method description (arguments and return type)
         * @param localMethodName The static method on {@link SignClassTransformer} that will be executed before the visited method returns
         */
        SignGeneratorAdapter(MethodVisitor mv, int access, String name, String desc, String localMethodName)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
            this.localMethodName = localMethodName;
        }

        /**
         * Visit all instructions and add an static invocation before the method returns
         * @param opcode The current instruction
         */
        @Override
        public void visitInsn(int opcode)
        {
            // If the current instruction is return
            if(opcode == Opcodes.RETURN)
            {
                // Add: com.example.examplemod.SignClassTransformer.<localMethodName>(this, arg1);
                // before the return statement
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/example/examplemod/SignClassTransformer", localMethodName, "(Ljava/lang/Object;Lnet/minecraft/nbt/NBTTagCompound;)V", false);
            }

            // Add the current instruction
            super.visitInsn(opcode);
        }
    }

    /**
     * Patches the {@link net.minecraft.tileentity.TileEntitySign}
     * @param name Obfuscated name of the class that is being loaded
     * @param srgName SRG name of the class that is being loaded
     * @param bytes Class that is being loaded
     * @return The modified class
     */
    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        // We will modify only the Sign class
        if("net.minecraft.tileentity.TileEntitySign".equals(srgName))
        {
            // Initialize ASM
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);

            // Add the custom field to the class
            writer.visitField(Opcodes.ACC_PUBLIC, "exampleModData", "Lnet/minecraft/nbt/NBTBase;", null, null).visitEnd();

            // This will iterate over everything on the class and allow us to modify what we want
            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer)
            {
                /**
                 * Modify the writeToNBT and readFromNBT methods to save/load our custom data
                 * @param access The current method's access flag
                 * @param name The current method's name
                 * @param desc The current method's description (arguments and return type)
                 * @param signature The current method's signature
                 * @param exceptions The exceptions that can be thrown by the current method
                 * @return The modified method
                 */
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
                {
                    // Load the method
                    MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

                    // The SRG name will be on obfuscated environment
                    if("func_145841_b".equals(name) || "writeToNBT".equals(name))
                        return new SignGeneratorAdapter(methodVisitor, access, name, desc, "writeToNBT");

                    else if("func_145839_a".equals(name) || "readFromNBT".equals(name))
                        return new SignGeneratorAdapter(methodVisitor, access, name, desc, "readFromNBT");

                    // Return the unmodified method
                    return methodVisitor;
                }
            };

            // Update the reader with our modification
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            // Save and return the modified class
            bytes = writer.toByteArray();

            // This is NOT NEEDED, I'm dumping the class for testing proposes since this is an example mod
            // This will save the modified class to a file, so you can decompile it to compare with the original version
            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream(srgName+".class");
                fos.write(bytes);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if(fos != null)
                    try
                    {
                        fos.close();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
            }
        }

        // If it was the sign, return the modified class, else return the original class
        return bytes;
    }
}
