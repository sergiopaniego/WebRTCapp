package com.sergiopaniegoblanco.webrtcexampleapp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class BasicVideoConferenceTest {
    WebDriver driver;

    @Before
    public void setUp() throws MalformedURLException, InterruptedException {

        System.setProperty("webdriver.chrome.driver", "/Users/sergiopaniegoblanco/Desktop/chromedriver");

        driver = new ChromeDriver();

        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
    }

    @Test
    public void testStartSession() throws InterruptedException {
        driver.get("https://demos.openvidu.io/basic-videoconference/");
        Thread.sleep(5000);
        driver.findElement(By.name("commit")).submit();
        Thread.sleep(30000);
        driver.manage().timeouts().implicitlyWait(30,TimeUnit.SECONDS);
    }

    @After
    public void End() {
        driver.quit();
    }
}
