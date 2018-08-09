package com.rk.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum OrderType {
    INCOME(true, false),
    TRANSFER(true, true),
    OUTCOME(false, true);

    private boolean isIngoing;
    private boolean isOutgoing;

    OrderType(boolean isIngoing, boolean isOutgoing) {
        this.isIngoing = isIngoing;
        this.isOutgoing = isOutgoing;
    }

    /**
     * Determines if specified order type requires receiver.
     * @return true if receiver is necessary, false otherwise
     */
    @JsonIgnore
    public boolean isIngoing() {
        return isIngoing;
    }

    /**
     * Determines if specified order type requires sender.
     * @return true if sender is necessary, false otherwise
     */
    @JsonIgnore
    public boolean isOutgoing() {
        return isOutgoing;
    }
}
