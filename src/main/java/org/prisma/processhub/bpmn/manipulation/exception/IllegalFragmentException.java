package org.prisma.processhub.bpmn.manipulation.exception;

/**
 * Created by renan on 8/3/15.
 */

public class IllegalFragmentException extends RuntimeException {
    public IllegalFragmentException()
    {
    }

    public IllegalFragmentException(String message)
    {
        super(message);
    }

    public IllegalFragmentException(Throwable cause)
    {
        super(cause);
    }

    public IllegalFragmentException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public IllegalFragmentException(String message, Throwable cause,
                                    boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
