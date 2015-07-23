package org.prisma.processhub.bpmn.manipulation.exception;

public class FlowElementNotFoundException extends Exception {
    public FlowElementNotFoundException()
    {
    }

    public FlowElementNotFoundException(String message)
    {
        super(message);
    }

    public FlowElementNotFoundException(Throwable cause)
    {
        super(cause);
    }

    public FlowElementNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FlowElementNotFoundException(String message, Throwable cause,
                                        boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
