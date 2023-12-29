package ru.bssg.lottabyte.core.usermanagement.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.ApiDataForLongPeriodToken;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetailsGroup;
import com.unboundid.util.json.JSONObject;
import ru.bssg.lottabyte.core.usermanagement.service.ApiTokensService;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;


@Slf4j
@Component
public class JwtHelper {
	private final RSAPrivateKey privateKey;
	private final RSAPublicKey publicKey;
	@Autowired
	private ApiTokensService apiTokensService;
	
	public JwtHelper(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	public boolean verifyToken(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm)
					//more validations if needed
					.build();
			verifier.verify(token);
			return true;
		} catch (Exception e){
			log.error("Exception in verifying " + e.toString());
			return false;
		}
	}
	
	public List<String> getRoles(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			Claim claim = claims.get("roles");

			return Arrays.asList(claim.asArray(String.class));
		} catch (Exception e){
			log.error("Ролей нет " + e.toString());
			return null;
		}
	}

	public Boolean getLongPeriodToken(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			Claim claim = claims.get("long_period_token");
			if(claim != null)
				return claims.get("long_period_token").asBoolean();
		} catch (Exception e){
			log.error("long_period_token отсутствуют " + e.toString());
			return null;
		}
		return false;
	}

	public List<String> getPermissions(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			Claim claim = claims.get("permissions");

			return Arrays.asList(claim.asArray(String.class));
		} catch (Exception e){
			log.error("Permissions отсутствуют " + e.toString());
			return null;
		}
	}

	public String getTenant(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			Claim claim = claims.get("tenant");

			return claim.asString();
		} catch (Exception e){
			log.error("Tenant отсутствует " + e.toString());
			return null;
		}
	}

	public String getUid(String token){
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			Claim claim = claims.get("uid");

			return claim.asString();
		} catch (Exception e){
			log.error("Tenant отсутствует " + e.toString());
			return null;
		}
	}

	public UserDetails getUserDetail(String token) throws LottabyteException {
		UserDetails userDetails = new UserDetails();
		try {
			Algorithm algorithm = Algorithm.RSA256(publicKey, null);
			JWTVerifier verifier = JWT.require(algorithm).build();
			DecodedJWT jwt = verifier.verify(token);
			Map<String, Claim> claims = jwt.getClaims();
			if (claims.containsKey("uid"))
				userDetails.setUid(claims.get("uid").asString());
			if (claims.containsKey("tenant"))
				userDetails.setTenant(claims.get("tenant").asString());
			if (claims.containsKey("internalUser"))
				userDetails.setInternalUser(claims.get("internalUser").asBoolean());
			if (claims.containsKey("permissions"))
				userDetails.setPermissions(Arrays.asList(claims.get("permissions").asArray(String.class)));
			if (claims.containsKey("roles"))
				userDetails.setUserRoles(Arrays.asList(claims.get("roles").asArray(String.class)));
			if (claims.containsKey("domains"))
				userDetails.setUserDomains(Arrays.asList(Arrays.stream(claims.get("domains").asArray(String.class)).map(x -> UUID.fromString(x)).toArray(sz -> new UUID[sz])));
			if (claims.containsKey("groups"))
				userDetails.setGroupRoles(Arrays.asList(claims.get("groups").asArray(String.class)));
			if (claims.containsKey("authenticator"))
				userDetails.setAuthenticator(claims.get("authenticator").asString());
			if (claims.containsKey("username"))
				userDetails.setUsername(claims.get("username").asString());
			if (claims.containsKey("stewardId"))
				userDetails.setStewardId(claims.get("stewardId").asString());
			if (claims.containsKey("system_name"))
				userDetails.setSystemName(claims.get("system_name").asString());
			if (claims.containsKey("description"))
				userDetails.setSystemName(claims.get("description").asString());
			if (claims.containsKey("language"))
				userDetails.setLanguage(Language.valueOf(claims.get("language").asString()));

			return userDetails;
		} catch (Exception e){
			throw new LottabyteException(HttpStatus.NOT_AUTHORIZED, e.toString());
		}
	}

	public String createJwtForClaims(UserDetails userDetails) {
		log.debug("Создание JWT токена для пользователя: " + userDetails.getUsername());

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Instant.now().toEpochMilli());
		calendar.add(Calendar.DATE, 1);

		Builder jwtBuilder = JWT.create().withSubject(userDetails.getUsername());

		if(userDetails.getUserRoles() == null){
			userDetails.setUserRoles(new ArrayList<String>(Arrays.asList("simple_user")));//Поставить дефолтные роли
		}
		
		// Add claims
		jwtBuilder.withClaim("uid", userDetails.getUid());
		jwtBuilder.withClaim("username", userDetails.getUsername());
		jwtBuilder.withClaim("tenant", userDetails.getTenant());
		jwtBuilder.withArrayClaim("roles", userDetails.getUserRoles().stream().toArray(String[]::new));
		jwtBuilder.withArrayClaim("domains", userDetails.getUserDomains().stream().map(x -> x.toString()).toArray(String[]::new));
		jwtBuilder.withArrayClaim("permissions", userDetails.getPermissions().stream().toArray(String[]::new));
		jwtBuilder.withClaim("language", userDetails.getLanguage().toString());
		List<Long> groups = new ArrayList<>();
		if(userDetails.getGroups() != null){
			for(UserDetailsGroup userDetailsGroup : userDetails.getGroups()){
				groups.add(userDetailsGroup.getGroupId());
			}
		}
		jwtBuilder.withArrayClaim("groups", groups.stream().toArray(Long[]::new));
		jwtBuilder.withClaim("authenticator", userDetails.getAuthenticator());
		jwtBuilder.withClaim("internalUser", userDetails.getInternalUser());
		if (userDetails.getStewardId() != null)
			jwtBuilder.withClaim("stewardId", userDetails.getStewardId());

		// Add expiredAt and etc
		return jwtBuilder
				.withNotBefore(new Date())
				.withExpiresAt(calendar.getTime())
				.sign(Algorithm.RSA256(publicKey, privateKey));
	}

	public String createLongPeriodToken(UserDetails apiDataForLongPeriodToken, UserDetails userDetails) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Instant.now().toEpochMilli());
		calendar.add(Calendar.YEAR, 1);

		Builder jwtBuilder = JWT.create().withSubject(apiDataForLongPeriodToken.getSystemName());

		if(apiDataForLongPeriodToken.getUserRoles() == null){
			apiDataForLongPeriodToken.setUserRoles(new ArrayList<String>(Arrays.asList("simple_user")));//Подтягивать дефолтные роли из бд
		}

		Integer apiTokenId = apiTokensService.createApiToken(apiDataForLongPeriodToken, userDetails);
		// Add claims
		jwtBuilder.withClaim("uid", apiTokenId);
		jwtBuilder.withClaim("tenant", apiDataForLongPeriodToken.getTenant());
		jwtBuilder.withClaim("system_name", apiDataForLongPeriodToken.getSystemName());
		jwtBuilder.withClaim("description", apiDataForLongPeriodToken.getDescription());
		jwtBuilder.withClaim("long_period_token", true);
		jwtBuilder.withArrayClaim("roles", apiDataForLongPeriodToken.getUserRoles().stream().toArray(String[]::new));
		jwtBuilder.withArrayClaim("permissions", apiDataForLongPeriodToken.getPermissions().stream().toArray(String[]::new));

		// Add expiredAt and etc
		return jwtBuilder
				.withNotBefore(new Date())
				.withExpiresAt(calendar.getTime())
				.sign(Algorithm.RSA256(publicKey, privateKey));
	}

	public String getUserFromToken(String token) throws Exception {
		JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();
		return claims.getSubject();
	}

	public String getUserDataFromTokenWithExpiredToken(String token, String target) throws Exception {
		Base64.Decoder decoder = Base64.getUrlDecoder();
		String[] parts = token.split("\\.");
		JSONObject obj = new JSONObject(new String(decoder.decode(parts[1])));
		String value = String.valueOf(obj.getField(target));
		return value.substring( 1, value.length() - 1 );
	}

	public RSAPublicKey getPublicKey() {
		return this.publicKey;
	}
}
