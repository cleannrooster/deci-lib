package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.builder.visual.VisualTheme;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TextureTable {

    private static final Identifier FALLBACK =
            Identifier.of(Decilib.MOD_ID, "textures/entity/missing.png");

    private TextureTable() {}

    public static Identifier get(Form form, VisualTheme theme) {
        if (form == null) return FALLBACK;
        var string = form.name().toLowerCase();
        if(theme != null){
            string += "_"+ theme.name().toLowerCase();
        }
        return Identifier.of(Decilib.MOD_ID,
                "textures/entity/" + string + ".png");
    }
}
