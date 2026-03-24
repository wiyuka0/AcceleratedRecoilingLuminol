rootProject.name = "AcceleratedRecoilingLuminol"
pluginManagement {
    repositories {
//        maven("https://repo.leavesmc.org/releases") {
//            name = "leavesmc-releases"
//        }
//        maven("https://repo.leavesmc.org/snapshots") {
//            name = "leavesmc-snapshots"
//        }

        maven("https://repo.spongepowered.org/repository/maven-public/")
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "luminol-snapshots"
            url = uri("https://repo.menthamc.org/snapshots/")
        }
        maven {
            name = "luminol-releases"
            url = uri("https://repo.menthamc.org/releases/")
        }
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            url = uri("https://repo.menthamc.org/repository/maven-public/")
        }


    }
}