package org.grnet.pidmr.service.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The KeycloakAdminService class provides methods to connect and interact with the Keycloak admin API.
 */
@ApplicationScoped
public class KeycloakAdminService {

    private static final Logger LOG = Logger.getLogger(KeycloakAdminService.class);

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    /**
     * Injection point for the Keycloak admin client
     */
    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    @ConfigProperty(name = "api.keycloak.user.id")
    String attribute;

    /**
     * This method retrieves all the available roles for a specific realm and client ID.
     *
     * @return A list of Role objects representing the available roles.
     */
    public List<KeycloakRole> fetchRoles() {

        var realmResource = keycloak.realm(realm);

        var clientRepresentation = realmResource.clients().findByClientId(clientId).stream().findFirst().get();

        var clientResource = realmResource.clients().get(clientRepresentation.getId());

        var roleRepresentations = clientResource.roles().list();

        return roleRepresentations
                .stream()
                .filter(roleRepresentation -> !roleRepresentation.getName().equals("uma_protection"))
                .map(roleRepresentation -> new KeycloakRole(roleRepresentation.getId(), roleRepresentation.getName(), roleRepresentation.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Assigns roles to a user.
     *
     * @param userId The unique identifier of the user. to assign roles to.
     * @param roles  The roles to be assigned to the user.
     */
    public void assignRoles(String userId, List<String> roles) {

        try {

            var realmResource = keycloak.realm(realm);

            var clientRepresentation = realmResource.clients().findByClientId(clientId).stream().findFirst().get();

            var clientResource = realmResource.clients().get(clientRepresentation.getId());

            var usersResource = realmResource.users();

            var userRepresentation = realmResource.users().searchByAttributes(String.format("%s:%s", attribute, userId)).stream().findFirst().get();

            var userResource = usersResource.get(userRepresentation.getId());

            // Get client level roles
            var rolesRepresentations = roles
                    .stream()
                    .map(role -> clientResource.roles().get(role).toRepresentation())
                    .collect(Collectors.toList());

            // Assign client level role to user
            userResource.roles().clientLevel(clientRepresentation.getId()).add(rolesRepresentations);

        } catch (Exception e) {

            LOG.error("A communication error occurred while assigning roles to the user.", e);
            throw new RuntimeException("A communication error occurred while assigning roles to the user.");
        }
    }

    public void removeRoles(String userId, List<String> roles) {

        try {

            var realmResource = keycloak.realm(realm);

            var clientRepresentation = realmResource.clients().findByClientId(clientId).stream().findFirst().get();

            var clientResource = realmResource.clients().get(clientRepresentation.getId());

            var usersResource = realmResource.users();

            var userRepresentation = realmResource.users().searchByAttributes(String.format("%s:%s", attribute, userId)).stream().findFirst().get();

            var userResource = usersResource.get(userRepresentation.getId());

            // Get client level roles
            var rolesRepresentations = roles
                    .stream()
                    .map(role -> clientResource.roles().get(role).toRepresentation())
                    .collect(Collectors.toList());

            // Remove client level role from user
            userResource.roles().clientLevel(clientRepresentation.getId()).remove(rolesRepresentations);

        } catch (Exception e) {

            LOG.error("A communication error occurred while removing roles from the user.", e);
            throw new RuntimeException("A communication error occurred while removing roles from the user.");
        }
    }

    /**
     * Checks if a role exists by searching for the role with the given name.
     *
     * @param names List of role names to search for
     * @throws NotFoundException if the role with the specified name is not found.
     */
    public void doRolesExist(List<String> names) {

        var roles = fetchRoles().stream().map(KeycloakRole::getName).collect(Collectors.toList());

        var notExist = names
                .stream()
                .filter(name -> !roles.contains(name))
                .collect(Collectors.toList());

        if (!notExist.isEmpty()) {
            throw new NotFoundException(String.format("The following roles %s do not exist.", notExist));
        }
    }

    public List<UserRepresentation> fetchRolesMembers(String role) {

        var realmResource = keycloak.realm(realm);
        var clientRepresentation = realmResource.clients().findByClientId(clientId).stream().findFirst().get();

        var clientResource = realmResource.clients().get(clientRepresentation.getId());

        return new ArrayList<>(clientResource.roles().get(role).getRoleUserMembers());
    }
}
