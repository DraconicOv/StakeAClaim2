package org.stakeaclaim.protection.databases;

import org.stakeaclaim.util.FatalConfigurationLoadingException;

public class InvalidTableFormatException extends FatalConfigurationLoadingException {
	private static final long serialVersionUID = 1L;

	protected String updateFile;

    public InvalidTableFormatException(String updateFile) {
        super();
        
        this.updateFile = updateFile;
    }
    
    public String toString() {
    	return "You need to update your database to the latest version.\n" +
    			"\t\tPlease see " + this.updateFile;
    }
}
