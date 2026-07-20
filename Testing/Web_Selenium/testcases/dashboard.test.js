const { expect } = require('chai');
const { getDriver } = require('../utils/driverSetup');
const DashboardPage = require('../pages/DashboardPage');
const AuthPage = require('../pages/AuthPage');

describe('Dashboard Web Tests', function() {
    this.timeout(60000);
    let driver;
    let dashboardPage;
    let authPage;

    before(async function() {
        driver = await getDriver();
        dashboardPage = new DashboardPage(driver);
        authPage = new AuthPage(driver);

        // Optional: Perform a real login or set localStorage depending on backend availability.
        // For now, we attempt to open the dashboard. If it redirects, we might need to mock auth state.
        // Let's set a mock user_id in localStorage to bypass some auth checks if possible.
        await driver.get('http://localhost:5173');
        await driver.executeScript("localStorage.setItem('dentnova_user_id', '123');");
        await driver.executeScript("localStorage.setItem('sb-your-supabase-project-auth-token', 'mock_token');");
    });

    afterEach(async function() {
        if (this.currentTest.state === 'failed') {
            const { takeScreenshot } = require('../utils/testUtils');
            await takeScreenshot(driver, this.currentTest.title, true);
        }
    });

    after(async function() {
        if (driver) {
            await driver.quit();
        }
    });

    it('should load dashboard and display user greeting', async function() {
        await dashboardPage.openDashboard();
        // Just verify the page title or basic elements are present
        const headerText = await dashboardPage.getText(dashboardPage.pageHeader);
        expect(headerText).to.include('Hello');
    });

    it('should have daily habits checkboxes', async function() {
        await dashboardPage.openDashboard();
        const brushingEl = await dashboardPage.find(dashboardPage.brushingCheckbox);
        const flossingEl = await dashboardPage.find(dashboardPage.flossingCheckbox);
        expect(brushingEl).to.not.be.null;
        expect(flossingEl).to.not.be.null;
    });

    it('should display streak count', async function() {
        await dashboardPage.openDashboard();
        const streak = await dashboardPage.getStreakCount();
        expect(streak).to.include('Days');
    });

    it('should display quick action cards', async function() {
        await dashboardPage.openDashboard();
        const practiceEl = await dashboardPage.find(dashboardPage.practiceBrushingCard);
        const scanEl = await dashboardPage.find(dashboardPage.aiToothScanCard);
        const checkEl = await dashboardPage.find(dashboardPage.checkOralHealthCard);
        expect(practiceEl).to.not.be.null;
        expect(scanEl).to.not.be.null;
        expect(checkEl).to.not.be.null;
    });

    it('should navigate to brushing timer on practice click', async function() {
        await dashboardPage.openDashboard();
        await dashboardPage.clickPracticeBrushing();
        const url = await driver.getCurrentUrl();
        expect(url).to.include('/brushing-timer');
    });
});
