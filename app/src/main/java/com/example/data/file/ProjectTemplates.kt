package com.example.data.file

object ProjectTemplates {

    const val TEMPLATE_HTML = "HTML Website"
    const val TEMPLATE_PYTHON = "Python Project"
    const val TEMPLATE_KOTLIN_ANDROID = "Android (Kotlin)"
    const val TEMPLATE_JAVA = "Java Project"
    const val TEMPLATE_MARKDOWN = "Markdown & Docs"
    const val TEMPLATE_EMPTY = "Empty Project"

    fun getTemplateFiles(templateName: String, projectName: String): Map<String, String> {
        return when (templateName) {
            TEMPLATE_HTML -> mapOf(
                "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$projectName - DevHub Preview</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="cyber-container">
    <header class="hero-header">
      <div class="badge">🚀 Built with DevHub IDE</div>
      <h1>Welcome to <span class="accent">$projectName</span></h1>
      <p class="subtitle">A full-stack responsive web project running live on Android.</p>
    </header>

    <main class="grid-card">
      <div class="card">
        <h3>⚡ Live Interaction</h3>
        <p>Click below to test JavaScript DOM manipulation and console outputs.</p>
        <div class="counter-box">
          <button id="decBtn" class="btn secondary">-</button>
          <span id="counter">0</span>
          <button id="incBtn" class="btn primary">+</button>
        </div>
      </div>

      <div class="card">
        <h3>🎨 Theme Generator</h3>
        <p>Switch dynamically between developer dark cyber and light mode.</p>
        <button id="themeBtn" class="btn neon">Toggle Glow Accent</button>
      </div>

      <div class="card full-width">
        <h3>💻 System Telemetry</h3>
        <div id="telemetry" class="code-box">Ready. Press buttons to inspect console logs.</div>
      </div>
    </main>

    <footer>
      <p>DevHub Mobile Developer IDE &bull; Real Webview Engine</p>
    </footer>
  </div>
  <script src="script.js"></script>
</body>
</html>
                """.trimIndent(),

                "style.css" to """
:root {
  --bg-color: #0d1117;
  --card-bg: #161b22;
  --accent-cyan: #00e5ff;
  --accent-blue: #3b82f6;
  --accent-purple: #8b5cf6;
  --text-main: #f0f6fc;
  --text-dim: #8b949e;
  --border-color: #30363d;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

body {
  background-color: var(--bg-color);
  color: var(--text-main);
  min-height: 100vh;
  display: flex;
  justify-content: center;
  padding: 24px 16px;
  line-height: 1.6;
}

.cyber-container {
  max-width: 800px;
  width: 100%;
}

.hero-header {
  text-align: center;
  margin-bottom: 32px;
}

.badge {
  display: inline-block;
  background: rgba(0, 229, 255, 0.1);
  color: var(--accent-cyan);
  border: 1px solid var(--accent-cyan);
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.accent {
  background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: var(--text-dim);
  font-size: 15px;
}

.grid-card {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
}

.card.full-width {
  grid-column: 1 / -1;
}

.card h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: var(--accent-cyan);
}

.counter-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

#counter {
  font-size: 28px;
  font-weight: bold;
  min-width: 48px;
  text-align: center;
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: transform 0.1s, opacity 0.2s;
}

.btn:active {
  transform: scale(0.96);
}

.btn.primary {
  background: var(--accent-blue);
  color: white;
}

.btn.secondary {
  background: #21262d;
  color: white;
  border: 1px solid var(--border-color);
}

.btn.neon {
  background: linear-gradient(135deg, var(--accent-cyan), var(--accent-blue));
  color: #070b10;
  width: 100%;
  margin-top: 12px;
}

.code-box {
  background: #090d13;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #21262d;
  font-family: monospace;
  font-size: 13px;
  color: #7ee787;
  margin-top: 10px;
  word-break: break-all;
}

footer {
  text-align: center;
  color: var(--text-dim);
  font-size: 12px;
}
                """.trimIndent(),

                "script.js" to """
console.log("DevHub IDE: HTML & JavaScript runtime loaded!");

let count = 0;
const counterEl = document.getElementById('counter');
const incBtn = document.getElementById('incBtn');
const decBtn = document.getElementById('decBtn');
const themeBtn = document.getElementById('themeBtn');
const telemetryEl = document.getElementById('telemetry');

function logEvent(action) {
  const timestamp = new Date().toLocaleTimeString();
  const msg = `[${'$'}{timestamp}] Event: ${'$'}{action} | Count = ${'$'}{count}`;
  console.log(msg);
  telemetryEl.textContent = msg;
}

incBtn.addEventListener('click', () => {
  count++;
  counterEl.textContent = count;
  logEvent('Increment Button Clicked');
});

decBtn.addEventListener('click', () => {
  count--;
  counterEl.textContent = count;
  logEvent('Decrement Button Clicked');
});

let isGlow = false;
themeBtn.addEventListener('click', () => {
  isGlow = !isGlow;
  document.body.style.filter = isGlow ? 'hue-rotate(90deg)' : 'none';
  console.info(`Theme Glow Toggled: ${'$'}{isGlow}`);
  logEvent(`Glow filter: ${'$'}{isGlow ? 'ACTIVE' : 'DEFAULT'}`);
});

console.warn("DevHub Environment ready for real-time mobile debugging.");
                """.trimIndent(),

                "README.md" to """
# $projectName

Created with **DevHub – Mobile Developer IDE**.

### Features
- Responsive HTML5 & CSS3 layout
- Live interactive JavaScript logic
- In-app WebView and browser preview
- Real-time JavaScript console output
                """.trimIndent()
            )

            TEMPLATE_PYTHON -> mapOf(
                "main.py" to """
# DevHub Mobile Python Environment
# Project: $projectName

import math

def calculate_stats(numbers):
    total = sum(numbers)
    avg = total / len(numbers)
    variance = sum((x - avg) ** 2 for x in numbers) / len(numbers)
    std_dev = math.sqrt(variance)
    return {
        "count": len(numbers),
        "sum": total,
        "average": round(avg, 2),
        "min": min(numbers),
        "max": max(numbers),
        "std_dev": round(std_dev, 2)
    }

def fibonacci(n):
    fib = [0, 1]
    for i in range(2, n):
        fib.append(fib[-1] + fib[-2])
    return fib

def main():
    print("=" * 45)
    print(" DevHub Python 3.12 Engine Initialized")
    print("=" * 45)

    data = [12, 45, 67, 89, 23, 56, 78, 90, 34, 51]
    print(f"Dataset: {data}")
    stats = calculate_stats(data)

    print("\n--- Statistical Analysis ---")
    for key, value in stats.items():
        print(f"  {key.capitalize()}: {value}")

    print("\n--- Fibonacci Sequence (10 terms) ---")
    print(f"  {fibonacci(10)}")

    print("\n Execution completed successfully!")

if __name__ == "__main__":
    main()
                """.trimIndent(),

                "utils.py" to """
# Helper utilities for $projectName

def format_currency(amount):
    return f"${'$'}{amount:,.2f}"

def is_prime(n):
    if n < 2:
        return False
    for i in range(2, int(n ** 0.5) + 1):
        if n % i == 0:
            return False
    return True
                """.trimIndent(),

                "README.md" to """
# $projectName

A standalone Python project running on Android via DevHub.

### Running
Open `main.py` in the Code Editor and click the **Run Python** button, or run `python main.py` in the built-in Terminal.
                """.trimIndent()
            )

            TEMPLATE_KOTLIN_ANDROID -> mapOf(
                "AndroidManifest.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.devhub.$projectName">

    <application
        android:allowBackup="true"
        android:label="$projectName"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.Dark">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
                """.trimIndent(),

                "MainActivity.kt" to """
package com.devhub.$projectName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hello from $projectName!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Created with DevHub Mobile IDE",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
                """.trimIndent(),

                "build.gradle.kts" to """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.devhub.$projectName"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devhub.$projectName"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}
                """.trimIndent(),

                "res/layout/activity_main.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:id="@+id/titleText"
        android:layout_width="wrap_wrap"
        android:layout_height="wrap_content"
        android:text="$projectName"
        android:textSize="24sp"
        android:textStyle="bold" />

</LinearLayout>
                """.trimIndent()
            )

            TEMPLATE_JAVA -> mapOf(
                "Main.java" to """
public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" DevHub Java Development Environment ");
        System.out.println(" Project: $projectName");
        System.out.println("========================================");

        Calculator calc = new Calculator();
        int a = 15;
        int b = 25;
        System.out.println("Addition: " + a + " + " + b + " = " + calc.add(a, b));
        System.out.println("Multiplication: " + a + " * " + b + " = " + calc.multiply(a, b));
        System.out.println("Factorial of 5: " + calc.factorial(5));

        System.out.println("\nExecution finished with exit code 0.");
    }
}
                """.trimIndent(),

                "Calculator.java" to """
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}
                """.trimIndent()
            )

            TEMPLATE_MARKDOWN -> mapOf(
                "README.md" to """
# $projectName

Welcome to your developer documentation notebook!

## Overview
- Project created: Today
- Engine: DevHub IDE for Android

## Architecture Notes
Write down project roadmaps, algorithms, equations, and code snippets here.

```kotlin
fun main() {
    println("Code blocks are fully formatted in DevHub!")
}
```

## Task Checklist
- [x] Initial project setup
- [ ] Implement core functions
- [ ] Add unit tests
- [ ] Publish repository
                """.trimIndent(),

                "DOCUMENTATION.md" to """
# Technical Specifications

### Endpoints & APIs
Detailed API notes and data schemas.
                """.trimIndent()
            )

            else -> mapOf(
                "README.md" to "# $projectName\n\nEmpty project created in DevHub."
            )
        }
    }
}
