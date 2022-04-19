package dev.vox.platform.kahpp.configuration.http.client.configuration;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;
import javax.net.ssl.SSLException;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

public class TimeoutAwareHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

  public TimeoutAwareHttpRequestRetryHandler(
      final int retryCount, final boolean requestSentRetryEnabled) {
    super(
        retryCount,
        requestSentRetryEnabled,
        List.of(UnknownHostException.class, ConnectException.class, SSLException.class));
  }
}
