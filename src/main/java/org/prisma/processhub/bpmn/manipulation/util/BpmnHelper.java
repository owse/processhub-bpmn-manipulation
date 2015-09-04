package org.prisma.processhub.bpmn.manipulation.util;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.prisma.processhub.bpmn.manipulation.bpmnt.Bpmnt;
import org.prisma.processhub.bpmn.manipulation.bpmnt.BpmntModelInstance;
import org.prisma.processhub.bpmn.manipulation.exception.ElementNotFoundException;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmn;
import org.prisma.processhub.bpmn.manipulation.tailoring.TailorableBpmnModelInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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

    public static void checkIllegalState(boolean condition, String message) {
        if (condition) {
            throw new IllegalStateException(message);
        }
    }

    public static void checkElementPresent(boolean condition, String message) {
        if (!condition) {
            throw new ElementNotFoundException(message);
        }
    }

    public static boolean isGatewayDivergent(Gateway gateway) {
        if (gateway.getOutgoing().size() > 1 && gateway.getIncoming().size() == 1) {
            return true;
        }
        return false;
    }

    public static boolean isGatewayConvergent(Gateway gateway) {
        if (gateway.getIncoming().size() > 1 && gateway.getOutgoing().size() == 1) {
            return true;
        }
        return false;
    }

}
