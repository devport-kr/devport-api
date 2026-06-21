pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "devport-api"

// Shared / cross-cutting modules
include(":support:cache")
include(":support:security")
include(":support:exception")
include(":support:logging")

// gitrepo domain — reference Hexagonal slice (model -> infrastructure -> service -> repository-jpa -> api)
include(":gitrepo:model")
include(":gitrepo:infrastructure")
include(":gitrepo:service")
include(":gitrepo:repository-jpa")
include(":gitrepo:api")

// llm domain
include(":llm:model")
include(":llm:infrastructure")
include(":llm:repository-jpa")
include(":llm:service")
include(":llm:api")

// auth domain
include(":auth:model")
include(":auth:infrastructure")
include(":auth:repository-jpa")
include(":auth:adapter-redis")
include(":auth:adapter-http")
include(":auth:service")
include(":auth:api")

// article domain
include(":article:model")
include(":article:repository-jpa")
include(":article:service")
include(":article:api")

// mypage domain (mutually referential with article: mypage -> article model/repo, article:api -> mypage:service)
include(":mypage:model")
include(":mypage:repository-jpa")
include(":mypage:service")
include(":mypage:api")

// port domain
include(":port:model")
include(":port:repository-jpa")
include(":port:service")
include(":port:api")

// wiki domain (large RAG/OpenAI Java retained inside modules)
include(":wiki:model")
include(":wiki:repository-jpa")
include(":wiki:service")
include(":wiki:api")

// Composition root: bootable Spring Boot app (JVM)
include(":application-api")
