package sem3.security.demo.security.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sem3.security.demo.error.Client4xxException;
import sem3.security.demo.security.entities.ERole;
import sem3.security.demo.security.entities.Role;
import sem3.security.demo.security.entities.User;
import sem3.security.demo.security.jwt.JwtUtils;
import sem3.security.demo.security.payload.request.LoginRequest;
import sem3.security.demo.security.payload.request.SignupRequest;
import sem3.security.demo.security.payload.response.JwtResponse;
import sem3.security.demo.security.payload.response.MessageResponse;
import sem3.security.demo.security.repositories.RoleRepository;
import sem3.security.demo.security.repositories.UserRepository;
import sem3.security.demo.security.services.UserDetailsImpl;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	static final String ROLE_NOT_FOUND_MESSAGE = "Error: Role is not found.";

	@Autowired
    AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
    PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority) //.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt,
												 userDetails.getUsername(), 
												 userDetails.getEmail(), 
												 roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			throw new Client4xxException("Username is already taken");
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			throw new Client4xxException(" Email is already in use");
		}

		User user = new User(signUpRequest.getUsername(),
							 signUpRequest.getEmail(),
							 encoder.encode(signUpRequest.getPassword()));

		Set<Role> roles = new HashSet<>();
    	Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
					.orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_MESSAGE));
			roles.add(customerRole);

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}
}
