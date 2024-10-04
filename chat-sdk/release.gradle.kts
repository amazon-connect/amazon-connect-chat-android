import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

apply(plugin = "maven-publish")
apply(plugin = "signing")

// Load SDK-related info from version.properties
val versionProperties = Properties().apply {
    load(file("$rootDir/chat-sdk/version.properties").inputStream())
}

// Load secrets from local.properties
val localProperties = Properties().apply {
    load(file("$rootDir/local.properties").inputStream())
}

val _sdkVersion = versionProperties.getProperty("sdkVersion")
val _groupId = versionProperties.getProperty("groupId")
val _artifactId = versionProperties.getProperty("artifactId")

val _signingKeyRingFile = localProperties.getProperty("signing.secretKeyRingFile")
val _signingPassword = localProperties.getProperty("signing.password")
val _keyId = localProperties.getProperty("signing.keyId")
val sonatypeUsername = localProperties.getProperty("SONATYPE_TOKEN_USERNAME")
val sonatypePassword = localProperties.getProperty("SONATYPE_TOKEN_PASSWORD")


// Helper function to configure POM metadata
fun MavenPublication.configurePom() {
    pom {
        name.set(findProperty("pomName") as String)
        description.set(findProperty("pomDescription") as String)
        url.set(findProperty("pomUrl") as String)

        licenses {
            license {
                name.set(findProperty("pomLicenseName") as String)
                url.set(findProperty("pomLicenseUrl") as String)
                distribution.set(findProperty("pomLicenseDist") as String)
            }
        }

        developers {
            developer {
                id.set(findProperty("pomDeveloperId") as String)
                organization.set(findProperty("pomDeveloperOrg") as String)
                organizationUrl.set(findProperty("pomDeveloperOrgUrl") as String)
            }
        }

        scm {
            connection.set(findProperty("pomScmConnection") as String)
            developerConnection.set(findProperty("pomScmDevConnection") as String)
            url.set(findProperty("pomScmUrl") as String)
        }
    }
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("publishChatSDK") {
            groupId = _groupId
            artifactId = _artifactId
            version = _sdkVersion

            // Ensure the AAR artifact is included
            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))
            }

            // Configure POM metadata using the helper function
            configurePom()
        }
    }

    // Repository configuration for Sonatype
    repositories {
        maven {
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
            val releasesRepoUrl = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
            url = if (_sdkVersion.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

configure<SigningExtension> {
    // Load signing details from local.properties
    val signingKey = _signingKeyRingFile
    val signingPassword = _signingPassword
    val keyId = _keyId

    // Ensure the required properties are available and use in-memory keys
    if (signingKey != null && signingPassword != null && keyId != null) {
        val privateKey = file(signingKey).readText()
        useInMemoryPgpKeys(keyId, privateKey, signingPassword)
    } else {
        logger.error("Missing GPG Signing Properties.")
    }

    // Sign the Maven publication
    sign(extensions.getByType(PublishingExtension::class).publications["publishChatSDK"])
}