package org.obolibrary.robot.exceptions;

/**
 * Created by edouglass on 9/7/17.
 *
 * Generic Exception for when writing queries
 */
public class ResultsWritingException extends RuntimeException {

    public ResultsWritingException(String message, Throwable cause) {
        super(message, cause);
    }
}
