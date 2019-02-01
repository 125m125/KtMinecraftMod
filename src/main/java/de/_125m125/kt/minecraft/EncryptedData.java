package de._125m125.kt.minecraft;

import java.io.Serializable;
import java.util.Base64;

import com.google.gson.annotations.Expose;

public class EncryptedData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6855960888013873299L;

	@Expose
	private final String cyphertext;
	@Expose
	private final String iv;
	@Expose
	private final String salt;

	public EncryptedData(final byte[] cyphertext, final byte[] iv, byte[] salt) {
		super();
		// we need strings for minecraft to write this data into the config file
		this.cyphertext = Base64.getEncoder().encodeToString(cyphertext);
		this.iv = Base64.getEncoder().encodeToString(iv);
		this.salt = Base64.getEncoder().encodeToString(salt);
	}

	public byte[] getCyphertext() {
		return Base64.getDecoder().decode(this.cyphertext);
	}

	public byte[] getIv() {
		return Base64.getDecoder().decode(this.iv);
	}

	public byte[] getSalt() {
		return Base64.getDecoder().decode(this.salt);
	}
}