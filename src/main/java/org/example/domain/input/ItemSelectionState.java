package org.example.domain.input;

import org.example.domain.enums.ItemType;

public class ItemSelectionState {
    private boolean awaitingSelection = false;
    private ItemType pendingItemType = null;

    public boolean isAwaitingSelection() {
        return awaitingSelection;
    }

    public ItemType getPendingItemType() {
        return pendingItemType;
    }

    public void setAwaitingSelection(ItemType itemType) {
        this.awaitingSelection = true;
        this.pendingItemType = itemType;
    }

    public void resetAwaitingState() {
        this.awaitingSelection = false;
        this.pendingItemType = null;
    }
}
