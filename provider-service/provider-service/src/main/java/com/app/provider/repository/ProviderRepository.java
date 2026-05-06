package com.app.provider.repository;

import com.app.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    @Query("""
           SELECT p FROM Provider p
           WHERE LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(p.specialization, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(p.clinicName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    List<Provider> searchByNameOrSpecialization(String keyword);

    List<Provider> findBySpecialization(String specialization);

    Optional<Provider> findByUserId(Long userId);
}
