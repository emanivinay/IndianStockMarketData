package club.vinnymaker.data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity(name="users")
@Getter
@Setter
public class User {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="user_id_gen")
	@SequenceGenerator(name="user_id_gen", sequenceName="users_user_id_seq", allocationSize = 1)
	@Column(name="user_id")
	private long id;
	
	@Column(name="username")
	private String username;
	
	@Column(name="password_hash")
	private String passwordHash;
	
	@Column(name="password_salt")
	private String passwordSalt;
	
	@Column(name="date_created")
	private Date dateCreated;
	
	public User() {
	}
}
