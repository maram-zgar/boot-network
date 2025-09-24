package dev.maram.boot_network.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.function.Function;


//the JwtService is the service that will generate the token, decode the token, exrtact the info form the token, validate the token..
@Service
public class JwtService {

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;


    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        token = token.trim().replaceAll("\\s+", "");
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
        return buildToken(claims, userDetails, jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims, //extra data that you might want to include in the token (ex: userId)
            UserDetails userDetails,
            long jwtExpiration
    ) {
//        Map<Integer, String> test = Map.of(
//                1, "test",
//                2, "test2"
//        );
//
//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                }
//        ).start();
//
//        List<Integer> integers = List.of(1, 2, 3, 4);
//        integers
//                .stream()
//                .filter(this::isEven)
//                .sorted(Comparator.comparingInt(i -> i))
//                .forEach(System.out::println);
//
//
//
//  private boolean isEven(int n) {
//      return n % 2 == 0;
//  }

        var authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) //Puts the user's username into the token. This is the primary identifier
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .claim("authorities", authorities)//adds the user's permoissions/roles to the token
                .signWith(getSignInKey()) //Cryptographically signs the token with a secret key.
                .compact(); //Finalizes the process and turns all the information into the final, URL-safe JWT string
    }

    //check the validity of the token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    private String getStringValue(int n) {
        return String.valueOf(n * 4);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes); // ALGO: HMAC256
    }
}
