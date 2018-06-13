package club.vinnymaker.datastore;

/**
 * An exception raised in user APIs when invalid user details are provided. 
 *  
 * @author evinay
 *
 */
public class InvalidUserException extends Exception {
	private static final long serialVersionUID = 7049959152988390258L;
	
	public InvalidUserException(String reason) {
		super(reason);
	}
}
