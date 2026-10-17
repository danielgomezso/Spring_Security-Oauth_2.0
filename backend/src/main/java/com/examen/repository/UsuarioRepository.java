package com.examen.repository;
import com.examen.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
 Optional<Usuario> findByUsername(String username);
}
