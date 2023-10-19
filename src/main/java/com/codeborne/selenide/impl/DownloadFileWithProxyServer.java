package com.codeborne.selenide.impl;

import com.codeborne.selenide.Config;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.files.DownloadAction;
import com.codeborne.selenide.files.FileFilter;
import com.codeborne.selenide.proxy.FileDownloadFilter;
import com.codeborne.selenide.proxy.SelenideProxyServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class DownloadFileWithProxyServer {
  private static final Logger log = LoggerFactory.getLogger(DownloadFileWithProxyServer.class);

  private final Waiter waiter;
  private final WindowsCloser windowsCloser;

  DownloadFileWithProxyServer(Waiter waiter, WindowsCloser windowsCloser) {
    this.waiter = waiter;
    this.windowsCloser = windowsCloser;
  }

  public DownloadFileWithProxyServer() {
    this(new Waiter(), new WindowsCloser());
  }

  @CheckReturnValue
  @Nonnull
  public File download(WebElementSource anyClickableElement,
                       WebElement clickable, long timeout,
                       FileFilter fileFilter,
                       DownloadAction action) throws FileNotFoundException {

    WebDriver webDriver = anyClickableElement.driver().getWebDriver();
    return windowsCloser.runAndCloseArisedWindows(webDriver, () ->
      clickAndInterceptFileByProxyServer(anyClickableElement, clickable, timeout, fileFilter, action)
    );
  }

  @Nonnull
  private File clickAndInterceptFileByProxyServer(WebElementSource anyClickableElement, WebElement clickable,
                                                  long timeout, FileFilter fileFilter,
                                                  DownloadAction action) throws FileNotFoundException {
    Driver driver = anyClickableElement.driver();
    Config config = driver.config();
    if (!config.proxyEnabled()) {
      throw new IllegalStateException("Cannot download file: proxy server is not enabled. Setup proxyEnabled");
    }

    SelenideProxyServer proxyServer = driver.getProxy();

    FileDownloadFilter filter = proxyServer.responseFilter("download");
    if (filter == null) {
      throw new IllegalStateException("Cannot download file: download filter is not activated");
    }

    filter.activate();
    try {
      long pollingInterval = Math.max(config.pollingInterval(), 50);
      waitForPreviousDownloadsCompletion(filter, timeout, pollingInterval);

      filter.reset();
      action.perform(driver, clickable);

      waitForNewDownloads(filter, fileFilter, timeout, pollingInterval);

      if (log.isInfoEnabled()) {
        log.info("Downloaded {}", filter.downloads().filesAsString());
        log.info("Just in case, intercepted {}", filter.responsesAsString());
      }
      return filter.downloads().firstDownloadedFile(timeout, fileFilter);
    }
    finally {
      filter.deactivate();
    }
  }

  private void waitForNewDownloads(FileDownloadFilter filter, FileFilter fileFilter, long timeout, long pollingInterval) {
    waiter.wait(timeout, pollingInterval, () -> !filter.downloads().files(fileFilter).isEmpty());
  }

  private void waitForPreviousDownloadsCompletion(FileDownloadFilter filter, long timeout, long pollingInterval) {
    waiter.wait(timeout, pollingInterval, new PreviousDownloadsCompleted(filter));
  }

  @ParametersAreNonnullByDefault
  private static class PreviousDownloadsCompleted implements Supplier<Boolean> {
    private final FileDownloadFilter filter;
    private int downloadsCount = -1;

    PreviousDownloadsCompleted(FileDownloadFilter filter) {
      this.filter = filter;
    }

    @Override
    public Boolean get() {
      int previousCount = downloadsCount;
      downloadsCount = filter.downloads().size();
      return downloadsCount == previousCount;
    }
  }
}
