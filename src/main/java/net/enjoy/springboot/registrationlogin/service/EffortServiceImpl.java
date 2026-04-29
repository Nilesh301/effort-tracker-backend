package net.enjoy.springboot.registrationlogin.service;

import net.enjoy.springboot.registrationlogin.entity.Effort;
import net.enjoy.springboot.registrationlogin.entity.User;
import net.enjoy.springboot.registrationlogin.repository.EffortRepository;
import net.enjoy.springboot.registrationlogin.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EffortServiceImpl implements EffortService{



    private EffortRepository effortRepository;
    private UserRepository userRepository;

    public EffortServiceImpl(EffortRepository effortRepository, UserRepository userRepository) {
        this.effortRepository = effortRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Effort saveEffort(Effort effort) {
        return effortRepository.save(effort);
    }

    @Override
    public List<Effort> getUserEfforts(String email) {
        User user = userRepository.findByEmail(email);
        return effortRepository.findByUser(user);
    }
}
