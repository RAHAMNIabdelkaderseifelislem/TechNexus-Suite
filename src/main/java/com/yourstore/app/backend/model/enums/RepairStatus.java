package com.yourstore.app.backend.model.enums;

public enum RepairStatus {
    PENDING_ASSESSMENT("Pending Assessment"),
    ASSESSED_WAITING_APPROVAL("Assessed - Waiting Customer Approval"),
    WAITING_FOR_PARTS("Waiting for Parts"),
    IN_PROGRESS("In Progress"),
    READY_FOR_PICKUP("Ready for Pickup"),
    COMPLETED_PAID("Completed & Paid"),
    COMPLETED_UNPAID("Completed & Unpaid"),
    CANCELLED_BY_CUSTOMER("Cancelled by Customer"),
    CANCELLED_BY_STORE("Cancelled by Store"),
    UNREPAIRABLE("Unrepairable");

    private final String displayName;

    RepairStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}