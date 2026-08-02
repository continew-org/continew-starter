# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

`CLAUDE.md` and `AGENTS.md` are mirror files. Whenever either file changes, apply the identical change to the other file and verify that their contents remain byte-for-byte identical（标题文件名除外）.

## Project Overview

ContiNew Starter is a Java 17, Spring Boot 3.5 multi-module Maven library. It publishes reusable starters and dependency-management POMs for Spring Boot web applications; it is not itself a runnable application.

There is no Maven wrapper, so commands below require `mvn` and must be run from the repository root.

## Build and Verification

```bash
# Compile the complete reactor (the same lifecycle used by build CI)
mvn compile

# Run the full verification lifecycle; tests run here if present
mvn verify

# Build one module and its reactor dependencies
mvn -pl :continew-starter-core -am compile

# Check formatting without changing files
mvn spotless:check

# Apply formatting and the project license header
mvn spotless:apply
```

`spotless:apply` is bound to Maven's `compile` phase. Therefore `compile`, `test`, and `verify` may rewrite Java files by removing unused imports, applying the Eclipse P3C formatter, and inserting `.style/license-header`. Inspect the working tree after running Maven.

The repository currently has no `src/test` tree or test dependencies. If tests are introduced using standard Maven conventions, run a class or method in its owning module with:

```bash
mvn -pl :<artifact-id> -Dtest=ClassName test
mvn -pl :<artifact-id> -Dtest=ClassName#methodName test
```

If the test needs uninstalled reactor dependencies, add `-am`; in that case `-Dsurefire.failIfNoSpecifiedTests=false` prevents upstream modules with no matching test from failing the build.

CI uses JDK 17. Pull requests to `dev` run `mvn -B compile`; Sonar CI runs `mvn -B verify -Psonar`. The `release` profile adds source/Javadoc artifacts, GPG signing, and Sonatype Central publishing, but the repository does not document an authoritative release command or credential setup.

## Architecture

### Maven and module layers

The effective dependency direction is:

```text
continew-starter-dependencies / continew-starter-bom
    -> continew-starter-core
    -> foundational feature starters
    -> vendor/framework implementation variants
    -> continew-starter-extension modules
```

- The root `pom.xml` is the reactor aggregator and parent for feature-family POMs. Most family directories contain a packaging-only `pom` whose dependencies are inherited by publishable child JARs.
- `continew-starter-dependencies` is the full platform parent/BOM: it pins Spring Boot, Spring Cloud, third-party libraries, Maven plugins, and imports the internal BOM.
- `continew-starter-bom` manages only artifacts under `top.continew.starter`. It is intended for consumers that already manage external dependency versions.
- All modules share `${revision}`. The Flatten Maven plugin rewrites publication POMs during `process-resources`; generated flattened POMs are removed by `clean`.
- `continew-starter-core` is shared infrastructure rather than a dependency-light domain module. It already brings in Spring Boot, AOP, Spring Web/MVC, Servlet APIs, and Hutool.
- `continew-starter-extension` is a higher-level layer over core/web/data/API-doc/Excel capabilities, not another foundational starter family.

### Starter activation pattern

Implementation JARs register Spring Boot 3 auto-configuration in:

```text
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

A typical starter combines `@AutoConfiguration`, typed configuration properties, class/property/bean conditions, and optional `default-*.yml` resources loaded through core's `GeneralPropertySourceFactory`. Default handlers and strategies normally use `@ConditionalOnMissingBean` so applications can replace them.

When adding a starter, place shared contracts in a core/shared module and vendor-specific integration in a leaf JAR. Register only the leaf auto-configuration and gate it with appropriate conditions. Some `custom` configuration modes deliberately create a failing fallback when the required consumer SPI bean is absent; treat these as explicit implementation contracts, not as a way to disable defaults.

Legacy `META-INF/spring.factories` is used in core only for the startup version-log `ApplicationListener`, not for normal auto-configuration discovery.

### Mutually exclusive variants

Several variants intentionally expose identical fully qualified class names and must not be placed on the same consumer classpath:

- `continew-starter-data-mp` and `continew-starter-data-mf` are alternative ORM implementations over `data-core`; their matching CRUD variants are also alternatives.
- `continew-starter-excel-fastexcel` and `continew-starter-excel-poi` both publish `top.continew.starter.excel.util.ExcelUtils`. CRUD core explicitly depends on FastExcel.
- `continew-starter-log-aop` and `continew-starter-log-interceptor` share auto-configuration/bean roles and are alternative logging implementations.

Cache modules are layered differently: Redisson is the foundation, while Spring Cache and JetCache build on the Redisson starter. Sa-Token (application authentication/authorization) and JustAuth (third-party OAuth login) are complementary rather than alternatives.

### Extension integration

- CRUD core is intentionally opt-in through `@EnableCrudApi`; it does not register itself through Boot auto-configuration imports. It defines controller/service contracts, while MP and MF leaves provide persistence implementations and lifecycle hooks.
- MyBatis-Plus auto-configuration collects all `InnerInterceptor` beans. Tenant and data-permission extensions integrate through this seam without introducing reverse dependencies into `data-mp`.
- The MP data-permission extension requires a consumer-provided `DataPermissionUserDataProvider`; tenant support similarly requires a `TenantProvider`. Missing required providers intentionally fail startup.
- MyBatis-Flex has a separate data-permission mechanism inside `data-mf`; do not confuse it with the MP-only extension module.

## Source Conventions

- Package root is `top.continew.starter`; follow the neighboring module's package structure and naming.
- Java formatting is defined by `.style/p3c-codestyle.xml` and follows the Alibaba Java Development Manual. Spotless also removes unused imports and applies the LGPL header.
- Use the established Angular/Conventional Commit style, typically `type(scope): description`, when asked to create commits.
- Feature work and optimizations target `dev`; maintenance branches named `x.x.x` accept fixes only.

## Agent skills

### Issue tracker

Issues for this repo live as GitHub issues, managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles map to label strings of the same name. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` at the repo root plus `docs/adr/`. See `docs/agents/domain.md`.
