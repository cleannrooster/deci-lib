package com.cleannrooster.decilib.ai;

public interface CanBulwark {

    void enterBulwark(float reflectCoeff);

    void exitBulwark();

    boolean isBulwarkActive();

    float getBulwarkReflectCoeff();
}
