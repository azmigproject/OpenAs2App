
package org.openas2.processor.sender;

import org.openas2.processor.ProcessorModule;


public interface SenderModule extends ProcessorModule {
	public static final String DO_SEND = "send";	
	public static final String DO_SENDMDN = "sendmdn";

	public static final String SSL_CLIENT_CERTIFICATE="ssl_pvt_cert";
	public static final String SSL_CLIENT_CERTIFICATE_PASSWORD="ssl_pvt_cert_pwd";
	public static final String ALLOW_HTTPAUTH="allow_httpauth";
	public static final String HTTP_AUTH_TYPE="http_auth_type";
	public static final String HTTP_AUTH_USER="http_auth_usr";
	public static final String HTTP_AUTH_USER_PWD="http_auth_usr_pwd";

	public static final String SOPT_RETRIES = "retries";
	
	public static final String DEFAULT_RETRIES = "-1";	// Infinite
}
