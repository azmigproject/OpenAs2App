package org.openas2.cert;

import java.security.cert.X509Certificate;

import org.openas2.OpenAS2Exception;

public class KeyNotFoundException extends OpenAS2Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public KeyNotFoundException(X509Certificate cert, String alias) {
		super("Key Not Found For Certificate: Alias: " + alias +",Cert :"  +cert.getIssuerDN()+","+cert.getSigAlgName()+","+cert.getSubjectDN() );
	}

	public KeyNotFoundException(String alias) {
		super("Key Not Found For Certificate: Alias: " + alias);
	}
	
	public KeyNotFoundException(X509Certificate cert, String alias, Throwable cause) {
		super("Key Not Found For Certificate: Alias: " + alias +",Cert :" +cert.getIssuerDN()+","+cert.getSigAlgName()+","+cert.getSubjectDN());
		initCause(cause);
	}

	public KeyNotFoundException(String alias, Throwable cause) {
		super("Key Not Found For Certificate: Alias: " + alias);
		initCause(cause);
	}
}
