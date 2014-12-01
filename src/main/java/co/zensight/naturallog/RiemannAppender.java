package co.zensight.naturallog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.aphyr.riemann.client.RiemannClient;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RiemannAppender extends AppenderBase<ILoggingEvent> {

    private RiemannClient riemannClient;
    private String riemannHost;
    private int riemannPort = -1;
    private String service;

    public static void sendEverythingToRiemann(RiemannClient client, String level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        RiemannAppender appender = new RiemannAppender();
        appender.setContext(context);
        appender.setRiemannClient(client);
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.valueOf(level));
        logger.addAppender(appender);
    }

    @Override
    protected void append(ILoggingEvent e) {
        lazyClient()
            .event()
            .description(e.getMessage())
            .service(getService())
            .time(e.getTimeStamp())
            .attribute("thread", e.getThreadName())
            .attribute("logger", e.getLoggerName())
            .attribute("level", e.getLevel().toString())
            .tag("log")
            .send();
    }

    public RiemannClient getRiemannClient() {
        return riemannClient;
    }

    public void setRiemannClient(RiemannClient riemannClient) {
        this.riemannClient = riemannClient;
    }

    public String getRiemannServer() {
        return riemannHost;
    }

    public void setRiemannServer(String riemannServer) {
        this.riemannHost = riemannServer;
    }

    public int getRiemannPort() {
        return riemannPort;
    }

    public void setRiemannPort(int riemannPort) {
        this.riemannPort = riemannPort;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    private RiemannClient lazyClient() {

        if (getRiemannClient() == null) {

            if (riemannHost == null) {
                throw new LogbackException("riemannHost must be set");
            }

            if (riemannPort <= 0) {
                throw new LogbackException("riemannPort must be set");
            }

            try {
                setRiemannClient(RiemannClient.tcp(riemannHost, riemannPort));

            } catch (IOException ex) {
                throw new LogbackException("Can't connect to " + riemannHost + ":" + riemannPort, ex);
            }
        }

        return getRiemannClient();
    }
}