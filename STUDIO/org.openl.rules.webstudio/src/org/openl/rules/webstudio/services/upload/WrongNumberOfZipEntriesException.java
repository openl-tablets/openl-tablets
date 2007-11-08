package org.openl.rules.webstudio.services.upload;

import org.openl.rules.webstudio.services.ServiceException;


/**
 * Thrown if count of files in zip mismatch with required.
 *
 * @author Andrey Naumenko
 */
public class WrongNumberOfZipEntriesException extends ServiceException {
    private static final long serialVersionUID = 1L;
    private int fileCount;
    private String listing;
    private String detailCode;

/**
     * Constructs a new <code>WrongNumberOfZipEntriesException</code> with
     * <code>null</code> as its detail message.
     */
    public WrongNumberOfZipEntriesException() {
        super();
    }

/**
     * Constructs a new <code>WrongNumberOfZipEntriesException</code> with the specified
     * detail message and specified detail code.
     *
     * @param message the detail message string
     * @param detailCode detail code
     */
    public WrongNumberOfZipEntriesException(String message, String detailCode) {
        super(message);
        this.detailCode = detailCode;
    }

/**
     * Constructs a new <code>WrongNumberOfZipEntriesException</code> with the specified
     * cause.
     *
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown
     */
    public WrongNumberOfZipEntriesException(Throwable cause) {
        super(cause);
    }

/**
     * Constructs a new <code>ServiceException</code> with the specified cause and
     * message.
     *
     * @param message the detail message string
     * @param cause the causing throwable. A null value is permitted and indicates that
     *        the cause is nonexistent or unknown
     */
    public WrongNumberOfZipEntriesException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Number of files in zip file.
     *
     * @return number of files in zip file
     */
    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * Listing of files in zip file.
     *
     * @return listing of files in zip file
     */
    public String getListing() {
        return listing;
    }

    public void setListing(String listing) {
        this.listing = listing;
    }

    /**
     * Detail code.
     *
     * @return detail code
     */
    public String getDetailCode() {
        return detailCode;
    }

    public void setDetailCode(String detailCode) {
        this.detailCode = detailCode;
    }
}
