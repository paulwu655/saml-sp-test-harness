package com.keyxentic.samlsptestharness.login;

/** Whether one specific check (signature, time validity) passed, failed, or wasn't reached. */
public record ValidationOutcome(Status status, String detail) {

    public enum Status {
        PASSED, FAILED, NOT_EVALUATED
    }

    public static ValidationOutcome passed() {
        return new ValidationOutcome(Status.PASSED, null);
    }

    public static ValidationOutcome failed(String detail) {
        return new ValidationOutcome(Status.FAILED, detail);
    }

    public static ValidationOutcome notEvaluated() {
        return new ValidationOutcome(Status.NOT_EVALUATED, null);
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }
}
