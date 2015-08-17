package org.prisma.processhub.bpmn.manipulation.bpmnt.operation;

import java.util.Collection;

public class SuppressAll extends BpmntOperation {
    private Collection<String> collectionSuppressedElementsIds;

    SuppressAll(int executionOrder, Collection<String> collectionSuppressedElementsIds) {
        this.executionOrder = executionOrder;
        this.collectionSuppressedElementsIds = collectionSuppressedElementsIds;
        name = "suppressAll";
    }

    public Collection<String> getCollectionSuppressedElementsIds() {
        return collectionSuppressedElementsIds;
    }
}
