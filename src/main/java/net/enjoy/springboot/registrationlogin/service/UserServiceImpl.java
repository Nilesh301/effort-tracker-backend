package net.enjoy.springboot.registrationlogin.service;

import net.enjoy.springboot.registrationlogin.dto.UserDto;
import net.enjoy.springboot.registrationlogin.entity.Role;
import net.enjoy.springboot.registrationlogin.entity.User;
import net.enjoy.springboot.registrationlogin.repository.RoleRepository;
import net.enjoy.springboot.registrationlogin.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.Arrays;

/*import java.util.Arrays;*/
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDate.parse;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
// new code start
    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();

        userDto.setId(user.getId());   // ✅ THIS LINE IS MISSING
        userDto.setFirstName(user.getName());
        userDto.setLastName(user.getName());
        userDto.setEmail(user.getEmail());

        return userDto;
    }
    // new code end

     // new code start

    @Override
    public void saveUser(UserDto userDto) {

        User user = new User();

        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        // ✅ ADD THIS
        user.setDob(parse(userDto.getDob()));

        Role role = roleRepository.findByName("ROLE_USER");

        if (role == null) {
            throw new RuntimeException("Role not found.");
        }

        user.setRoles(Arrays.asList(role));

        userRepository.save(user);
    }
    // new code end

   /* @Override
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        //encrypt the password using spring security
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        Role role = roleRepository.findByName("ROLE_ADMIN");
        if (role == null) {
            role = checkRoleExist();
        }
        user.setRoles(List.of(role));
        userRepository.save(user);
    }*/



    private Role checkRoleExist() {
        Role role = new Role();
        role.setName("ROLE_ADMIN");
        return roleRepository.save(role);
    }


    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map((user) -> convertEntityToDto(user))
                .collect(Collectors.toList());
    }

/*    @Override
    public String generateResetToken(String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        return token;
    }*/

    private UserDto convertEntityToDto(User user) {
        UserDto userDto = new UserDto();
        String[] name = user.getName().split(" ");
        userDto.setFirstName(name[0]);
        userDto.setLastName(name[1]);
        userDto.setEmail(user.getEmail());
        return userDto;
    }
/*    @Override
    public boolean resetPassword(String token, String newPassword) {

        User user = userRepository.findByResetToken(token);

        // ❌ Invalid or expired token
        if (user == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // ✅ Update password
        user.setPassword(passwordEncoder.encode(newPassword));

        // ✅ Clear token after use
        user.setResetToken(null);
        user.setTokenExpiry(null);

        userRepository.save(user);

        return true;
    }*/
}
