package com.example.demo.config;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.handler.LoggingHandler;

import java.io.File;
import java.io.IOException;

@Configuration
public class SftpConfig {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.user}")
    private String user;

    @Value("${sftp.password}")
    private String password;

    @Value("${sftp.privateKey}")
    private Resource privateKey;

    @Value("${sftp.knownHost}")
    private Resource knownHost;

    @Value("${sftp.remote-directory}")
    private String remoteDirectory;

    @Value("${sftp.local-directory}")
    private String localDirectory;

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() throws IOException {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        factory.setPassword(password);
        factory.setPrivateKey(privateKey);
        factory.setKnownHostsResource(knownHost);
        return factory;
    }

    @Bean
    public MessagingTemplate messagingTemplate() {
        return new MessagingTemplate();
    }

    @Bean
    public MessageChannel sftpLsChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel sftpGetChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel sftpPutChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel sftpMgetChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel sftpMputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel sftpReplyChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpReplyChannel")
    public MessageHandler replyHandler() {
        LoggingHandler loggingHandler = new LoggingHandler("INFO");
        loggingHandler.setLoggerName("sftpReplyLogger");
        return loggingHandler;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpLsChannel")
    public MessageHandler lsHandler() throws IOException {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), "ls", "payload");
        gateway.setLocalDirectory(new File(localDirectory));
        return gateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpGetChannel")
    public MessageHandler getHandler() throws IOException {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), "get", "payload");
        gateway.setLocalDirectory(new File(localDirectory));
        gateway.setFileExistsMode(FileExistsMode.REPLACE);
        return gateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpPutChannel")
    public MessageHandler putHandler() throws IOException {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), "put", "payload");
        gateway.setLocalDirectoryExpression(new LiteralExpression(localDirectory));
        gateway.setRemoteDirectoryExpression(new LiteralExpression(remoteDirectory));
        gateway.setFileExistsMode(FileExistsMode.REPLACE);
        gateway.setAutoCreateDirectory(false);
        return gateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpMgetChannel")
    public MessageHandler mgetHandler() throws IOException {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), "mget", "payload");
        gateway.setLocalDirectory(new File(localDirectory));
        gateway.setFileExistsMode(FileExistsMode.REPLACE);
        gateway.setAutoCreateLocalDirectory(false);
        return gateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpMputChannel")
    public MessageHandler mputHandler() throws IOException {
        SftpOutboundGateway gateway = new SftpOutboundGateway(sftpSessionFactory(), "mput", "payload");
        gateway.setLocalDirectoryExpression(new LiteralExpression(localDirectory));
        gateway.setRemoteDirectoryExpression(new LiteralExpression(remoteDirectory));
        gateway.setFileExistsMode(FileExistsMode.REPLACE);
        gateway.setAutoCreateDirectory(false);
        return gateway;
    }
}
