package com.cleannrooster.decilib.builder.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates errors and warnings during profile validation.
 *
 * <p>Errors are fatal — {@link com.cleannrooster.decilib.builder.MobBuilder}
 * throws {@link com.cleannrooster.decilib.builder.MobProfileException} when any
 * errors are present. Warnings are surfaced via logging but do not block loading.
 */
public final class ValidationResult {

    private final List<String> errors   = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String message)   { errors.add(message); }
    public void warn(String message)    { warnings.add(message); }

    public boolean hasErrors()          { return !errors.isEmpty(); }
    public boolean hasWarnings()        { return !warnings.isEmpty(); }

    public List<String> errors()        { return Collections.unmodifiableList(errors); }
    public List<String> warnings()      { return Collections.unmodifiableList(warnings); }
}
