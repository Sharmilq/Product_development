const fs = require('fs');
const path = require('path');

function takeScreenshot(driver, testName, isFailure = false) {
    return driver.takeScreenshot().then(
        function(image, err) {
            if (err) {
                console.error('Error taking screenshot:', err);
                return;
            }
            const dir = path.join(__dirname, '..', 'screenshots');
            if (!fs.existsSync(dir)){
                fs.mkdirSync(dir, { recursive: true });
            }
            const status = isFailure ? 'FAIL' : 'PASS';
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const safeTestName = testName.replace(/[^a-z0-9]/gi, '_').toLowerCase();
            const fileName = `${status}_${safeTestName}_${timestamp}.png`;
            const filePath = path.join(dir, fileName);
            fs.writeFileSync(filePath, image, 'base64');
            console.log(`Screenshot saved to ${filePath}`);
        }
    );
}

module.exports = { takeScreenshot };
