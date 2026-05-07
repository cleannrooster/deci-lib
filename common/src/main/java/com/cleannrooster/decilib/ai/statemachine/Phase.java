package com.cleannrooster.decilib.ai.statemachine;

public record Phase(String id, int ordinal) {

    public boolean isAfter(Phase other) {
        return this.ordinal > other.ordinal;
    }

    @Override
    public String toString() {
        return "Phase[" + id + "]";
    }
}
