pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "lmspilot-backend"
include(":platform-contracts", ":service-support")
include(":services:api-gateway")
include(":services:identity-service")
include(":services:organization-service")
include(":services:course-service")
include(":services:enrollment-service")
include(":services:learning-service")
include(":services:assessment-service")
include(":services:grading-service")
include(":services:reporting-service")
include(":services:file-storage-service")
include(":services:license-service")
include(":services:audit-service")
include(":services:notification-service")
include(":services:certificate-service")
include(":services:ai-service")
include(":services:configuration-service")
include(":services:integration-service")
include(":services:operations-service")
include(":services:competency-service")
