package com.payment.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.payment.wallet.Enum.RoleEnum;
import com.payment.wallet.models.Auth;
import com.payment.wallet.repositories.AuthRepository;


@SpringBootApplication
public class WalletApplication implements CommandLineRunner{

	@Autowired
	private AuthRepository authRepository;



	public static void main(String[] args) {
		SpringApplication.run(WalletApplication.class, args);
	}

	public void run(String... args) throws Exception{
		Auth admin = authRepository.findByRole(RoleEnum.ADMIN);
		if(admin==null){
			Auth auth = new Auth();
			auth.setRole(RoleEnum.ADMIN);
			auth.setEmail("admin@gmail.com");
			auth.setPassword("admin");
			authRepository.save(auth);
		}

	}
}
