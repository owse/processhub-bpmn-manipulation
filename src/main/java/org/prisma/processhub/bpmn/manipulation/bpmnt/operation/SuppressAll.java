package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import java.util.Collection;

public class SuppressAll extends BpmntOperation {
    private Collection<String> collectionSuppressedElementsIds;

    public SuppressAll(int executionOrder, Collection<String> collectionSuppressedElementsIds) {
        this.executionOrder = executionOrder;
        this.collectionSuppressedElementsIds = collectionSuppressedElementsIds;
        name = "SuppressAll";
    }

    public Collection<String> getCollectionSuppressedElementsIds() {
        return collectionSuppressedElementsIds;
    }
}
