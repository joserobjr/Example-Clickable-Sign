package com.example.examplemod;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Inform forge that we use a class transformer
 */

// Optional annotations but it's good to provide information
@IFMLLoadingPlugin.Name("ExampleCoreMod")
@IFMLLoadingPlugin.MCVersion("1.7.10")
// We don't need to patch our own classes
@IFMLLoadingPlugin.TransformerExclusions("com.example.examplemod")
// Above 1000 to work with the same names as in development
@IFMLLoadingPlugin.SortingIndex(value = 1001)
public class ExampleCoreMod implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        // We use only one class transformer, tell forge about it
        return new String[]{
            SignClassTransformer.class.getName()
        };
    }

    // This is the only thing that we use here, the code below just tell to forge that we don't need that features

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {

    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
