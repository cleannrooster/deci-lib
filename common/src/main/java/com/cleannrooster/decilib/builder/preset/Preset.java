package com.cleannrooster.decilib.builder.preset;

import com.cleannrooster.decilib.builder.MobProfile;


public interface Preset {

    String id();


    MobProfile apply(MobProfile base);
}
