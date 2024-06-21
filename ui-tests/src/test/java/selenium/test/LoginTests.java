package selenium.test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoginTests {

    private WebDriver driver;

    @BeforeEach
    public void setUp() {
        // Assurez-vous que le chemin vers votre WebDriver est correctement configuré
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");
        // Initialise une nouvelle instance de ChromeDriver,
        driver = new ChromeDriver();
    }

    @Test
    public void testLogin() {
        // Ouvrez la page de connexion
        driver.get("http://localhost:8067/jw/web/login");

        // Attente explicite pour l'élément de nom d'utilisateur
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement usernameElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));

        // Saisie du nom d'utilisateur et du mot de passe
        usernameElement.sendKeys("admin");
        driver.findElement(By.id("j_password")).sendKeys("admin");
        driver.findElement(By.name("submit")).click();

        // Attendre que la page de console se charge après la connexion
        wait.until(ExpectedConditions.urlToBe("http://localhost:8067/jw/web/console/home"));

        // Vérifier que l'URL actuelle est celle attendue pour la page de console
        assertEquals("http://localhost:8067/jw/web/console/home", driver.getCurrentUrl());
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
