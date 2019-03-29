package org.openas2.processor.sender;

import java.net.HttpURLConnection;

import org.openas2.OpenAS2Exception;
import org.openas2.util.HTTPUtil;


public abstract class HttpSenderModule extends BaseSenderModule implements SenderModule {
	
	  public static final String PARAM_READ_TIMEOUT = "readtimeout";
	  public static final String PARAM_CONNECT_TIMEOUT = "connecttimeout";
	  
  	//private Log logger = LogFactory.getLog(HttpSenderModule.class.getSimpleName());
	
    public HttpURLConnection getConnection(String url, boolean output, boolean input,
        boolean useCaches, String requestMethod) throws OpenAS2Exception
    {
    	if (url == null) throw new OpenAS2Exception("HTTP sender module received empty URL string.");
        HttpURLConnection connection = HTTPUtil.getConnection(url, output, input, useCaches, requestMethod);
        return connection;
    }

    public HttpURLConnection getConnection(String url, boolean output, boolean input,
                                           boolean useCaches, String requestMethod, int readTimeout, int connectTimeout) throws OpenAS2Exception
    {
        if (url == null) throw new OpenAS2Exception("HTTP sender module received empty URL string.");
        HttpURLConnection connection = HTTPUtil.getConnection(url, output, input, useCaches, requestMethod);
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        return connection;
    }
}
