# Contributing to Kruize Optimizer

Thank you for your interest in Kruize Optimizer! We welcome your contributions to the project.

## Contribution Workflow

These steps outline the general contribution workflow:

- Fork and set up the Kruize Optimizer repository on your workstation
- Create a topic branch from where you want to base your work (usually main)
- Install the required software to build and deploy (see [Installation Guide](installation.md))
- Make sure the tests pass, add any new tests if appropriate
- Make commits of logical units
- Make sure your commit messages are in the proper format (see [below](#commit-message))
- Push your changes to a topic branch in your fork of the repository
- Submit a pull request to the original repository
- Ensure the PR checks pass

### Fork and Clone

```bash
git clone https://github.com/YOUR_USERNAME/kruize-optimizer.git
cd kruize-optimizer
git remote add upstream https://github.com/kruize/kruize-optimizer.git
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify
```

## Commit Message

We use GPG keys for signing the commits. Refer [generating a new GPG key](https://docs.github.com/en/authentication/managing-commit-signature-verification/generating-a-new-gpg-key) for details. The commit message should indicate what has changed and why the change was made. Sign off on your commit in the footer. By doing this, you assert original authorship of the commit and that you are permitted to contribute it. This can be automatically added to your commit by passing `-S` to `git commit`, or by manually adding the following line to the footer of the commit.

```
Signed-off-by: Full Name <email>
```

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Previous**: [Build Guide](build.md) | [Back to docs](README.md)

**Last Updated**: 2026-11-04