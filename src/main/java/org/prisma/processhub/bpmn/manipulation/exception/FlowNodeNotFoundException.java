package org.prisma.processhub.bpmn.manipulation.exception;

public class FlowNodeNotFoundException extends Exception {
    public FlowNodeNotFoundException()
    {
    }

    public FlowNodeNotFoundException(String message)
    {
        super(message);
    }

    public FlowNodeNotFoundException(Throwable cause)
    {
        super(cause);
    }

    public FlowNodeNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FlowNodeNotFoundException(String message, Throwable cause,
                           boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
