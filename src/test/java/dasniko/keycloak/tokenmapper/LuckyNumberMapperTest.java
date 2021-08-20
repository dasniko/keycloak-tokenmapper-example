package dasniko.keycloak.tokenmapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class LuckyNumberMapperTest {

	@Test
	public void shouldStartKeycloakWithLuckyNumberMapper() throws VerificationException {
		try (KeycloakContainer keycloak = new KeycloakContainer()
			.withExtensionClassesFrom("target/classes")) {
			keycloak.start();

			Keycloak keycloakClient = Keycloak.getInstance(keycloak.getAuthServerUrl(), "master",
				keycloak.getAdminUsername(), keycloak.getAdminPassword(), "admin-cli");

			RealmResource realm = keycloakClient.realm("master");
			ClientRepresentation client = realm.clients().findByClientId("admin-cli").get(0);

			ProtocolMapperRepresentation mapper = configureCustomOidcProtocolMapper();
			realm.clients().get(client.getId()).getProtocolMappers().createMapper(mapper).close();

			keycloakClient.tokenManager().refreshToken();
			AccessTokenResponse tokenResponse = keycloakClient.tokenManager().getAccessToken();

			TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class);
			verifier.parse();

			AccessToken accessToken = verifier.getToken();
			int luckyNumber = (int) accessToken.getOtherClaims().get("myLuckyNumber");
			System.out.printf("Your LuckyNumber is %s%n", luckyNumber);

			assertTrue(luckyNumber >= 11);
			assertTrue(luckyNumber <= 88);
		}
	}

	private ProtocolMapperRepresentation configureCustomOidcProtocolMapper() {
		ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		mapper.setProtocolMapper(LuckyNumberMapper.PROVIDER_ID);
		mapper.setName("lucky-number-mapper");
		Map<String, String> config = new HashMap<>();
		config.put(LuckyNumberMapper.LOWER_BOUND, "11");
		config.put(LuckyNumberMapper.UPPER_BOUND, "88");
		config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "myLuckyNumber");
		config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
		mapper.setConfig(config);
		return mapper;
	}
}
