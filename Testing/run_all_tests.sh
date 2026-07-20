#!/bin/bash
echo "Starting DentNova Enterprise Testing Pipeline..."

# Move to the testing directory
cd "$(dirname "$0")" || exit

echo "1. Running Web Selenium Tests..."
npm run test:web

echo "2. Running Android Appium Tests..."
npm run test:android

echo "3. Running k6 Load Tests..."
if command -v k6 &> /dev/null
then
    k6 run Load_Testing/k6/load_test.js
else
    echo "k6 could not be found, skipping load tests."
fi

echo "Testing Pipeline Execution Complete!"
