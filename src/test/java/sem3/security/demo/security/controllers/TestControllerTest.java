package sem3.security.demo.security.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sem3.security.demo.security.entities.ERole;
import sem3.security.demo.security.entities.Role;
import sem3.security.demo.security.entities.User;
import sem3.security.demo.security.payload.request.LoginRequest;
import sem3.security.demo.security.payload.request.SignupRequest;
import sem3.security.demo.security.payload.response.JwtResponse;
import sem3.security.demo.security.repositories.RoleRepository;
import sem3.security.demo.security.repositories.UserRepository;
import static org.junit.jupiter.api.Assertions.*;




@ActiveProfiles("test")  //Will prevent the DateSetup CommandlineRunner from running
@AutoConfigureTestDatabase
@EnableAutoConfiguration
@SpringBootTest(
        classes = {sem3.security.demo.DemoApplication.class},
        //classes = {sem3.security.demo.security.controllers.TestControllerTest.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestControllerTest {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    private final String BASE_PATH = "/api/test";
    private final HttpHeaders headers = new HttpHeaders();

    @LocalServerPort
    private int port;

    @Autowired
    TestRestTemplate restTemplate;

    //This is how we hold on to the token after login, similar to that a client must store the token somewhere
    private static String securityToken;

    //Utility method to login and store the returned securityToken
    private   ResponseEntity<JwtResponse>  login(String userName, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(userName);
        loginRequest.setPassword(password);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest,headers);
        ResponseEntity<JwtResponse> response = restTemplate.exchange(makeUrl("/api/auth/signin"),
                HttpMethod.POST,
                entity,
                JwtResponse.class);
        return response;
    }

    @BeforeEach
    public  void addRoles(){
        Role userRole = new Role(ERole.ROLE_CUSTOMER);
        Role empRole = new Role(ERole.ROLE_EMPLOYEE);
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        roleRepository.save(userRole);
        roleRepository.save(empRole);
        roleRepository.save(adminRole);
        User user = new User("user", "user@a.dk", encoder.encode("test"));
        user.addRole(userRole);
        User admin = new User("admin", "admin@a.dk", encoder.encode("test"));
        admin.addRole(adminRole);
        User emp = new User("employee", "emp@a.dk", encoder.encode("test"));
        emp.addRole(empRole);
        User user_admin = new User("user_admin", "both@a.dk", encoder.encode("test"));
        user_admin.addRole(userRole);
        user_admin.addRole(adminRole);
        userRepository.save(user);
        userRepository.save(emp);
        userRepository.save(admin);
        userRepository.save(user_admin);
    }

    @Test
    void allAccessTest() {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl(BASE_PATH+ "/all"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(200,response.getStatusCode().value());
        assertEquals("Public Content.",response.getBody());
    }


    @Test
    void addNewUserTest() {
        SignupRequest newUser = new SignupRequest();
        newUser.setUsername("xxxxx");
        newUser.setEmail("xxxx@xx.dk");
        newUser.setPassword("xxxxxx");
        HttpHeaders headersForRequest = new HttpHeaders();
        headersForRequest.add("Content-Type","application/json");
        HttpEntity<SignupRequest> entity = new HttpEntity<>(newUser,headersForRequest);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl("/api/auth/signup"),
                HttpMethod.POST,
                entity,
                String.class);
        assertEquals(200,response.getStatusCode().value());

        //Verify that we can log in as the new user
        ResponseEntity<JwtResponse>  res = login("xxxxx","xxxxxx");
        assertEquals(200,res.getStatusCode().value());
        JwtResponse jwtResponse = res.getBody();
        assertNotNull(jwtResponse.getAccessToken());
        assertEquals(1,jwtResponse.getRoles().size());
        assertEquals("ROLE_CUSTOMER",jwtResponse.getRoles().get(0));
    }

    @Test
    void accessProtectedResourceNotLoggedInTest(){
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl(BASE_PATH+ "/mod"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(401,response.getStatusCode().value());
    }

    @Test
    void loginTest(){
        JwtResponse res = login("user","test").getBody();
        assertNotNull(res.getAccessToken());
        assertEquals(1,res.getRoles().size());
        assertEquals("ROLE_CUSTOMER",res.getRoles().get(0));
    }

    @Test
    void invalidLoginTest(){
        //Important --> to get this test to work, the client has been replaced (in POM) with httpComponent
        //Se this post for details --> https://stackoverflow.com/questions/27341604/exception-when-using-testresttemplate
        ResponseEntity<JwtResponse>  res = login("i-dont-exist","test");
        assertEquals(401,res.getStatusCode().value());
    }

    @Test
    void accessProtectedResourceWithInvalidRoleTest(){
        String securityToken = "Bearer "+ login("user","test").getBody().getAccessToken();
        HttpHeaders headersForRequest = new HttpHeaders();
        headersForRequest.add("Authorization",securityToken);
        HttpEntity<String> entity = new HttpEntity<>(null,headersForRequest);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl(BASE_PATH+ "/admin"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(403,response.getStatusCode().value());
    }

    @Test
    void accessProtectedResourceWithValidRoleTest(){
        String securityToken = "Bearer "+ login("admin","test").getBody().getAccessToken();
        HttpHeaders headersForRequest = new HttpHeaders();
        headersForRequest.add("Authorization",securityToken);
        HttpEntity<String> entity = new HttpEntity<>(null,headersForRequest);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl(BASE_PATH+ "/admin"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(200,response.getStatusCode().value());
        assertEquals("Admin Board.",response.getBody());
    }

    @Test
    void userWithMultipleRolesTest() {
        String securityToken = "Bearer "+ login("user_admin","test").getBody().getAccessToken();
        HttpHeaders headersForRequest = new HttpHeaders();
        headersForRequest.add("Authorization",securityToken);

        HttpEntity<String> entity = new HttpEntity<>(null,headersForRequest);
        ResponseEntity<String> response = restTemplate.exchange(makeUrl(BASE_PATH+ "/admin"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(200,response.getStatusCode().value(),"Can access /admin");


        response = restTemplate.exchange(makeUrl(BASE_PATH+ "/user"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(200,response.getStatusCode().value(),"Can access /user");

        response = restTemplate.exchange(makeUrl(BASE_PATH+ "/mod"),
                HttpMethod.GET,
                entity,
                String.class);
        assertEquals(403,response.getStatusCode().value(),"CANNOT access mod");

    }

    @Test
    void endPointWithMultipleRolesTest() {
        String securityToken = "Bearer "+ login("admin","test").getBody().getAccessToken();
        ResponseEntity<String> response = getStringResponseEntity(securityToken);
        assertEquals(200,response.getStatusCode().value(),"User, 'employee' CANNOT access /api/test/user");

        securityToken = "Bearer "+ login("user","test").getBody().getAccessToken();
        response = getStringResponseEntity(securityToken);
        assertEquals(200,response.getStatusCode().value(),"User, 'user' CAN access /api/test/user");

        securityToken = "Bearer "+ login("employee","test").getBody().getAccessToken();
        response = getStringResponseEntity(securityToken);
        assertEquals(403,response.getStatusCode().value(),"User, 'admin' CAN access /api/test/user");
    }

    private ResponseEntity<String> getStringResponseEntity(String securityToken) {
        HttpHeaders headersForRequest = new HttpHeaders();
        headersForRequest.add("Authorization",securityToken);
        HttpEntity<String> entity = new HttpEntity<>(null,headersForRequest);
        return restTemplate.exchange(makeUrl(BASE_PATH+ "/user"),
                HttpMethod.GET,
                entity,
                String.class);
    }

    private String makeUrl(String path){
        String pathBuilded = "http://localhost:"+port+path;
        //System.out.println(pathBuilded);
        return pathBuilded;
    }
}