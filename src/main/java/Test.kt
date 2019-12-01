import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe
import java.util.*
import kotlin.collections.ArrayList


fun main() {
    BasicConfigurator.configure();

    val driver: WebDriver = ChromeDriver()
    driver.get("http://category.gmarket.co.kr/listview/L100000014.aspx")
    val document = driver.findElement(By.tagName("body"))
    val a = Main()
    val res = a.createTopCategories(document)
}

class Main() {
    val logger = Logger.getLogger(this.javaClass.name)

    fun loadMidCategoryPages(driver: ChromeDriver, categoryIndexes: List<Int>) {
        val originalWindow = driver.windowHandle
        val count = categoryIndexes.size

        //Check we don't have other windows open already
        assert(driver.windowHandles.size == 1)

        //Click the link which opens in a new windows
        for (i in 1..count) {
            driver.findElement(By.linkText("new window")).click()
        }

        //Wait for the new window or tab
        WebDriverWait(driver, 10)
            .until(numberOfWindowsToBe(count + 1))

        //Loop through until we find a new window handle
        val stack = Stack<Int>().apply {
            addAll(categoryIndexes)
        }
        for (windowHandle in driver.windowHandles) {
            if (!originalWindow.contentEquals(windowHandle)) {
                driver.switchTo().window(windowHandle)
                    .get("http://browse.gmarket.co.kr/list?category=${stack.pop()}")
            }
        }
        driver.windowHandles
    }

    fun createTopCategories(document: WebElement): List<TopCategory> {
        //find TopCategory element which holds title
        val titleElements: List<WebElement> = document.findElements(By.ByCssSelector("#gnb > .cate_title"))

        //create topCategory list
        val topCategories: List<TopCategory> = titleElements.foldIndexed(ArrayList()) { index, categoryLists, el ->
            val title = el.text
            //find mid category in top category
            val midCategories: List<Category> = document.findElements(
                // find mid categories a tags in index+1 th category
                // nth-child starts with 1 not 0. so you need to plus 1
                By.ByCssSelector("#gnb > .mid-cate>  li:nth-child(${index + 1}) > a:not(.arrow)")
            //create mid category list
            ).fold(ArrayList(index)) { acc, categoryEl ->
                val pattern = "category=([0-9])\\w+'".toRegex()
                acc.add(
                    Category(
                        categoryEl.text,
                        // ex) javascript:GoSNAChannel('CCP0A003', 'http://browse.gmarket.co.kr/list?category=200000458', '')
                        pattern.find(categoryEl.getAttribute("href"))
                            ?.value?.slice(IntRange(9, 17))?.toInt() ?: -1
                    )
                )
                return@fold acc
            }
            //add created top category to result(category list)
            categoryLists.add(TopCategory(title, midCategories))
            return@foldIndexed categoryLists
        }
        logger.debug(topCategories)
        return topCategories
    }
}


data class TopCategory(
    val title: String,
    val mid_categories: List<Category>
)

data class Category(
    val title: String,
    val categoryIndex: Int
)
