package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;

/**
 * Created by renan on 8/3/15.
 */
public final class BpmnHelper {
    private BpmnHelper() {}

    public static <T> void checkNotNull(T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
    }

    public static void checkInvalidArgument(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkElementPresent(boolean condition, String message) {
        if (!condition) {
            throw new ElementNotFoundException(message);
        }
    }

    public static boolean isGatewayDivergent(Gateway gateway) {
        if (gateway.getSucceedingNodes().count() > 1) {
            return true;
        }
        return false;
    }

    public static boolean isGatewayConvergent(Gateway gateway) {
        if (gateway.getPreviousNodes().count() > 1) {
            return true;
        }
        return false;
    }
}
