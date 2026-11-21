# Continuous integration

[Gestiora CI](../.github/workflows/ci.yml) runs on pushes, pull requests, and manual dispatches. A newer run cancels an older run for the same event and ref. Both jobs use the checked-in Gradle wrapper, Azul Zulu JDK 21 (matching the Gradle daemon criteria), and the Gradle User Home cache from `gradle/actions/setup-gradle`. The workflow uses standard GitHub-hosted runners and read-only repository access.

## What runs

| Job | Runner | Checks |
| --- | --- | --- |
| Android build and tests | `ubuntu-24.04` | Builds the `internalDebug` APK and the unsigned, R8-enabled `productionRelease` bundle. Runs the app's `internalDebug` unit tests and the shared modules' Android host tests. Starts an API 36 emulator to run the Room and Keystore instrumentation suite in `:shared:data:datasources-core`. The project still compiles against Android API 37. |
| iOS Simulator build | `macos-26` (Apple Silicon) | Selects Xcode 26.4.1. Links the static `Shared.framework` for `iosSimulatorArm64`, then builds the Swift host and its resources with `xcodebuild` and the shared `iosApp` scheme. The Xcode build phase still uses Gradle's `embedAndSignAppleFrameworkForXcode` direct integration. No CocoaPods setup is needed. |

The iOS job uses a generic simulator destination and disables signing for that invocation. It requires neither an Apple account nor provisioning profiles. It does not boot an iOS simulator, launch the app, or test Keychain at runtime. The framework and `.app` target an arm64 simulator. They are not an iPhone release build, an IPA, or an App Store archive. TestFlight and App Store distribution require a separately signed device archive and Apple Developer access.

The iOS build is pending until both **Link the Kotlin iOS Simulator framework** and **Build the iOS app without signing** pass on GitHub. A clean local checkout or YAML validation alone does not establish build compatibility. When investigating failures, start with the failed step. A framework-link failure points to the Gradle/Kotlin stage, while a Swift, linker, asset, or Xcode configuration failure is reported by the app build step.

## First run and manual runs

1. If this is a private repository, check the owner's **Billing & licensing** usage and spending controls before the first push. Standard runners are free for public repos. GitHub Free currently includes USD 12 of monthly compute for eligible private usage. Standard macOS uses USD 0.062 per minute of that allowance, about 193 minutes if nothing else consumes it. A full 35-minute iOS job would use about USD 2.17 of compute credit. Android and other eligible workloads share the allowance. The timeout limits a single job, and GitHub billing settings control additional spend. [Included usage](https://docs.github.com/en/billing/reference/product-usage-included) · [Runner pricing](https://docs.github.com/en/billing/reference/actions-runner-pricing) · [Spending controls](https://docs.github.com/en/billing/how-tos/set-up-budgets)
2. Verify that GitHub Actions is enabled and the repository policy allows the actions listed in the workflow. Commit and push the CI files on the current branch when ready. The push triggers the first run even if the workflow has not reached the default branch. Open **Actions → Gestiora CI**, then select the run for that commit.
3. Once the workflow file exists on the default branch, use **Actions → Gestiora CI → Run workflow**, select the branch, and start the run. Write access is required. For a workflow that is still exclusive to a feature branch, use its push or pull request run and **Re-run jobs** when needed. [GitHub manual dispatch](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)

## Download artifacts

Open the run's **Summary → Artifacts** and select the artifact to download. Read access to the repo is required. [GitHub artifact downloads](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)

| Artifact | Contents | Retention |
| --- | --- | --- |
| `android-builds` | Debug APK and unsigned production AAB, if the Gradle build and host-test step succeeds | 3 days |
| `android-test-reports` | Available host and instrumented test reports/results | 7 days |
| `ios-simulator-builds` | Tarballs of `Shared.framework` and `Gestiora.app` as produced. If Xcode fails after the framework links, the framework can still be downloaded. | 3 days |
| `ios-build-diagnostics` | Available Kotlin/Native and Xcode logs and the Xcode result bundle | 7 days |

The iOS tarballs preserve file permissions and symlinks. Extract them on macOS with `tar -xzf <file>.tar.gz`. Open `Build.xcresult` with Xcode if it was generated. If a job fails before producing diagnostics, use its step logs in GitHub Actions.
