package com.itm.space.backendresources.service;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
public class UserServiceImplIT {

    private Keycloak keycloakClient;

    @Autowired
    UserServiceImpl underTest;

    @Container
    public static GenericContainer<?> keycloak = new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:legacy"))
            .withEnv("KEYCLOAK_USER", "admin")
            .withEnv("KEYCLOAK_PASSWORD", "admin")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/auth/admin/").forStatusCode(200));

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.auth-server-url", () -> "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/auth");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/auth/realms/ITM");
        registry.add("keycloak.realm", () -> "master");
        registry.add("resource", () -> "backend-resources");
        registry.add("keycloak.credentials.secret", () -> "yoiQZKVLvtahMDp06yJG3B3px594oElM");
        registry.add("keycloak.username", () -> "admin");
        registry.add("keycloak.password", () -> "admin");
        registry.add("keycloak.resource", () -> "admin-cli");
    }

    @BeforeEach
    void setup() {
        String keycloakHost = keycloak.getHost();
        int keycloakPort = keycloak.getMappedPort(8080);

        String keycloakUrl = "http://" + keycloakHost + ":" + keycloakPort + "/auth";
        keycloakClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();

    }

    @Test
    void itShouldCreateUser() {

        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("test-user");
        userRequest.setEmail("email@test.com");
        userRequest.setFirstName("userName");
        userRequest.setLastName("userSurname");
        userRequest.setPassword("test-password");

        underTest.createUser(userRequest);

        List<UserRepresentation> users = keycloakClient.realm("master").users().search(userRequest.getUsername());
        UserRepresentation createdUser = users.get(0);

        assertNotNull(createdUser);
        assertEquals(userRequest.getUsername(), createdUser.getUsername());
        assertEquals(userRequest.getEmail(), createdUser.getEmail());
        assertEquals(userRequest.getFirstName(), createdUser.getFirstName());
        assertEquals(userRequest.getLastName(), createdUser.getLastName());
    }

    @Test
    void itShouldGetUserById() throws Exception {

        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("test-user1");
        userRequest.setEmail("email@test.com1");
        userRequest.setFirstName("userName1");
        userRequest.setLastName("userSurname1");
        userRequest.setPassword("test-password1");

        List<UserRepresentation> usersBefore = keycloakClient.realm("master").users().list();

        underTest.createUser(userRequest);

        List<UserRepresentation> users = keycloakClient.realm("master").users().search(userRequest.getUsername());

        UserRepresentation user = users.get(0);
        String userId = user.getId();
        UserResponse userFromDb = underTest.getUserById(UUID.fromString(userId));

        assertEquals(userRequest.getEmail(), userFromDb.getEmail());
        assertEquals(userRequest.getFirstName(), userFromDb.getFirstName());
        assertEquals(userRequest.getLastName(), userFromDb.getLastName());
    }
}