package com.citasmedicas.repository;
import com.citasmedicas.model.CitaMedica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Long> {
 List<CitaMedica> findByUsuarioUsernameOrderByFechaAscHoraAsc(String username);
 List<CitaMedica> findAllByOrderByFechaAscHoraAsc();
}
