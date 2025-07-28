# 🔐 Shamir's Secret Sharing Recovery (Java)

This project implements a Java-based decoder for Shamir's Secret Sharing scheme using Gaussian elimination over rational numbers (`BigInteger` fractions). It processes test cases provided in JSON files and reconstructs the original secret, even in the presence of inconsistent shares.

---

## 📜 Features

- 🔍 Reads JSON-formatted test cases with share data.
- ➗ Uses exact arithmetic with rational numbers for precision.
- ⚙️ Implements Vandermonde matrix & Gaussian elimination.
- ✅ Identifies **valid** and **invalid** shares based on majority consensus.
- 📁 Supports selecting multiple JSON files via a file chooser dialog.

---

## 🏁 Getting Started

### Prerequisites

- Java 8 or later

### Compile

```bash
javac ShamirsSecretAlgo.java
Run
bash
Copy
Edit
java ShamirsSecretAlgo
A file chooser will appear, prompting you to select one or more .json files with test cases.

📂 JSON Test Case Format
json
Copy
Edit
{
  "keys": {
    "n": 6,
    "k": 3
  },
  "1": { "base": "10", "value": "109" },
  "2": { "base": "10", "value": "155" },
  "3": { "base": "10", "value": "211" }
}
