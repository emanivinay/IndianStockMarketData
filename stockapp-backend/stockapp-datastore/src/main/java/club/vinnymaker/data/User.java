package club.vinnymaker.data;

import java.util.Date;

import lombok.Getter;

@Getter
public class User {
	private long id;
	private String username;
	private String passwordHash;
	private String passwordSalt;
	private Date dateCreated;
}
