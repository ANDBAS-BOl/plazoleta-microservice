package com.pragma.powerup.plazoleta.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.pragma.powerup.plazoleta";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void domainShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Domain must not depend on infrastructure (hexagonal architecture)")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .because("Domain must not depend on application layer")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnSpringFramework() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain must be free of Spring framework dependencies")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnJavaxPersistence() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("javax.persistence..")
                .because("Domain must not depend on JPA annotations")
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnInfrastructureExceptMapper() {
        noClasses()
                .that().resideInAPackage("..application.handler..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.out..")
                .because("Application handlers must not access infrastructure output adapters directly")
                .check(importedClasses);
    }

    @Test
    void useCasesShouldOnlyDependOnDomainApi() {
        classes()
                .that().resideInAPackage("..domain.usecase..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..",
                        "java..",
                        "lombok.."
                )
                .because("Use cases must only depend on domain types")
                .check(importedClasses);
    }

    @Test
    void controllersShouldNotDependOnDomainUseCases() {
        noClasses()
                .that().resideInAPackage("..infrastructure.input.rest..")
                .should().dependOnClassesThat().resideInAPackage("..domain.usecase..")
                .because("Controllers must delegate through application handlers, not call use cases directly")
                .check(importedClasses);
    }

    @Test
    void controllersShouldNotDependOnJpaEntities() {
        noClasses()
                .that().resideInAPackage("..infrastructure.input.rest..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.out.jpa.entity..")
                .because("Controllers must not reference JPA entities")
                .check(importedClasses);
    }

    @Test
    void noLegacyServicePackageShouldExist() {
        assertTrue(
                importedClasses.that(inPackage(BASE_PACKAGE + ".service")).isEmpty(),
                "Legacy service package should not exist — all logic goes through hexagonal layers");
    }

    @Test
    void noLegacyRepositoryPackageAtRoot() {
        assertTrue(
                importedClasses.that(inPackageStartingWith(BASE_PACKAGE + ".repository")).isEmpty(),
                "Repositories must be in infrastructure.out.jpa.repository, not at package root");
    }

    @Test
    void noLegacyClientPackageAtRoot() {
        assertTrue(
                importedClasses.that(inPackage(BASE_PACKAGE + ".client")).isEmpty(),
                "HTTP clients must be in infrastructure.out.http.client, not at package root");
    }

    @Test
    void noLegacyWebPackageAtRoot() {
        assertTrue(
                importedClasses.that(inPackageStartingWith(BASE_PACKAGE + ".web")).isEmpty(),
                "Controllers must be in infrastructure.input.rest, not in a web package");
    }

    private static DescribedPredicate<JavaClass> inPackage(String packageName) {
        return new DescribedPredicate<>("reside in package " + packageName) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().equals(packageName);
            }
        };
    }

    private static DescribedPredicate<JavaClass> inPackageStartingWith(String prefix) {
        return new DescribedPredicate<>("reside in package starting with " + prefix) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().startsWith(prefix);
            }
        };
    }
}
