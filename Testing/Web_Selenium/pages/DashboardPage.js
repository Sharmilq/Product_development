const { By } = require('selenium-webdriver');
const BasePage = require('./BasePage');

class DashboardPage extends BasePage {
    constructor(driver) {
        super(driver);
    }

    // Locators
    get pageHeader() { return By.xpath("//h1[contains(text(), 'Hello')]"); }
    get brushingCheckbox() { return By.xpath("//span[contains(text(), 'Brushing Done')]/preceding-sibling::input"); }
    get flossingCheckbox() { return By.xpath("//span[contains(text(), 'Flossing Done')]/preceding-sibling::input"); }
    get streakCount() { return By.xpath("//p[contains(text(), 'Days')]"); }
    
    get startAssessmentLink() { return By.xpath("//a[contains(@href, '/assessment') and contains(text(), 'Start')]"); }
    get viewAssessmentLink() { return By.xpath("//a[contains(@href, '/assessment-result') and contains(text(), 'View')]"); }
    get scanTeethLink() { return By.xpath("//a[contains(@href, '/tooth-scan') and contains(text(), 'Scan')]"); }
    get scheduleAppointmentLink() { return By.xpath("//a[contains(@href, '/visit-reminders') and contains(text(), 'Schedule')]"); }
    get manageAppointmentLink() { return By.xpath("//a[contains(@href, '/visit-reminders') and contains(text(), 'Manage')]"); }

    // Quick Action Locators
    get practiceBrushingCard() { return By.xpath("//h3[text()='Practice Brushing']/ancestor::div[contains(@class, 'cursor-pointer')]"); }
    get aiToothScanCard() { return By.xpath("//h3[text()='AI Tooth Scan']/ancestor::div[contains(@class, 'cursor-pointer')]"); }
    get checkOralHealthCard() { return By.xpath("//h3[text()='Check Oral Health']/ancestor::div[contains(@class, 'cursor-pointer')]"); }
    get remindersCard() { return By.xpath("//h3[contains(text(), 'Reminders')]/ancestor::div[contains(@class, 'cursor-pointer')]"); }

    // Methods
    async openDashboard() {
        await this.open('/dashboard');
    }

    async toggleBrushing() {
        await this.click(this.brushingCheckbox);
    }

    async toggleFlossing() {
        await this.click(this.flossingCheckbox);
    }

    async getStreakCount() {
        return await this.getText(this.streakCount);
    }

    async clickPracticeBrushing() {
        await this.click(this.practiceBrushingCard);
    }

    async clickAiToothScan() {
        await this.click(this.aiToothScanCard);
    }
}

module.exports = DashboardPage;
