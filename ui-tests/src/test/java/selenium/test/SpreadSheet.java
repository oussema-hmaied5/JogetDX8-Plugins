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

public class SpreadSheet extends BaseUITest {

    @Test
    public void testFormRepeaterFunctionality() {
        driver.get("http://localhost:8067/jw/web/login");


        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement usernameElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
        usernameElement.sendKeys("admin");
        driver.findElement(By.name("j_password")).sendKeys("admin");
        driver.findElement(By.name("submit")).click();

        // Naviguer vers l'application FormRepeater et tester sa fonctionnalité
        driver.get("http://localhost:8067/jw/web/userview/SpreedSheets_ID/ui_SpreedSheets_id/_/create");
        WebElement repeaterElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("formRepeaterElementId")));
        assertEquals("Expected Text", repeaterElement.getText());

        // Ajouter des assertions supplémentaires et des interactions avec les éléments du FormRepeater
    }

}
