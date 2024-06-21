package selenium.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoginTests {

    private WebDriver driver ;

    @BeforeEach
    public void setUp() {
        // Assurez-vous que le chemin vers votre WebDriver est correctement configuré
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");
        // Initialise une nouvelle instance de ChromeDriver,
        driver = new ChromeDriver() ;
    }
    @Test
    public void testLogin (){
        driver.get("http://localhost:8067/jw/web/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("admin");
        driver.findElement(By.name("loginButton")).click();

        assertEquals("http://localhost:8067/jw/web/console/home", driver.getCurrentUrl());
    }


    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
