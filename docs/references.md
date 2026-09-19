# References and provenance

Official references used when preparing this starter (2026-09-12):

- Spring Boot 3.5 system requirements: `https://docs.spring.io/spring-boot/3.5/system-requirements.html`
- Spring Boot Maven executable packaging: `https://docs.spring.io/spring-boot/maven-plugin/packaging.html`
- Actuator endpoints: `https://docs.spring.io/spring-boot/reference/actuator/endpoints.html`
- Apache Maven Wrapper: `https://maven.apache.org/tools/wrapper/`
- Spring guide's Maven wrapper configuration: `https://raw.githubusercontent.com/spring-guides/gs-spring-boot/main/complete/.mvn/wrapper/maven-wrapper.properties`

## Bundled third-party source

The `mvnw` file is the unmodified Apache Maven Wrapper 3.3.4 script retrieved from
`spring-guides/gs-spring-boot`, path `complete/mvnw`.
Its Git blob SHA is `bd8896bf2217b46faa0291585e01ac1a3441a958`.
The prepared file's Git blob hash was checked against the retrieved source.

The distribution URL is pinned to Apache Maven 3.9.16 over HTTPS, as found in the
Spring guide's wrapper configuration. No independently verified distribution SHA-256
was available in the preparation environment, so checksum pinning is not claimed.
We can add a verified checksum as part of the CI supply-chain hardening phase.

The wrapper's Apache license and notice are included under `licenses/`.
The Java application and project-specific tests/documentation were created for this lab.
