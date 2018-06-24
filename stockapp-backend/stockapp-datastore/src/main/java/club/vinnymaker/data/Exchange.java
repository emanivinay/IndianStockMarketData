package club.vinnymaker.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Getter;

/**
 * A simple POJO representing a stock exchange in our application.
 *  
 * @author evinay
 */
@Entity(name = "exchanges")
@Getter
public class Exchange {
	
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="exchange_id_gen")
	@SequenceGenerator(name = "exchange_id_gen", allocationSize = 1, sequenceName = "exchanges_exchange_id_seq")
	@Id
	@Column(name = "exchange_id")
	private long id;
	
	// Code for this exchange. NSE, BSE etc...
	@Column(name = "code")
	private final String code;
	
	// Title of this exchange, e.g., National Stock Exchange of India
	@Column(name = "title")
	private final String title;

	private Exchange(String title, String code) {
		this.title = title;
		this.code = code;
	}
	
	/**
	 * Empty constructor only for ORM purpose. Should never be called in code.
	 */
	public Exchange() {
		this(null, null);
	}
}
