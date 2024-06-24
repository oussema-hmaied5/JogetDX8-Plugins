        package selenium.test;


        import org.junit.jupiter.api.Test;
        import org.openqa.selenium.By;
        import org.openqa.selenium.WebElement;
        import org.openqa.selenium.support.ui.ExpectedConditions;
        import org.openqa.selenium.support.ui.WebDriverWait;

        import java.time.Duration;

        import static org.junit.jupiter.api.Assertions.assertEquals;

        public class form_repeater extends BaseUITest {

            @Test
            public void testFormRepeaterFunctionality() throws InterruptedException {

                String buildNumber = System.getenv("BUILD_NUMBER");
    //            if (buildNumber == null || buildNumber.isEmpty()) {
    //                throw new IllegalArgumentException("BUILD_NUMBER environment variable is not set");
    //            }

                driver.get("http://localhost:8067/jw/web/login");

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                WebElement usernameElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
                usernameElement.sendKeys("admin");
                driver.findElement(By.name("j_password")).sendKeys("admin");
                Thread.sleep(3000);

                driver.findElement(By.name("submit")).click();

                // Naviguer vers l'application FormRepeater et tester sa fonctionnalité
                driver.get("http://localhost:8067/jw/web/userview/FormRepater_ID/Repeater_ID/_/ajout");

                // Fill in the department field
                WebElement departmentField = driver.findElement(By.id("departement"));
                departmentField.sendKeys("Test " + buildNumber);
                Thread.sleep(3000);

                // Add a new entry to the repeater
                WebElement addButton = driver.findElement(By.cssSelector(".repeater-actions-add"));
                addButton.click();
                Thread.sleep(3000);

                // Wait for the new row to be added


                // Submit the form
                WebElement submitButton = driver.findElement(By.name("submit"));
                submitButton.click();

                // Validate the result
    //              WebElement resultElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resultElementId")));
    //            assertEquals("Expected Text After Submission", resultElement.getText());

            }

        }
