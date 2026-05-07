package com.cleannrooster.decilib.ai.brain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrainValidationResult {

    private final List<String> warnings;
    private final List<String> errors;

    private BrainValidationResult(List<String> warnings, List<String> errors) {
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        this.errors   = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<String> warnings()  { return warnings; }
    public List<String> errors()    { return errors; }
    public boolean hasErrors()      { return !errors.isEmpty(); }
    public boolean hasWarnings()    { return !warnings.isEmpty(); }

    static Builder builder() { return new Builder(); }

    static final class Builder {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors   = new ArrayList<>();

        Builder warn(String message)  { warnings.add(message); return this; }
        Builder error(String message) { errors.add(message);   return this; }

        BrainValidationResult build() {
            return new BrainValidationResult(warnings, errors);
        }
    }
}
