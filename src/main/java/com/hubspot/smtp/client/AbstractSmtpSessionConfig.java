package com.hubspot.smtp.client;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.ImplementationVisibility;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;

@Immutable
@Style(typeImmutable = "*", visibility = ImplementationVisibility.PUBLIC)
abstract class AbstractSmtpSessionConfig {
  public static final Executor DIRECT_EXECUTOR = Runnable::run;

  private static final com.google.common.base.Supplier<Executor> SHARED_DEFAULT_EXECUTOR = Suppliers.memoize(AbstractSmtpSessionConfig::getSharedExecutor);

  private static ExecutorService getSharedExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("niosmtpclient-%d").build();
    return Executors.newCachedThreadPool(threadFactory);
  }

  public abstract InetSocketAddress getRemoteAddress();
  public abstract Optional<InetSocketAddress> getLocalAddress();
  public abstract Optional<Duration> getKeepAliveTimeout();
  public abstract Optional<Executor> getExecutor();

  Executor getEffectiveExecutor() {
    return getExecutor().orElse(SHARED_DEFAULT_EXECUTOR.get());
  }

  @Default
  public Duration getConnectionTimeout() {
    return Duration.ofMinutes(2);
  }

  @Default
  public Duration getReadTimeout() {
    return Duration.ofMinutes(2);
  }

  @Default
  public EnumSet<Extension> getDisabledExtensions() {
    return EnumSet.noneOf(Extension.class);
  }

  @Default
  public String getConnectionId() {
    return "unidentified-connection";
  }

  @Default
  public ByteBufAllocator getAllocator() {
    return PooledByteBufAllocator.DEFAULT;
  }

  @Default
  public Supplier<SSLEngine> getSslEngineSupplier() {
    return this::createSSLEngine;
  }

  @Default
  public Class<? extends Channel> getChannelClass() {
    return NioSocketChannel.class;
  }

  @Check
  protected void check() {
    Preconditions.checkState(!getKeepAliveTimeout().orElse(Duration.ofSeconds(1)).isZero(),
        "keepAliveTimeout must not be zero; use Optional.empty() to disable keepalive");
  }

  private SSLEngine createSSLEngine() {
    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init((KeyStore) null);

      return SslContextBuilder
          .forClient()
          .trustManager(trustManagerFactory)
          .build()
          .newEngine(getAllocator());
    } catch (Exception e) {
      throw new RuntimeException("Could not create SSLEngine", e);
    }
  }

  public static SmtpSessionConfig forRemoteAddress(String host, int port) {
    return forRemoteAddress(InetSocketAddress.createUnresolved(host, port));
  }

  public static SmtpSessionConfig forRemoteAddress(InetSocketAddress remoteAddress) {
    return SmtpSessionConfig.builder().remoteAddress(remoteAddress).build();
  }
}
