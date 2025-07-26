# Java Terminal Text Editor ✍️

A minimal terminal-based text editor built in Java using the JLine3 library.

## 📦 Features

- Terminal-based text editing
- Arrow-key cursor movement (left, right, up, down)
- Character insertion, deletion, and line splitting (Enter key)
- ANSI-compatible redraws
- Cross-platform (Linux, macOS, Windows with ANSI support)

---

## 🛠 Prerequisites

- Java 11 or higher
- Maven 3.x installed and configured (`mvn -v`)
  -Make sure you have Maven installed. If not, follow the instructions here:  
  👉 [Install Maven](https://maven.apache.org/install.html)

---

## 🚀 How to Build and Run (MAVEN)

### 1. Clone the Repo

```bash
git clone https://github.com/ArpitAditya45/Java-Text-Editor.git
cd Java-Text-Editor
```

### 2. Use Maven to execute

```bash
mvn compile
mvn exec:java
```

## 🚀 How to Build and Run (DOCKER)

### 1. Pull the Docker Image

```bash
docker pull godslayer45/javatexteditor
```

### 2. Run the Docker Image

```bash
docker run -it godslayer45/javatexteditor
```
