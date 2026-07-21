# Hotel Management System

A Java-based Data Structures and Algorithms (DSA) project.

---

## 🛠 Prerequisites & Environment Setup

Before running the application, your local environment must be configured with the correct compiler and build paths.

### 1. Install Java Development Kit (JDK) 26

- Download and install JDK 26 from the official Oracle portal: [Oracle Java Downloads](https://www.oracle.com/asean/java/technologies/downloads/#jdk26-windows).
- Ensure your system's `JAVA_HOME` environment variable points to your JDK 26 installation folder.

### 2. Configure Apache Ant Path

You do not need to download Apache Ant separately if you already have NetBeans installed on your machine. You can directly expose NetBeans' bundled Ant binary to your system:

- Open your system **Environment Variables** configuration.
- Edit the global `Path` variable and append the following directory path:
  ```text
  C:\Program Files\Apache NetBeans\extide\ant\bin
  ```
- Alternatively, if you do not use NetBeans, download the official binary distribution from [Apache Ant Downloads](https://ant.apache.org/bindownload.cgi) and add its corresponding `bin` extraction path to your system variables.
- Verify the installation by opening a new terminal panel and typing `ant -version`.

---

## 🚀 Getting Started & Execution Scripts

To accommodate different development workflows, this repository contains utility scripts in the root directory:

### Execution & Compilation

- **`br.bat` (Recommended Shortcut):** The fastest option for development. Typing `.\br` in your VS Code terminal automatically runs a chained lifecycle execution block (`call ant jar && java -jar ...`). It compiles your changes via Ant and boots the application immediately if the build succeeds. If compilation fails, it terminates instantly to show errors.
- **`build.bat`:** Run this script to compile your Java source files, handle project dependency generation, and package your distribution assets natively using Apache Ant (`ant jar`).
- **`run.bat`:** Run this script to execute the pre-compiled application package directly using the native Java launcher (`java -jar "dist\HotelManagementSystem.jar"`).

### Code Quality Utilities

- **`ci.bat`:** Run this script to execute a local dry-run verification of the project's code quality before pushing. It verifies your local Java installation version, checks for Google Java Format style compliance, and runs an Ant build test.
- **`format.bat`:** Run this script to automatically format and overwrite all of your `.java` source files in the `src` directory so that they perfectly align with the strict Google Java Formatting style.

---

## 📦 Project Structure

```text
├── .github/workflows/   # Automated CI Pipeline (Targets 'main' branch on PRs)
├── .vscode/             # Local IDE formatters, settings, and launch configurations
├── src/                 # Main Java Source Code
│   ├── adt/             # Abstract Data Types implementations
│   ├── control/         # Controller/Core Application Logic
│   ├── entity/          # Business Model / Entity classes
│   ├── util/            # Helper utilities
│   └── view/            # UI components / Boundary classes
├── br.bat               # Fast Build & Run shortcut script
├── build.bat            # Isolated project compilation script
├── run.bat              # Isolated application runner script
├── ci.bat               # Local CI verification script
├── format.bat           # Auto-formatting tool script
└── build.xml            # Apache Ant build script configurations
```

---

## 🧪 CI Code Quality Pipeline

This repository runs a mandatory automated **GitHub Actions CI (Continuous Integration)** pipeline on every Pull Request targeting the **`main`** branch (specifically triggering when a PR is `opened`, `synchronize`d with new commits, or `reopened`).

_Note: This repository implements **CI only** (automated verification, code quality checks, and compilation integrity). It does not contain CD (Continuous Deployment) logic since there is no production server environment deployment required for this application._

The automated workflow executes the following validations before allowing a branch merge:

1.  **Style Guide Audit:** Enforces compliance by auditing modified files against the standard **Google Java Formatting Style**.
2.  **Compilation Check:** Boots up a clean virtual machine targeting **JDK 26**, configures dependencies via fast-caching layers, and runs `ant jar` to verify compilation integrity.

---

## 🏷 Git Collaboration Standards

To maintain a clean, professional git workflow, all team members must strictly follow these naming conventions when checking out branches and creating pull requests:

### 1. Branch Naming Conventions (Use `/`)

Always name your local git branch using lowercase characters, separating the type from the description with a forward slash (`/`):

- `feat/` — Adding a brand new feature or module (e.g., `feat/reservation-history`)
- `fix/` — Fixing a bug or runtime problem (e.g., `fix/array-bounds-exception`)
- `docs/` — Modifying documentation or the README file (e.g., `docs/update-environment-paths`)
- `refactor/` — Reworking existing code logic without changing behavior (e.g., `refactor/optimize-binary-tree`)
- `chore/` — Routine maintenance, updating configurations, or file cleanups (e.g., `chore/hide-build-folders`)

### 2. Pull Request (PR) Title Conventions (Use `:`)

Your PR title must use a colon (`:`) after the type identifier so the team instantly understands the purpose of your merge request:

- `feat: <short description>` (e.g., `feat: implement double linked list for history`)
- `fix: <short description>` (e.g., `fix: resolve null pointer during checkout`)
- `docs: <short description>` (e.g., `docs: document installation guide for netbeans ant`)
- `refactor: <short description>` (e.g., `refactor: clean up switch cases in room view`)
- `chore: <short description>` (e.g., `chore: hide build and dist directories from explorer`)
