package com.example.examplemod;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class SignClassTransformer implements IClassTransformer
{
    private static Field exampleModDataField;

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

    public static void setExampleModDataValue(Object sign, NBTBase modData)
    {
        try
        {
            getExampleModDataField(sign).set(sign, modData);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public static void writeToNBT(Object sign, NBTTagCompound tagCompound)
    {
        NBTBase modData = getExampleModDataValue(sign);
        if(modData != null)
            tagCompound.setTag(ExampleMod.TAG_ROOT, modData);
    }

    @SuppressWarnings("unused")
    public static void readFromNBT(Object sign, NBTTagCompound tagCompound)
    {
        NBTBase modData = tagCompound.getTag(ExampleMod.TAG_ROOT);
        setExampleModDataValue(sign, modData);
    }

    private class SignGeneratorAdapter extends GeneratorAdapter
    {
        String localMethodName;

        SignGeneratorAdapter(MethodVisitor mv, int access, String name, String desc, String localMethodName)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
            this.localMethodName = localMethodName;
        }

        @Override
        public void visitInsn(int opcode)
        {
            if(opcode == Opcodes.RETURN)
            {
                // before return
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/example/examplemod/SignClassTransformer", localMethodName, "(Ljava/lang/Object;Lnet/minecraft/nbt/NBTTagCompound;)V", false);
            }
            super.visitInsn(opcode);
        }
    }

    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        if("net.minecraft.tileentity.TileEntitySign".equals(srgName))
        {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);

            writer.visitField(Opcodes.ACC_PUBLIC, "exampleModData", "Lnet/minecraft/nbt/NBTBase;", null, null).visitEnd();

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer)
            {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
                {
                    MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

                    if("writeToNBT".equals(name))
                        return new SignGeneratorAdapter(methodVisitor, access, name, desc, "writeToNBT");

                    else if("readFromNBT".equals(name))
                        return new SignGeneratorAdapter(methodVisitor, access, name, desc, "readFromNBT");

                    return methodVisitor;
                }
            };

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            bytes = writer.toByteArray();
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

        return bytes;
    }
}
