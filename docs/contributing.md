# Contributing Guide

Thank you for your interest in contributing to Kruize Optimizer! This guide will help you get started with contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Testing Guidelines](#testing-guidelines)
- [Code Style Guidelines](#code-style-guidelines)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. We expect all contributors to:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

Before you begin, ensure you have:

- **Java 25+** installed
- **Maven 3.6.0+** installed
- **Git** for version control
- **Docker** (optional, for containerized testing)
- A **GitHub account**

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:

```bash
git clone https://github.com/YOUR_USERNAME/kruize-optimizer.git
cd kruize-optimizer
```

3. Add the upstream repository:

```bash
git remote add upstream https://github.com/kruize/kruize-optimizer.git
```

4. Verify remotes:

```bash
git remote -v
```

## Development Setup

### Build the Project

```bash
# Clean and build
./mvnw clean package

# Skip tests for faster build
./mvnw clean package -DskipTests
```

### Run in Development Mode

```bash
# Start with hot reload
./mvnw quarkus:dev
```

The application will start on `http://localhost:8080` with:
- Hot reload enabled
- Dev UI available at `http://localhost:8080/q/dev`
- Continuous testing mode

## Important Notes

### Kruize Dependency

**IMPORTANT**: Kruize Optimizer requires Kruize to be already running. See the [Installation Guide - Important Notes](installation.md#important-notes) for detailed information about Kruize dependencies and installation methods.

### IDE Setup

#### IntelliJ IDEA

1. Open the project as a Maven project
2. Enable annotation processing:
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"
3. Install Quarkus plugin (optional but recommended)

#### VS Code

1. Install extensions:
   - Extension Pack for Java
   - Quarkus Tools
2. Open the project folder
3. Maven will auto-import dependencies

#### Eclipse

1. Import as Maven project
2. Install Quarkus plugin from Eclipse Marketplace
3. Enable annotation processing in project properties

## Development Workflow

### 1. Create a Feature Branch

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes

- Write clean, maintainable code
- Add JavaDoc if required
- Follow the project's code style
- Add tests for new functionality
- Update documentation as needed

### 3. Test Your Changes

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=DatasourceResourceTest

# Run tests with coverage
./mvnw verify
```

### 4. Commit Your Changes

```bash
# Stage your changes
git add .

# Commit with a descriptive message
git commit -m "<commit-message>"
```

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create a Pull Request

1. Go to your fork on GitHub
2. Click "New Pull Request"
3. Select your feature branch
4. Fill in the PR template
5. Submit the pull request

## Code Style Guidelines

### Java Code Style

Follow standard Java conventions:

```java
// Class names: PascalCase
public class DatasourceService {
    
    // Constants: UPPER_SNAKE_CASE
    private static final String DEFAULT_TYPE = "prometheus";
    
    // Variables and methods: camelCase
    private String datasourceName;
    
    public void addDatasource(Datasource datasource) {
        // Method implementation
    }
}
```

### Code Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Braces**: Opening brace on same line
- **Imports**: Organize and remove unused imports

### Documentation

- Add JavaDoc for public classes and methods
- Include parameter descriptions
- Document return values and exceptions

```java
/**
 * Service for managing datasources.
 * 
 * @author Your Name
 */
public class DatasourceService {
    
    /**
     * Adds a new datasource to the system.
     * 
     * @param datasource the datasource to add
     * @return the added datasource with generated ID
     * @throws DatasourceException if datasource already exists
     */
    public Datasource addDatasource(Datasource datasource) {
        // Implementation
    }
}
```

## Pull Request Process

### Before Submitting

1. **Update your branch** with latest upstream changes:

```bash
git fetch upstream
git rebase upstream/main
```

2. **Run all tests**:

```bash
./mvnw clean verify
```

3. **Update documentation** if needed

### PR Template

When creating a PR, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass locally
```

### Review Process

1. **Automated Checks**: CI/CD will run tests and checks
2. **Code Review**: Maintainers will review your code
3. **Address Feedback**: Make requested changes
4. **Approval**: Once approved, your PR will be merged

### After Merge

1. **Delete your branch**:

```bash
git branch -d feature/your-feature-name
git push origin --delete feature/your-feature-name
```

2. **Update your local main**:

```bash
git checkout main
git pull upstream main
```

## Documentation

### Types of Documentation

1. **Code Documentation**: JavaDoc comments
2. **API Documentation**: Update [`optimizerAPI.md`](optimizerAPI.md)
3. **User Documentation**: Update relevant docs in `docs/`
4. **README**: Update main README if needed

### Documentation Guidelines

- Keep documentation up-to-date with code changes
- Use clear, concise language
- Include examples where helpful
- Update table of contents when adding sections

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Pull Requests**: Code contributions and reviews

### Getting Help

- Check existing issues and documentation
- Ask questions in GitHub Discussions
- Reach out to maintainers if needed


## Additional Resources

- [Installation Guide](installation.md) - Setup instructions
- [Build Guide](build.md) - Building and pushing Docker images
- [Design Documentation](design.md) - Architecture overview
- [API Reference](optimizerAPI.md) - API documentation
- [Configuration Guide](configurables.md) - Configuration options
- [Test Documentation](../src/test/README.md) - Testing details

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Thank you for contributing to Kruize Optimizer!**

**Last Updated**: 2026-05-05