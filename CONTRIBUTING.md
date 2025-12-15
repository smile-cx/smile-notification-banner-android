# Contributing to SmileNotificationBanner

Thank you for considering contributing to SmileNotificationBanner! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

If you find a bug, please open an issue on GitHub with:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Screenshots (if applicable)
- Device information and Android version
- Library version

### Suggesting Features

Feature suggestions are welcome! Please open an issue with:
- A clear description of the feature
- Use cases for the feature
- Any examples from other libraries (if applicable)

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test your changes thoroughly
5. Commit your changes (`git commit -m 'Add some amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Write tests for new features

### Testing

Before submitting a PR:
- Build the project successfully
- Test on multiple Android versions (if possible)
- Test on different screen sizes
- Ensure no regressions in existing features

### Documentation

- Update README.md if you add new features
- Add KDoc comments to public APIs
- Update CHANGELOG.md with your changes

### Release Process

When creating a new release:

1. Update the version in `build.gradle.kts` files
2. Update CHANGELOG.md with the new version and changes
3. Commit the changes
4. Create a git tag **without** the "v" prefix:
   ```bash
   # Correct
   git tag -a 1.0.0 -m "Release 1.0.0"

   # Incorrect (do not use "v" prefix)
   git tag -a v1.0.0 -m "Release 1.0.0"
   ```
5. Push the tag: `git push origin 1.0.0`

**Important:** Tags must NOT include the "v" prefix for JitPack compatibility. Use `1.0.0`, not `v1.0.0`.

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for all.

### Our Standards

- Be respectful and inclusive
- Welcome newcomers
- Accept constructive criticism gracefully
- Focus on what is best for the community

## Questions?

Feel free to open an issue for any questions about contributing.

Thank you for helping make SmileNotificationBanner better!
