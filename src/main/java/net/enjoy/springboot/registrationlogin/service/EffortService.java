package net.enjoy.springboot.registrationlogin.service;

import net.enjoy.springboot.registrationlogin.entity.Effort;

import java.util.List;

public interface EffortService {


    Effort saveEffort(Effort effort);

    List<Effort> getUserEfforts(String email);

}
