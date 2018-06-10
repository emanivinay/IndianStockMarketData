package club.vinnymaker.data;

import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
	private long id;
	private String username;
	private String passwordHash;
	private String passwordSalt;
	private Date dateCreated;
	
	public User() {
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof User)) {
			return false;
		}
		
		User u = (User) obj;
		return u.username != null && u.username.equals(username);
	}
}
